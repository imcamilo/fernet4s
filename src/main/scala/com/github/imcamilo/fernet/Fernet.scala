package com.github.imcamilo.fernet

import com.github.imcamilo.validators.{StandardValidator, Validator}
import scala.util.Try
import java.security.SecureRandom
import java.time.Duration

/** Fernet - Symmetric encryption that makes sure your data cannot be manipulated or read without the key.
  *
  * Fernet is built on top of:
  *  - '''AES-128-CBC''' for encryption
  *  - '''HMAC-SHA256''' for authentication
  *  - '''Timestamp''' for TTL (time-to-live) support
  *
  * This implementation follows the [[https://github.com/fernet/spec/blob/master/Spec.md Fernet specification]]
  * and is compatible with implementations in Python, Ruby, Go, and other languages.
  *
  * ==Overview==
  * Fernet provides a simple API for symmetric encryption with built-in authentication and optional expiration.
  * All operations return `Either[String, A]` for type-safe error handling.
  *
  * ==Key Management==
  * Keys should be:
  *  - Generated using [[generateKey()]]
  *  - Stored securely (environment variables, secret managers, etc.)
  *  - Never hardcoded in source code
  *  - Rotated periodically using [[MultiFernet]]
  *
  * ==Security Properties==
  *  - '''Confidentiality''': Data is encrypted with AES-128-CBC
  *  - '''Integrity''': HMAC-SHA256 ensures data hasn't been tampered with
  *  - '''Authenticity''': Only someone with the key can create valid tokens
  *  - '''Freshness''': Optional TTL prevents replay attacks with old tokens
  *
  * @example Basic encryption/decryption:
  * {{{
  * import com.github.imcamilo.fernet.Fernet
  *
  * val key = Fernet.generateKey()
  * val encrypted = Fernet.encrypt("Hello, Fernet!", key)
  * val decrypted = Fernet.decrypt(encrypted.toOption.get, key)
  * println(decrypted) // Right("Hello, Fernet!")
  * }}}
  *
  * @example Using fluent syntax:
  * {{{
  * import com.github.imcamilo.fernet.Fernet.syntax._
  *
  * val key = Fernet.generateKey()
  * val result = for {
  *   token <- key.encrypt("Secret data")
  *   plain <- key.decrypt(token)
  * } yield plain
  * }}}
  *
  * @example With TTL (time-to-live):
  * {{{
  * val key = Fernet.generateKey()
  * val token = key.encrypt("Temporary data").toOption.get
  *
  * // Valid for 60 seconds
  * key.decrypt(token, ttlSeconds = Some(60))
  * }}}
  *
  * @see [[MultiFernet]] for key rotation support
  * @see [[https://github.com/fernet/spec Fernet Specification]]
  * @since 0.1.0
  */
object Fernet {

  /** Generates a new cryptographically secure Fernet key.
    *
    * The key consists of:
    *  - 128-bit (16-byte) signing key for HMAC
    *  - 128-bit (16-byte) encryption key for AES
    *
    * Keys are generated using [[java.security.SecureRandom]] for cryptographic strength.
    *
    * @return a new randomly generated Fernet key
    *
    * @example
    * {{{
    * val key = Fernet.generateKey()
    * // Store securely: val keyString = Fernet.keyToString(key)
    * }}}
    *
    * @note Keys should be stored securely and never exposed in logs or error messages
    * @since 0.1.0
    */
  def generateKey(): Key = {
    val random = new SecureRandom()
    val signingKey = new Array[Byte](Constants.signingKeyBytes)
    val encryptionKey = new Array[Byte](Constants.encryptionKeyBytes)

    random.nextBytes(signingKey)
    random.nextBytes(encryptionKey)

    new Key(signingKey, encryptionKey)
  }

