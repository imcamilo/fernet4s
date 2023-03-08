package com.github.imcamilo.fernet

import com.github.imcamilo.exceptions.{WHKeyException, WHTokenException}
import org.slf4j.LoggerFactory

import java.io.{ByteArrayOutputStream, DataOutputStream}
import java.security.{
  InvalidAlgorithmParameterException,
  InvalidKeyException,
  NoSuchAlgorithmException
}
import java.time.Instant
import java.util.Arrays.{copyOf, copyOfRange}
import javax.crypto.Cipher.{DECRYPT_MODE, ENCRYPT_MODE}
import javax.crypto._
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import scala.util.{Failure, Success, Try, Using}

/** Create a Key from individual components.
  *
  *  @param signingKey
  *    a 128-bit (16 byte) key for signing tokens.
  *  @param encryptionKey
  *    a 128-bit (16 byte) key for encrypting and decrypting token contents.
  */
class Key(val signingKey: Array[Byte], val encryptionKey: Array[Byte])

object Key {

  private val logger = LoggerFactory.getLogger(getClass)

  import Constants._

  /** Encrypt a payload to embed in a Fernet token
    *  @param payload
    *    the raw bytes of the data to store in a token
    *  @param initializationVector
    *    random bytes from a high-entropy source to initialise the AES cipher
    *  @param breadcrumbEncryptionKey
    *    encryption key, for keeping immutable data
    *  @return
    *    the AES-encrypted payload. The length will always be a multiple of 16 (128 bits).
    */
  def encrypt(
      payload: Array[Byte],
      initializationVector: IvParameterSpec,
      breadcrumbEncryptionKey: Array[Byte]
  ): Array[Byte] = {
    val encryptionKeySpec = getEncryptionKeySpec(breadcrumbEncryptionKey)
    try {
      val cipher = Cipher.getInstance(cipherTransformation)
      cipher.init(ENCRYPT_MODE, encryptionKeySpec, initializationVector)
      cipher.doFinal(payload)
    } catch {
      case e @ (_: NoSuchAlgorithmException | _: NoSuchPaddingException) =>
        // these should not happen as we use an algorithm (AES) and padding (PKCS5) that are guaranteed to exist
        throw new IllegalStateException(
          "Unable to access cipher " + cipherTransformation + ": " + e.getMessage,
          e
        )
      case e @ (_: InvalidKeyException |
          _: InvalidAlgorithmParameterException) =>
        // this should not happen as the key is validated ahead of time and
        // we use an algorithm guaranteed to exist
        throw new IllegalStateException(
          "Unable to initialise encryption cipher with algorithm " + encryptionKeySpec.getAlgorithm + " and format " + encryptionKeySpec.getFormat + ": " + e.getMessage,
          e
        )
      case e @ (_: IllegalBlockSizeException | _: BadPaddingException) =>
        // these should not happen as we control the block size and padding
        throw new IllegalStateException(
          "Unable to encrypt data: " + e.getMessage,
          e
        )
    }
  }

  /** @param string
    *    a Base 64 URL string in the format Signing-key (128 bits) || Encryption-key (128 bits).
    *
    *  Create a Key from a payload containing the signing and encryption key. Use a concatenatedKeys an array of 32 bytes
    *  of which the first 16 is the signing key and the last 16 is the encryption/decryption key
    *  @return
    *    a WHKey case class from individual components.
    */
  def apply(string: String): Option[Key] = {
    val concatenatedKeys = decoder.decode(string)

    val keyInstances = Try {
      creatingKeyInstance(
        copyOfRange(concatenatedKeys, 0, signingKeyBytes),
        copyOfRange(concatenatedKeys, signingKeyBytes, fernetKeyBytes)
      )
    }.recover {
      case error =>
        throw new WHKeyException(error.getMessage)
    }

    keyInstances.flatten match {
      case Failure(exception) =>
        logger.error(
          "exception creating key instance - " + exception.getMessage
        )
        None
      case Success(keys) => Option(new Key(keys._1, keys._2))
    }

  }

  def creatingKeyInstance(
      signingKey: Array[Byte],
      encryptionKey: Array[Byte]
  ): Try[(Array[Byte], Array[Byte])] =
    Try {
      if (signingKey == null || signingKey.length != signingKeyBytes) {
        throw new IllegalArgumentException("Signing key must be 128 bits")
      }
      if (encryptionKey == null || encryptionKey.length != encryptionKeyBytes) {
        throw new IllegalArgumentException("Encryption key must be 128 bits")
      }
      val localSigningKey = copyOf(signingKey, signingKeyBytes)
      val localEncryptionKey = copyOf(encryptionKey, encryptionKeyBytes)

      (localSigningKey, localEncryptionKey)
    }

