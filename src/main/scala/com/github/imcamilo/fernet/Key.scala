package com.github.imcamilo.fernet

import com.github.imcamilo.exceptions.Token4sException

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

/**
  * Companion object for creating instances of the Key class.
  */
object Key {

  private val logger = LoggerFactory.getLogger(getClass)

  import Constants._

  /** Encrypt a payload to embed in a Fernet token
    *  @param payload
    *    the raw bytes of the data to store in a token
    *  @param initializationVector
    *    random bytes from a high-entropy source to initialise the AES cipher
    *  @param encryptionKey
    *    encryption key, for keeping immutable data
    *  @return
    *    the AES-encrypted payload. The length will always be a multiple of 16 (128 bits).
    */
  def encrypt(
      payload: Array[Byte],
      initializationVector: IvParameterSpec,
      encryptionKey: Array[Byte]
  ): Array[Byte] = {
    val encryptionKeySpec = getEncryptionKeySpec(encryptionKey)
    Try {
      val cipher = Cipher.getInstance(cipherTransformation)
      cipher.init(ENCRYPT_MODE, encryptionKeySpec, initializationVector)
      cipher.doFinal(payload)
    }.recover {
      case e @ (_: NoSuchAlgorithmException | _: NoSuchPaddingException) =>
        throw new IllegalStateException(
          s"Unable to access cipher $cipherTransformation: ${e.getMessage}",
          e
        )
      case e @ (_: InvalidKeyException |
          _: InvalidAlgorithmParameterException) =>
        throw new IllegalStateException(
          s"Unable to initialize encryption cipher with algorithm ${encryptionKeySpec.getAlgorithm}: ${e.getMessage}",
          e
        )
      case e @ (_: IllegalBlockSizeException | _: BadPaddingException) =>
        throw new IllegalStateException(
          s"Unable to encrypt data: ${e.getMessage}",
          e
        )
    }.get
  }

  /**
    * Creates a Key instance from the given string.
    * @param string
    *    a Base64 URL string in the format Signing-key (128 bits) || Encryption-key (128 bits).
    *
    * @return
    *    a Key instance from individual components.
    */
  def apply(string: String): Option[Key] = {
    val concatenatedKeys = decoder.decode(string)
    val keyInstance = creatingKeyInstance(
      copyOfRange(concatenatedKeys, 0, signingKeyBytes),
      copyOfRange(concatenatedKeys, signingKeyBytes, fernetKeyBytes)
    )
    keyInstance match {
      case Failure(ex) =>
        logger.error(s"Exception creating key instance: ${ex.getMessage}", ex)
        None
      case Success((signingKey, encryptionKey)) =>
        Some(new Key(signingKey, encryptionKey))
    }
  }

  /**
    * Creates a key instance from the given signing and encryption keys.
    * @param signingKey The signing key.
    * @param encryptionKey The encryption key.
    * @return A Try containing the key instance if successful.
    */
  def creatingKeyInstance(
      signingKey: Array[Byte],
      encryptionKey: Array[Byte]
  ): Try[(Array[Byte], Array[Byte])] = {
    Try {
      validateKeys(signingKey, encryptionKey)
      (
        copyOf(signingKey, signingKeyBytes),
        copyOf(encryptionKey, encryptionKeyBytes)
      )
    }
  }

  /**
    * Validates the signing and encryption keys.
    * @param signingKey The signing key.
    * @param encryptionKey The encryption key.
    * @throws IllegalArgumentException If the signing or encryption key is null or not of the expected length.
    */
  private def validateKeys(
      signingKey: Array[Byte],
      encryptionKey: Array[Byte]
  ): Unit = {
    if (signingKey == null || signingKey.length != signingKeyBytes) {
      throw new IllegalArgumentException("Signing key must be 128 bits.")
    }
    if (encryptionKey == null || encryptionKey.length != encryptionKeyBytes) {
      throw new IllegalArgumentException("Encryption key must be 128 bits.")
    }
  }

  /**
    * Sign the data with HMAC.
    * @param version The version of the token.
    * @param timestamp The timestamp of token creation.
    * @param initializationVector The IV used for encryption.
    * @param cipherText The encrypted payload.
    * @param signingKey The signing key for HMAC.
    * @return The HMAC signature.
    */
  def sign(
      version: Byte,
      timestamp: Instant,
      initializationVector: IvParameterSpec,
      cipherText: Array[Byte],
      signingKey: Array[Byte]
  ): Array[Byte] = {
    Using(new ByteArrayOutputStream(tokenPrefixBytes + cipherText.length)) {
      byteStream =>
        Using(new DataOutputStream(byteStream)) { dataStream =>
          dataStream.writeByte(version)
          dataStream.writeLong(timestamp.getEpochSecond)
          dataStream.write(initializationVector.getIV)
          dataStream.write(cipherText)
          try {
            val mac = Mac.getInstance(signingAlgorithm)
            mac.init(getSigningKeySpec(signingKey))
            mac.doFinal(byteStream.toByteArray)
          } catch {
            case ike: InvalidKeyException =>
              throw new IllegalStateException(
                s"Unable to initialize HMAC with shared secret: ${ike.getMessage}",
                ike
              )
            case nsae: NoSuchAlgorithmException =>
              throw new IllegalStateException(
                s"Signing algorithm not available: ${nsae.getMessage}",
                nsae
              )
          }
        }
    }.recover {
      case e =>
        throw new IllegalStateException(
          s"Error during signing process: ${e.getMessage}",
          e
        )
    }.get
  }

  private def getSigningKeySpec(signingKey: Array[Byte]): SecretKeySpec = {
    new SecretKeySpec(signingKey, signingAlgorithm)
  }

  private def getEncryptionKeySpec(
      encryptionKey: Array[Byte]
  ): SecretKeySpec = {
    new SecretKeySpec(encryptionKey, encryptionAlgorithm)
  }

  /** Decrypt the payload of a Fernet token.
    *  @param cipherText
    *    the encrypted payload.
    *  @param initializationVector
    *    the IV used in the AES encryption.
    *  @param encryptionKey
    *    The encryption key.
    *  @return
    *    the decrypted payload.
    */
  def decrypt(
      cipherText: Array[Byte],
      initializationVector: IvParameterSpec,
      encryptionKey: Array[Byte]
  ): Array[Byte] = {
    Try {
      val cipher = Cipher.getInstance(cipherTransformation)
      cipher.init(
        DECRYPT_MODE,
        getEncryptionKeySpec(encryptionKey),
        initializationVector
      )
      cipher.doFinal(cipherText)
    }.recover {
      case e @ (_: NoSuchAlgorithmException | _: NoSuchPaddingException |
          _: InvalidKeyException | _: InvalidAlgorithmParameterException |
          _: IllegalBlockSizeException) =>
        throw new IllegalStateException(
          s"Unable to decrypt data: ${e.getMessage}",
          e
        )
      case bpe: BadPaddingException =>
        throw new Token4sException(
          s"Invalid padding in token: ${bpe.getMessage}",
          bpe
        )
    }.get
  }
}
