package com.github.imcamilo.fernet

import com.github.imcamilo.validators.{StandardValidator, Validator}
import cats.implicits._
import scala.util.Try
import java.security.SecureRandom
import java.time.Duration

/** Fernet - Symmetric encryption that makes sure your data cannot be manipulated or read without the key.
  *
  * Fernet is built on top of:
  * - AES 128 encryption in CBC mode
  * - HMAC-SHA256 for authentication
  * - Timestamp for TTL support
  *
  * This is a simple, elegant, and functional API for working with Fernet tokens in Scala.
  *
  * @example
  * {{{
  * // Generate a new key
  * val key = Fernet.generateKey()
  *
  * // Encrypt data
  * val encrypted = Fernet.encrypt("Hello, Fernet!", key)
  *
  * // Decrypt data
  * val decrypted = Fernet.decrypt(encrypted, key)
  * }}}
  */
object Fernet {

  /** Generate a new Fernet key.
    *
    * @return a new randomly generated Fernet Key
    */
  def generateKey(): Key = {
    val random = new SecureRandom()
    val signingKey = new Array[Byte](Constants.signingKeyBytes)
    val encryptionKey = new Array[Byte](Constants.encryptionKeyBytes)

    random.nextBytes(signingKey)
    random.nextBytes(encryptionKey)

    new Key(signingKey, encryptionKey)
  }

  /** Generate a new Fernet key from a Base64 URL encoded string.
    *
    * @param keyString Base64 URL encoded key string
    * @return Either an error message or a Key
    */
  def keyFromString(keyString: String): Either[String, Key] = {
    Try(Key(keyString)).toOption.flatten.toRight("Invalid key format")
  }

  /** Encode a Key to Base64 URL string format.
    *
    * @param key the Fernet key
    * @return Base64 URL encoded string representation of the key
    */
  def keyToString(key: Key): String = {
    val concatenated = key.signingKey ++ key.encryptionKey
    Constants.encoder.encodeToString(concatenated)
  }

  /** Encrypt plaintext and generate a Fernet token.
    *
    * @param plainText the data to encrypt
    * @param key the Fernet key
    * @return Either an error message or the encrypted token string
    */
  def encrypt(plainText: String, key: Key): Either[String, String] = {
    for {
      token <- Token(key, plainText).toRight("Failed to create token")
      tokenString = Token.serialize(token)
    } yield tokenString
  }

  /** Encrypt bytes and generate a Fernet token.
    *
    * @param payload the bytes to encrypt
    * @param key the Fernet key
    * @return Either an error message or the encrypted token string
    */
  def encryptBytes(payload: Array[Byte], key: Key): Either[String, String] = {
    for {
      token <- Token(new SecureRandom(), key, payload).toRight("Failed to create token")
      tokenString = Token.serialize(token)
    } yield tokenString
  }

  /** Decrypt a Fernet token and return the plaintext.
    *
    * @param tokenString the encrypted token string
    * @param key the Fernet key
    * @param ttlSeconds optional time-to-live in seconds (default: no expiration)
    * @return Either an error message or the decrypted plaintext
    */
  def decrypt(
      tokenString: String,
      key: Key,
      ttlSeconds: Option[Long] = None
  ): Either[String, String] = {
    val validator = ttlSeconds match {
      case Some(seconds) => customValidator(Duration.ofSeconds(seconds))
      case None => StandardValidator.validator
    }

    for {
      token <- Token.fromString(tokenString).toRight("Invalid token format")
      decrypted <- token.validateAndDecrypt(key, validator).toRight("Decryption failed")
    } yield decrypted
  }

  /** Decrypt a Fernet token and return the bytes.
    *
    * @param tokenString the encrypted token string
    * @param key the Fernet key
    * @param ttlSeconds optional time-to-live in seconds (default: no expiration)
    * @return Either an error message or the decrypted bytes
    */
  def decryptBytes(
      tokenString: String,
      key: Key,
      ttlSeconds: Option[Long] = None
  ): Either[String, Array[Byte]] = {
    decrypt(tokenString, key, ttlSeconds).map(_.getBytes(Constants.charset))
  }

  /** Verify a Fernet token without decrypting.
    *
    * @param tokenString the token string to verify
    * @param key the Fernet key
    * @param ttlSeconds optional time-to-live in seconds (default: no expiration)
    * @return Either an error message or true if valid
    */
  def verify(
      tokenString: String,
      key: Key,
      ttlSeconds: Option[Long] = None
  ): Either[String, Boolean] = {
    Try {
      decrypt(tokenString, key, ttlSeconds) match {
        case Right(_) => Right(true)
        case Left(err) => Left(err)
      }
    }.toOption.getOrElse(Left("Token verification failed"))
  }

  /** Create a custom validator with specific TTL.
    *
    * @param ttl the time-to-live duration
    * @return a String validator with the specified TTL
    */
  private def customValidator(ttl: Duration): Validator[String] = {
    new com.github.imcamilo.validators.StringValidator {
      override def getTimeToLive = ttl
    }
  }

  /** Functional API for chaining operations. */
  object syntax {

    implicit class KeyOps(val key: Key) extends AnyVal {
      def encrypt(plainText: String): Either[String, String] =
        Fernet.encrypt(plainText, key)

      def encryptBytes(payload: Array[Byte]): Either[String, String] =
        Fernet.encryptBytes(payload, key)

      def decrypt(tokenString: String, ttlSeconds: Option[Long] = None): Either[String, String] =
        Fernet.decrypt(tokenString, key, ttlSeconds)

      def decryptBytes(tokenString: String, ttlSeconds: Option[Long] = None): Either[String, Array[Byte]] =
        Fernet.decryptBytes(tokenString, key, ttlSeconds)

      def verify(tokenString: String, ttlSeconds: Option[Long] = None): Either[String, Boolean] =
        Fernet.verify(tokenString, key, ttlSeconds)

      def toBase64: String =
        Fernet.keyToString(key)
    }

    implicit class StringOps(val keyString: String) extends AnyVal {
      def asFernetKey: Either[String, Key] =
        Fernet.keyFromString(keyString)
    }
  }
}