  def sign(
      version: Byte,
      timestamp: Instant,
      initializationVector: IvParameterSpec,
      cipherText: Array[Byte],
      breadcrumbSignKey: Array[Byte]
  ): Array[Byte] = {
    Using(new ByteArrayOutputStream(tokenPrefixBytes + cipherText.length)) {
      byteStream =>
        sign(
          version,
          timestamp,
          initializationVector,
          cipherText,
          byteStream,
          breadcrumbSignKey
        )
    }.recover {
      case e =>
        throw new IllegalStateException(e.getMessage, e)
    }.get
  }

  def sign(
      version: Byte,
      timestamp: Instant,
      initializationVector: IvParameterSpec,
      cipherText: Array[Byte],
      byteStream: ByteArrayOutputStream,
      breadcrumbSignKey: Array[Byte]
  ): Array[Byte] = {
    Using(new DataOutputStream(byteStream)) { dataStream =>
      dataStream.writeByte(version)
      dataStream.writeLong(timestamp.getEpochSecond)
      dataStream.write(initializationVector.getIV)
      dataStream.write(cipherText)
      try {
        val mac = Mac.getInstance(signingAlgorithm)
        mac.init(getSigningKeySpec(breadcrumbSignKey))
        mac.doFinal(byteStream.toByteArray)
      } catch {
        case ike: InvalidKeyException =>
          // this should not happen because we control the signing key
          // algorithm and pre-validate the length
          throw new IllegalStateException(
            "Unable to initialise HMAC with shared secret: " + ike.getMessage,
            ike
          )
        case nsae: NoSuchAlgorithmException =>
          // this should not happen as implementors are required to
          // provide the HmacSHA256 algorithm.
          throw new IllegalStateException(nsae.getMessage, nsae)
      }
    }.recover {
      case error =>
        throw new RuntimeException("exception sign key")
    }.get
  }

  def getSigningKeySpec(breadcrumbSigningKey: Array[Byte]): SecretKeySpec = {
    new SecretKeySpec(breadcrumbSigningKey, signingAlgorithm)
  }

  def getEncryptionKeySpec(
      breadcrumbEncryptionKey: Array[Byte]
  ): SecretKeySpec = {
    new SecretKeySpec(breadcrumbEncryptionKey, encryptionAlgorithm)
  }

  /** Decrypt the payload of a Fernet token.
    *
    *  <p> Warning: Do not call this unless the cipher text has first been verified. Attempting to decrypt a cipher text
    *  that has been tampered with will leak whether or not the padding is correct and this can be used to decrypt stolen
    *  cipher text. </p>
    *
    *  @param cipherText
    *    the verified padded encrypted payload of a token. The length <em>must</em> be a multiple of 16 (128 bits).
    *  @param initializationVector
    *    the random bytes used in the AES encryption of the token
    *  @param breadcrumbEncryptionKey
    *    A breadcrumb. The Array of Bytes of the encryption key, just for immutable reasons.
    *  @return
    *    the decrypted payload
    */
  def decrypt(
      cipherText: Array[Byte],
      initializationVector: IvParameterSpec,
      breadcrumbEncryptionKey: Array[Byte]
  ): Array[Byte] = {
    try {
      val cipher = Cipher.getInstance(cipherTransformation)
      cipher.init(
        DECRYPT_MODE,
        getEncryptionKeySpec(breadcrumbEncryptionKey),
        initializationVector
      )
      cipher.doFinal(cipherText)
    } catch {
      case e @ (_: NoSuchAlgorithmException | _: NoSuchPaddingException |
          _: InvalidKeyException | _: InvalidAlgorithmParameterException |
          _: IllegalBlockSizeException) =>
        // this should not happen as we use an algorithm (AES) and padding
        // (PKCS5) that are guaranteed to exist.
        // in addition, we validate the encryption key and initialization vector up front
        throw new IllegalStateException(e.getMessage, e)
      case bpe: BadPaddingException =>
        throw new WHTokenException(
          "Invalid padding in token: " + bpe.getMessage + " - Bad padding exception: " + bpe
        )
    }
  }

}