  /** Imports a Fernet key from a Base64 URL-encoded string.
    *
    * The string must be a valid Fernet key in the format:
    * `Base64URL(signing_key || encryption_key)` where each key is 128 bits.
    *
    * @param keyString Base64 URL-encoded key string (44 characters)
    * @return Right(key) if valid, Left(error) if invalid format
    *
    * @example
    * {{{
    * val keyString = "cw_0x689RpI-jtRR7oE8h_eQsKImvJapLeSbXpwF4e4="
    * Fernet.keyFromString(keyString) match {
    *   case Right(key) => // Use key
    *   case Left(error) => // Handle error
    * }
    * }}}
    *
    * @note The key string must be exactly 44 characters (32 bytes base64url encoded)
    * @since 0.1.0
    */
  def keyFromString(keyString: String): Either[String, Key] = {
    Try(Key(keyString)).toOption.flatten.toRight("Invalid key format")
  }

  /** Imports a Fernet key from a Base64 URL-encoded string (Java-friendly).
    *
    * Returns a [[Result]] which is easier to use from Java than [[Either]].
    *
    * @param keyString Base64 URL-encoded key string (44 characters)
    * @return Result containing the key if valid, failure with error message if invalid
    *
    * @example Java usage:
    * {{{
    * Result<Key> result = Fernet.keyFromStringResult("cw_0x689RpI...");
    * if (result.isSuccess()) {
    *     Key key = result.get();
    * } else {
    *     String error = result.getError();
    * }
    * }}}
    *
    * @since 1.0.0
    */
  def keyFromStringResult(keyString: String): Result[Key] = {
    Result.fromEither(keyFromString(keyString))
  }

  /** Exports a Fernet key to a Base64 URL-encoded string.
    *
    * The resulting string can be stored in:
    *  - Environment variables
    *  - Configuration files
    *  - Secret management systems (AWS Secrets Manager, HashiCorp Vault, etc.)
    *
    * @param key the Fernet key to export
    * @return Base64 URL-encoded string representation (44 characters)
    *
    * @example
    * {{{
    * val key = Fernet.generateKey()
    * val keyString = Fernet.keyToString(key)
    * // Store: System.setenv("FERNET_KEY", keyString)
    * }}}
    *
    * @note Store the key string securely - anyone with this string can decrypt your data
    * @since 0.1.0
    */
  def keyToString(key: Key): String = {
    val concatenated = key.signingKey ++ key.encryptionKey
    Constants.encoder.encodeToString(concatenated)
  }

  /** Encrypts plaintext and generates a Fernet token.
    *
    * The token format is:
    * {{{
    * Base64URL(Version || Timestamp || IV || Ciphertext || HMAC)
    * }}}
    *
    * Where:
    *  - Version: 1 byte (always 0x80)
    *  - Timestamp: 8 bytes (seconds since Unix epoch)
    *  - IV: 16 bytes (random initialization vector)
    *  - Ciphertext: variable length (AES-128-CBC encrypted payload)
    *  - HMAC: 32 bytes (HMAC-SHA256 signature)
    *
    * @param plainText the data to encrypt (will be UTF-8 encoded)
    * @param key the Fernet key for encryption
    * @return Right(token) if successful, Left(error) if encryption fails
    *
    * @example
    * {{{
    * val key = Fernet.generateKey()
    * Fernet.encrypt("sensitive data", key) match {
    *   case Right(token) => // Store or transmit token
    *   case Left(error) => // Handle error
    * }
    * }}}
    *
    * @note Tokens are non-deterministic (include timestamp and random IV)
    * @since 0.1.0
    */
  def encrypt(plainText: String, key: Key): Either[String, String] = {
    for {
      token <- Token(key, plainText).toRight("Failed to create token")
      tokenString <- Token.serialize(token).toOption.toRight("Failed to serialize token")
    } yield tokenString
  }

