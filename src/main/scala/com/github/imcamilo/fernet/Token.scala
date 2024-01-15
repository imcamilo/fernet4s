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
    *    an object that encapsulates the validation parameters (e.g. TTL). By default we are using a String validator
    *  @tparam A
    *    type of the validator, by default we are using WHStringValidator trait.
    *  @return
    *    the decrypted, deserialised payload of this token
    */
  def validateAndDecrypt[A](key: Key, validator: Validator[A]): Option[A] = {
    validator.validateAndDecrypt(key, this) match {
      case Failure(exception) =>
        logger.error(
          "exception validating and decrypting key - " + exception.getMessage
        )
        None
      case Success(value) =>
        Option(value)
    }
  }

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

  def validateAndDecrypt(
      key: Key,
      earliestValidInstant: Instant,
      latestValidInstant: Instant
  ): Array[Byte] = {
    if (version != 0x80.toByte) {
      throw new Token4sException("Invalid version");
    } else if (!timestamp.isAfter(earliestValidInstant)) {
      throw new Token4sException("Token is expired");
    } else if (!timestamp.isBefore(latestValidInstant)) {
      throw new Token4sException(
        "Token timestamp is in the future (clock skew)."
      );
    } else if (!isValidSignature(key)) {
      throw new Token4sException("Signature does not match.");
    }
    Key.decrypt(cipherText, initializationVector, key.encryptionKey)
  }
}

object Token {

  import Constants._

  private val logger = LoggerFactory.getLogger(getClass)

  /** Convenience method to generate a new Fernet token with a string payload.
    *
    *  @param key
    *    the secret key for encrypting <em>plainText</em> and signing the token
    *  @param plainText
    *    the payload to embed in the token
    *  @return
    *    a unique Fernet token
    */
  def apply(key: Key, plainText: String): Option[Token] = {
    apply(new SecureRandom, key, plainText)
  }

  /** Convenience method to generate a new Fernet token with a string payload.
    *  @param random
    *    a source of entropy for your application
    *  @param key
    *    the secret key for encrypting <em>plainText</em> and signing the token
    *  @param plainText
    *    the payload to embed in the token
    *  @return
    *    a unique Fernet token
    */
  def apply(
      random: SecureRandom,
      key: Key,
      plainText: String
  ): Option[Token] = {
    apply(random, key, plainText.getBytes(charset))
  }

  /** Generate a new Fernet token.
    *  @param random
    *    a source of entropy for your application
    *  @param key
    *    the secret key for encrypting payload and signing the token
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

  def serialise(breadcrumbToken: Token): String = {
    Using(
      new ByteArrayOutputStream(
        tokenStaticBytes + breadcrumbToken.cipherText.length
      )
    ) { byteStream =>
      writeTo(byteStream, breadcrumbToken)
      return encoder.encodeToString(byteStream.toByteArray)
    }.recover {
      case e =>
        throw new IllegalStateException(e.getMessage, e)
    }.get
  }

  def writeTo(outputStream: OutputStream, breadcrumbToken: Token): Try[Unit] = {
    Using(new DataOutputStream(outputStream)) { dataStream =>
      dataStream.writeByte(breadcrumbToken.version)
      dataStream.writeLong(breadcrumbToken.timestamp.getEpochSecond)
      dataStream.write(breadcrumbToken.initializationVector.getIV)
      dataStream.write(breadcrumbToken.cipherText)
      dataStream.write(breadcrumbToken.hmac)
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
    val retval = new Array[Byte](initializationVectorBytes)
    random.nextBytes(retval)
    retval
  }

  protected def read(stream: DataInputStream, numBytes: Int): Array[Byte] = {
    val retval = new Array[Byte](numBytes)
    val bytesRead = stream.read(retval)
    if (bytesRead < numBytes)
      throw new Token4sException("Not enough bits to generate a Token")
    retval
  }

  /** Deserialise a Base64 URL Fernet token string. This does NOT validate that the token was generated using a valid
    *  Key.
    *  @param string
    *    the Base 64 URL encoding of a token in the form Version | Timestamp | IV | Ciphertext | HMAC
    *  @return
    *    a new WHToken
    */
  def fromString(string: String): Option[Token] = {
    fromBytes(decoder.decode(string)) match {
      case Failure(exception) =>
        logger.error("exception decoding from bytes - " + exception.getMessage)
        None
      case Success(value) =>
        Option(value)
    }
  }

  /** Read a Token from bytes. This does NOT validate that the token was generated using a valid Key.
    *  @param bytes
    *    a Fernet token in the form Version | Timestamp | IV | Ciphertext | HMAC
    *  @return
    *    a new WHToken
    */
  def fromBytes(bytes: Array[Byte]): Try[Token] = {

    if (bytes.length < minimumTokenBytes)
      throw new Token4sException("Not enough bits to generate a Token")

    val response = Using(new ByteArrayInputStream(bytes)) { inputStream =>
      Using(new DataInputStream(inputStream)) { dataStream =>
        val version = dataStream.readByte
        val timestampSeconds = dataStream.readLong
        val initializationVector = read(dataStream, initializationVectorBytes)
        val cipherText = read(dataStream, bytes.length - tokenStaticBytes)
        val hmac = read(dataStream, signatureBytes)
        if (dataStream.read() != -1)
          throw new Token4sException("more bits found")
        Token.initializeToken(
          version,
          Instant.ofEpochSecond(timestampSeconds),
          new IvParameterSpec(initializationVector),
          cipherText,
          hmac
        )
      }
    }

    response match {
      case Success(value) => value
      case Failure(ioe)   => throw new IllegalStateException(ioe.getMessage, ioe);
    }

  }

  /** Initialise a new Token from raw components. No validation of the signature is performed. However, the other fields
    *  are validated to ensure they conform to the Fernet specification. Warning: Subsequent modifications to the input
    *  arrays will write through to this object.
    *  @param version
    *    The version of the Fernet token specification. Currently, only 0x80 is supported.
    *  @param timestamp
    *    the time the token was generated
    *  @param initializationVector
    *    the randomly-generated bytes used to initialise the encryption cipher
    *  @param cipherText
    *    the encrypted the encrypted payload
    *  @param hmac
    *    the signature of the token
    *  @return
    *    the final WHToken
    */
  def initializeToken(
      version: Byte,
      timestamp: Instant,
      initializationVector: IvParameterSpec,
      cipherText: Array[Byte],
      hmac: Array[Byte]
  ): Token = {
    if (version != supportedVersion)
      throw new Token4sException("Unsupported version: " + version)
    if (timestamp == null)
      throw new Token4sException("timestamp cannot be null")
    if (
      initializationVector == null || initializationVector.getIV.length != initializationVectorBytes
    )
      throw new Token4sException("Initialization Vector must be 128 bits")
    if (cipherText == null || cipherText.length % cipherTextBlockSize != 0)
      throw new Token4sException("Ciphertext must be a multiple of 128 bits")
    if (hmac == null || hmac.length != signatureBytes)
      throw new Token4sException("hmac must be 256 bits")
    new Token(version, timestamp, initializationVector, cipherText, hmac)
  }

}
