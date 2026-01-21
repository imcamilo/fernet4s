package com.github.imcamilo.fernet

import com.github.imcamilo.exceptions.Token4sException
import com.github.imcamilo.validators.Validator
import org.slf4j.LoggerFactory

import java.io._
import java.security.{MessageDigest, SecureRandom}
import java.time.Instant
import javax.crypto.spec.IvParameterSpec
import scala.util.{Failure, Success, Try, Using}

class Token(
    val version: Byte,
    val timestamp: Instant,
    val initializationVector: IvParameterSpec,
    val cipherText: Array[Byte],
    val hmac: Array[Byte]
) {

  private val logger = LoggerFactory.getLogger(getClass)

  /** Check the validity of this token.
    *  @param key
    *    the secret key against which to validate the token
    *  @param validator
    *    an object that encapsulates the validation parameters (e.g. TTL).
    *  @tparam A
    *    type of the validator
    *  @return
    *    the decrypted, deserialized payload of this token
    */
  def validateAndDecrypt[A](key: Key, validator: Validator[A]): Option[A] = {
    validator.validateAndDecrypt(key, this) match {
      case Failure(exception) =>
        logger.debug(s"Token validation failed: ${exception.getMessage}")
        None
      case Success(value) =>
        Option(value)
    }
  }

  /** Validate the token's signature using the provided key.
    *  @param key the secret key to validate against
    *  @return true if the signature is valid, false otherwise
    */
  def isValidSignature(key: Key): Boolean = {
    val computedHmac = Key.sign(
      version,
      timestamp,
      initializationVector,
      cipherText,
      key.signingKey
    )
    MessageDigest.isEqual(hmac, computedHmac)
  }

  /** Decrypt the token if it passes the validation checks.
    *  @param key
    *    the secret key
    *  @param earliestValidInstant
    *    the earliest allowed instant for validation
    *  @param latestValidInstant
    *    the latest allowed instant for validation
    *  @return
    *    the decrypted payload
    */
  def validateAndDecrypt(
      key: Key,
      earliestValidInstant: Instant,
      latestValidInstant: Instant
  ): Array[Byte] = {
    if (version != 0x80.toByte) {
      throw new Token4sException("Invalid token version.")
    } else if (timestamp.isBefore(earliestValidInstant)) {
      throw new Token4sException("Token has expired.")
    } else if (timestamp.isAfter(latestValidInstant)) {
      throw new Token4sException(
        "Token timestamp is too far in the future (clock skew)."
      )
    } else if (!isValidSignature(key)) {
      throw new Token4sException("Signature validation failed.")
    }
    Key.decrypt(cipherText, initializationVector, key.encryptionKey)
  }
}

object Token {

  import Constants._

  private val logger = LoggerFactory.getLogger(getClass)

  /** Convenience method to generate a new Fernet token with a string payload.
    *
    * @param key
    *   the secret key for encrypting <em>plainText</em> and signing the token
    * @param plainText
    *   the payload to embed in the token
    * @return
    *   a unique Fernet token
    */
  def apply(key: Key, plainText: String): Option[Token] = {
    apply(new SecureRandom, key, plainText.getBytes(charset))
  }

  /** Generate a new Fernet token.
    *  @param random
    *    a source of entropy
    *  @param key
    *    the secret key for encrypting the payload and signing the token
    *  @param payload
    *    the unencrypted data to embed in the token
    *  @return
    *    a unique Fernet token
    */
  def apply(
      random: SecureRandom,
      key: Key,
      payload: Array[Byte]
  ): Option[Token] = {
    val initializationVector = generateInitializationVector(random)
    val cipherText =
      Key.encrypt(payload, initializationVector, key.encryptionKey)
    val timestamp = Instant.now
    val hmac = Key.sign(
      supportedVersion,
      timestamp,
      initializationVector,
      cipherText,
      key.signingKey
    )
    Option(
      Token.initializeToken(
        supportedVersion,
        timestamp,
        initializationVector,
        cipherText,
        hmac
      )
    )
  }

  def serialize(token: Token): Try[String] = {
    Using(
      new ByteArrayOutputStream(tokenStaticBytes + token.cipherText.length)
    ) { byteStream =>
      writeTo(byteStream, token)
      encoder.encodeToString(byteStream.toByteArray)
    }.recover {
      case e: Exception =>
        logger.debug(s"Error serializing token: ${e.getMessage}")
        ""
    }
  }