  /** Encrypts plaintext and generates a Fernet token (Java-friendly).
    *
    * Returns a [[Result]] which is easier to use from Java than [[Either]].
    *
    * @param plainText the data to encrypt (will be UTF-8 encoded)
    * @param key the Fernet key for encryption
    * @return Result containing the token if successful, failure with error if encryption fails
    *
    * @example Java usage:
    * {{{
    * Result<String> result = Fernet.encryptResult("sensitive data", key);
    * if (result.isSuccess()) {
    *     String token = result.get();
    * } else {
    *     String error = result.getError();
    * }
    * }}}
    *
    * @since 1.0.0
    */
  def encryptResult(plainText: String, key: Key): Result[String] = {
    Result.fromEither(encrypt(plainText, key))
  }

  /** Encrypts binary data and generates a Fernet token.
    *
    * Useful for encrypting non-text data like images, files, or serialized objects.
    *
    * @param payload the bytes to encrypt
    * @param key the Fernet key for encryption
    * @return Right(token) if successful, Left(error) if encryption fails
    *
    * @example
    * {{{
    * val key = Fernet.generateKey()
    * val fileData = Files.readAllBytes(Paths.get("secret.bin"))
    * Fernet.encryptBytes(fileData, key)
    * }}}
    *
    * @see [[decryptBytes]] for decryption
    * @since 0.1.0
    */
  def encryptBytes(payload: Array[Byte], key: Key): Either[String, String] = {
    for {
      token <- Token(new SecureRandom(), key, payload).toRight("Failed to create token")
      tokenString <- Token.serialize(token).toOption.toRight("Failed to serialize token")
    } yield tokenString
  }

  /** Encrypts binary data and generates a Fernet token (Java-friendly).
    *
    * Returns a [[Result]] which is easier to use from Java than [[Either]].
    *
    * @param payload the bytes to encrypt
    * @param key the Fernet key for encryption
    * @return Result containing the token if successful, failure with error if encryption fails
    *
    * @example Java usage:
    * {{{
    * byte[] data = Files.readAllBytes(Paths.get("secret.bin"));
    * Result<String> result = Fernet.encryptBytesResult(data, key);
    * if (result.isSuccess()) {
    *     String token = result.get();
    * }
    * }}}
    *
    * @since 1.0.0
    */
  def encryptBytesResult(payload: Array[Byte], key: Key): Result[String] = {
    Result.fromEither(encryptBytes(payload, key))
  }

  /** Decrypts a Fernet token and returns the plaintext.
    *
    * Validation includes:
    *  - Token format and structure
    *  - HMAC signature verification
    *  - Token version check
    *  - Optional TTL verification
    *
    * @param tokenString the encrypted token string
    * @param key the Fernet key for decryption
    * @param ttlSeconds optional time-to-live in seconds (None = no expiration)
    * @return Right(plaintext) if valid, Left(error) if invalid or expired
    *
    * @example Without TTL:
    * {{{
    * val key = Fernet.generateKey()
    * val token = Fernet.encrypt("data", key).toOption.get
    * Fernet.decrypt(token, key) // Always valid (until token is old enough to overflow)
    * }}}
    *
    * @example With TTL:
    * {{{
    * val token = Fernet.encrypt("temporary", key).toOption.get
    * Fernet.decrypt(token, key, ttlSeconds = Some(300)) // Valid for 5 minutes
    * }}}
    *
    * @note TTL is checked against the token's embedded timestamp, not creation time
    * @since 0.1.0
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

  /** Decrypts a Fernet token and returns the plaintext (Java-friendly).
    *
    * Returns a [[Result]] which is easier to use from Java than [[Either]].
    *
    * @param tokenString the encrypted token string
    * @param key the Fernet key for decryption
    * @return Result containing the plaintext if valid, failure with error if invalid
    *
    * @example Java usage:
    * {{{
    * Result<String> result = Fernet.decryptResult(token, key);
    * if (result.isSuccess()) {
    *     String plaintext = result.get();
    * }
    * }}}
    *
    * @since 1.0.0
    */
  def decryptResult(tokenString: String, key: Key): Result[String] = {
    Result.fromEither(decrypt(tokenString, key))
  }