  def writeTo(outputStream: OutputStream, token: Token): Try[Unit] = {
    Using(new DataOutputStream(outputStream)) { dataStream =>
      dataStream.writeByte(token.version)
      dataStream.writeLong(token.timestamp.getEpochSecond)
      dataStream.write(token.initializationVector.getIV)
      dataStream.write(token.cipherText)
      dataStream.write(token.hmac)
    }
  }

  protected def generateInitializationVector(
      random: SecureRandom
  ): IvParameterSpec = {
    new IvParameterSpec(generateInitializationVectorBytes(random))
  }

  protected def generateInitializationVectorBytes(
      random: SecureRandom
  ): Array[Byte] = {
    val ivBytes = new Array[Byte](initializationVectorBytes)
    random.nextBytes(ivBytes)
    ivBytes
  }

  protected def read(stream: DataInputStream, numBytes: Int): Array[Byte] = {
    val bytesRead = new Array[Byte](numBytes)
    if (stream.read(bytesRead) < numBytes) {
      throw new Token4sException("Insufficient data to construct token.")
    }
    bytesRead
  }

  /** Deserialize a Base64-encoded Fernet token string.
    *  @param string the Base64 encoded token string
    *  @return a Token instance, if successfully deserialized
    */
  def fromString(string: String): Option[Token] = {
    Try(decoder.decode(string)) match {
      case Failure(exception) =>
        logger.debug(s"Failed to decode Base64 string: ${exception.getMessage}")
        None
      case Success(bytes) =>
        fromBytes(bytes) match {
          case Failure(exception) =>
            logger.debug(s"Failed to decode token from bytes: ${exception.getMessage}")
            None
          case Success(value) =>
            Option(value)
        }
    }
  }

  /** Deserialize a token from raw bytes.
    *  @param bytes the raw byte representation of the token
    *  @return a Token instance, if successfully deserialized
    */
  def fromBytes(bytes: Array[Byte]): Try[Token] = {
    if (bytes.length < minimumTokenBytes)
      return Failure(new Token4sException("Insufficient bytes to generate token."))

    Using(new ByteArrayInputStream(bytes)) { inputStream =>
      Using(new DataInputStream(inputStream)) { dataStream =>
        val version = dataStream.readByte
        val timestamp = Instant.ofEpochSecond(dataStream.readLong)
        val initializationVector = read(dataStream, initializationVectorBytes)
        val cipherText = read(dataStream, bytes.length - tokenStaticBytes)
        val hmac = read(dataStream, signatureBytes)

        if (dataStream.read() != -1)
          throw new Token4sException("Extra bytes found in token.")

        Token.initializeToken(
          version,
          timestamp,
          new IvParameterSpec(initializationVector),
          cipherText,
          hmac
        )
      }
    }.flatten.recoverWith {
      case ioe: IOException =>
        Failure(new IllegalStateException(ioe.getMessage, ioe))
    }
  }

  /** Initialize a token from its components.
    *  @param version the token version
    *  @param timestamp the creation timestamp
    *  @param initializationVector the IV used for encryption
    *  @param cipherText the encrypted payload
    *  @param hmac the token signature
    *  @return a constructed Token
    */
  def initializeToken(
      version: Byte,
      timestamp: Instant,
      initializationVector: IvParameterSpec,
      cipherText: Array[Byte],
      hmac: Array[Byte]
  ): Token = {
    if (version != supportedVersion)
      throw new Token4sException(s"Unsupported token version: $version")
    if (timestamp == null)
      throw new Token4sException("Timestamp cannot be null.")
    if (
      initializationVector == null || initializationVector.getIV.length != initializationVectorBytes
    )
      throw new Token4sException("Invalid Initialization Vector size.")
    if (cipherText == null || cipherText.length % cipherTextBlockSize != 0)
      throw new Token4sException("Invalid ciphertext size.")
    if (hmac == null || hmac.length != signatureBytes)
      throw new Token4sException("Invalid HMAC size.")
    new Token(version, timestamp, initializationVector, cipherText, hmac)
  }

}