  /** Decrypts a Fernet token with TTL and returns the plaintext (Java-friendly).
    *
    * @param tokenString the encrypted token string
    * @param key the Fernet key for decryption
    * @param ttlSeconds time-to-live in seconds
    * @return Result containing the plaintext if valid, failure with error if invalid or expired
    *
    * @since 1.0.0
    */
  def decryptResult(tokenString: String, key: Key, ttlSeconds: Long): Result[String] = {
    Result.fromEither(decrypt(tokenString, key, Some(ttlSeconds)))
  }

  /** Decrypts a Fernet token and returns the raw bytes.
    *
    * Use this when the encrypted data was binary (not text).
    *
    * @param tokenString the encrypted token string
    * @param key the Fernet key for decryption
    * @param ttlSeconds optional time-to-live in seconds
    * @return Right(bytes) if valid, Left(error) if invalid or expired
    *
    * @example
    * {{{
    * val key = Fernet.generateKey()
    * val data = Array[Byte](1, 2, 3, 4, 5)
    * val token = Fernet.encryptBytes(data, key).toOption.get
    * Fernet.decryptBytes(token, key) // Right(Array(1, 2, 3, 4, 5))
    * }}}
    *
    * @see [[encryptBytes]] for encryption
    * @since 0.1.0
    */
  def decryptBytes(
      tokenString: String,
      key: Key,
      ttlSeconds: Option[Long] = None
  ): Either[String, Array[Byte]] = {
    decrypt(tokenString, key, ttlSeconds).map(_.getBytes(Constants.charset))
  }

  /** Decrypts a Fernet token and returns the raw bytes (Java-friendly).
    *
    * @param tokenString the encrypted token string
    * @param key the Fernet key for decryption
    * @return Result containing the bytes if valid, failure with error if invalid
    *
    * @since 1.0.0
    */
  def decryptBytesResult(tokenString: String, key: Key): Result[Array[Byte]] = {
    Result.fromEither(decryptBytes(tokenString, key))
  }

  /** Decrypts a Fernet token with TTL and returns the raw bytes (Java-friendly).
    *
    * @param tokenString the encrypted token string
    * @param key the Fernet key for decryption
    * @param ttlSeconds time-to-live in seconds
    * @return Result containing the bytes if valid, failure with error if invalid or expired
    *
    * @since 1.0.0
    */
  def decryptBytesResult(tokenString: String, key: Key, ttlSeconds: Long): Result[Array[Byte]] = {
    Result.fromEither(decryptBytes(tokenString, key, Some(ttlSeconds)))
  }

  /** Verifies a Fernet token without decrypting the contents.
    *
    * This is useful when you only need to check if a token is valid
    * without accessing the encrypted data.
    *
    * Verification includes:
    *  - Token format validation
    *  - HMAC signature check
    *  - Optional TTL check
    *
    * @param tokenString the token string to verify
    * @param key the Fernet key for verification
    * @param ttlSeconds optional time-to-live in seconds
    * @return Right(true) if valid, Left(error) if invalid
    *
    * @example
    * {{{
    * val key = Fernet.generateKey()
    * val token = Fernet.encrypt("data", key).toOption.get
    * Fernet.verify(token, key) match {
    *   case Right(true) => // Token is valid
    *   case Left(error) => // Token is invalid
    * }
    * }}}
    *
    * @note This still performs decryption internally but doesn't return the data
    * @since 0.1.0
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

  /** Verifies a Fernet token without decrypting the contents (Java-friendly).
    *
    * @param tokenString the token string to verify
    * @param key the Fernet key for verification
    * @return Result containing true if valid, failure with error if invalid
    *
    * @example Java usage:
    * {{{
    * Result<Boolean> result = Fernet.verifyResult(token, key);
    * if (result.isSuccess()) {
    *     System.out.println("Token is valid");
    * } else {
    *     System.err.println("Invalid: " + result.getError());
    * }
    * }}}
    *
    * @since 1.0.0
    */
  def verifyResult(tokenString: String, key: Key): Result[Boolean] = {
    Result.fromEither(verify(tokenString, key))
  }

  /** Verifies a Fernet token with TTL without decrypting the contents (Java-friendly).
    *
    * @param tokenString the token string to verify
    * @param key the Fernet key for verification
    * @param ttlSeconds time-to-live in seconds
    * @return Result containing true if valid, failure with error if invalid or expired
    *
    * @since 1.0.0
    */
  def verifyResult(tokenString: String, key: Key, ttlSeconds: Long): Result[Boolean] = {
    Result.fromEither(verify(tokenString, key, Some(ttlSeconds)))
  }

  /** Creates a custom validator with specific TTL.
    *
    * @param ttl the time-to-live duration
    * @return a String validator with the specified TTL
    */
  private def customValidator(ttl: Duration): Validator[String] = {
    new com.github.imcamilo.validators.StringValidator {
      override def getTimeToLive = ttl
    }
  }

  /** Syntax extensions for fluent API usage.
    *
    * Import with:
    * {{{
    * import com.github.imcamilo.fernet.Fernet.syntax._
    * }}}
    *
    * Provides extension methods on:
    *  - [[Key]] for direct encryption/decryption
    *  - [[String]] for key deserialization
    *
    * @example
    * {{{
    * import com.github.imcamilo.fernet.Fernet.syntax._
    *
    * val key = Fernet.generateKey()
    * val result = for {
    *   token <- key.encrypt("data")
    *   plain <- key.decrypt(token)
    * } yield plain
    *
    * val keyString = key.toBase64
    * val imported = keyString.asFernetKey
    * }}}
    *
    * @since 0.1.0
    */
  object syntax {

    /** Extension methods for [[Key]].
      *
      * Provides fluent API for encryption/decryption operations directly on key instances.
      *
      * @param key the Fernet key
      * @since 0.1.0
      */
    implicit class KeyOps(val key: Key) extends AnyVal {

      /** Encrypts plaintext using this key.
        *
        * @param plainText the data to encrypt
        * @return Right(token) or Left(error)
        */
      def encrypt(plainText: String): Either[String, String] =
        Fernet.encrypt(plainText, key)

      /** Encrypts binary data using this key.
        *
        * @param payload the bytes to encrypt
        * @return Right(token) or Left(error)
        */
      def encryptBytes(payload: Array[Byte]): Either[String, String] =
        Fernet.encryptBytes(payload, key)

      /** Decrypts a token using this key.
        *
        * @param tokenString the token to decrypt
        * @param ttlSeconds optional TTL in seconds
        * @return Right(plaintext) or Left(error)
        */
      def decrypt(tokenString: String, ttlSeconds: Option[Long] = None): Either[String, String] =
        Fernet.decrypt(tokenString, key, ttlSeconds)

      /** Decrypts a token to bytes using this key.
        *
        * @param tokenString the token to decrypt
        * @param ttlSeconds optional TTL in seconds
        * @return Right(bytes) or Left(error)
        */
      def decryptBytes(tokenString: String, ttlSeconds: Option[Long] = None): Either[String, Array[Byte]] =
        Fernet.decryptBytes(tokenString, key, ttlSeconds)

      /** Verifies a token using this key.
        *
        * @param tokenString the token to verify
        * @param ttlSeconds optional TTL in seconds
        * @return Right(true) or Left(error)
        */
      def verify(tokenString: String, ttlSeconds: Option[Long] = None): Either[String, Boolean] =
        Fernet.verify(tokenString, key, ttlSeconds)

      /** Converts this key to Base64 string.
        *
        * @return Base64 URL-encoded key string
        */
      def toBase64: String =
        Fernet.keyToString(key)
    }

    /** Extension methods for [[String]].
      *
      * Provides key deserialization from Base64 strings.
      *
      * @param keyString the Base64 URL-encoded key string
      * @since 0.1.0
      */
    implicit class StringOps(val keyString: String) extends AnyVal {

      /** Deserializes a Fernet key from this Base64 string.
        *
        * @return Right(key) or Left(error)
        */
      def asFernetKey: Either[String, Key] =
        Fernet.keyFromString(keyString)
    }
  }
}
