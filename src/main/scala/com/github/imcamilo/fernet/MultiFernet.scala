package com.github.imcamilo.fernet

import scala.util.Try

/** MultiFernet allows working with multiple Fernet keys for key rotation.
  *
  * This is useful when you need to:
  * - Rotate keys without breaking existing tokens
  * - Decrypt tokens that may have been encrypted with old keys
  * - Gradually migrate to a new key
  *
  * @example
  * {{{
  * val oldKey = Fernet.generateKey()
  * val newKey = Fernet.generateKey()
  * val multiFernet = MultiFernet(newKey, oldKey)
  *
  * // Encrypts with the first key (newKey)
  * val encrypted = multiFernet.encrypt("data")
  *
  * // Decrypts trying each key in order
  * val decrypted = multiFernet.decrypt(token)
  * }}}
  *
  * @param keys List of Fernet keys, first key is used for encryption
  */
class MultiFernet(val keys: List[Key]) {

  require(keys.nonEmpty, "At least one key is required")

  /** Encrypt data using the first (primary) key.
    *
    * @param plainText the data to encrypt
    * @return Either an error or the encrypted token
    */
  def encrypt(plainText: String): Either[String, String] = {
    Fernet.encrypt(plainText, keys.head)
  }

  /** Encrypt binary data using the first (primary) key.
    *
    * @param payload the bytes to encrypt
    * @return Either an error or the encrypted token
    */
  def encryptBytes(payload: Array[Byte]): Either[String, String] = {
    Fernet.encryptBytes(payload, keys.head)
  }

  /** Decrypt a token trying each key in order until one succeeds.
    *
    * @param tokenString the token to decrypt
    * @param ttlSeconds optional TTL in seconds
    * @return Either an error or the decrypted plaintext
    */
  def decrypt(
      tokenString: String,
      ttlSeconds: Option[Long] = None
  ): Either[String, String] = {
    keys.foldLeft[Either[String, String]](Left("No valid key found")) {
      case (Right(result), _) => Right(result) // Already decrypted
      case (Left(_), key) =>
        Fernet.decrypt(tokenString, key, ttlSeconds) match {
          case success @ Right(_) => success
          case Left(_) => Left("No valid key found") // Try next key
        }
    }
  }

  /** Decrypt binary data trying each key in order.
    *
    * @param tokenString the token to decrypt
    * @param ttlSeconds optional TTL in seconds
    * @return Either an error or the decrypted bytes
    */
  def decryptBytes(
      tokenString: String,
      ttlSeconds: Option[Long] = None
  ): Either[String, Array[Byte]] = {
    keys.foldLeft[Either[String, Array[Byte]]](Left("No valid key found")) {
      case (Right(result), _) => Right(result)
      case (Left(_), key) =>
        Fernet.decryptBytes(tokenString, key, ttlSeconds) match {
          case success @ Right(_) => success
          case Left(_) => Left("No valid key found")
        }
    }
  }

  /** Rotate a token to be encrypted with the primary key.
    *
    * This decrypts the token with any available key and re-encrypts
    * with the first (primary) key.
    *
    * @param tokenString the token to rotate
    * @param ttlSeconds optional TTL for validation
    * @return Either an error or the new token encrypted with primary key
    */
  def rotate(
      tokenString: String,
      ttlSeconds: Option[Long] = None
  ): Either[String, String] = {
    for {
      plainText <- decrypt(tokenString, ttlSeconds)
      newToken <- encrypt(plainText)
    } yield newToken
  }

  /** Add a new key to the end of the key list.
    *
    * @param key the key to add
    * @return a new MultiFernet instance with the added key
    */
  def addKey(key: Key): MultiFernet = {
    new MultiFernet(keys :+ key)
  }

  /** Set a new primary key (used for encryption).
    *
    * The new key will be added to the front of the list.
    *
    * @param key the new primary key
    * @return a new MultiFernet instance with the new primary key
    */
  def setPrimaryKey(key: Key): MultiFernet = {
    new MultiFernet(key :: keys)
  }

  /** Remove a key from the key list.
    *
    * @param keyString the Base64 string of the key to remove
    * @return a new MultiFernet instance without the specified key
    */
  def removeKey(keyString: String): Either[String, MultiFernet] = {
    val filteredKeys = keys.filterNot(k => Fernet.keyToString(k) == keyString)

    if (filteredKeys.isEmpty) {
      Left("Cannot remove all keys")
    } else {
      Right(new MultiFernet(filteredKeys))
    }
  }
}

object MultiFernet {

  /** Create a MultiFernet instance from a list of keys.
    *
    * @param keys variable number of keys (first is primary)
    * @return a MultiFernet instance
    */
  def apply(keys: Key*): MultiFernet = {
    new MultiFernet(keys.toList)
  }

  /** Create a MultiFernet instance from Base64 key strings.
    *
    * @param keyStrings variable number of Base64 key strings
    * @return Either an error or a MultiFernet instance
    */
  def fromStrings(keyStrings: String*): Either[String, MultiFernet] = {
    val keysResult = keyStrings.map(Fernet.keyFromString).toList

    val errors = keysResult.collect { case Left(err) => err }
    if (errors.nonEmpty) {
      Left(s"Invalid keys: ${errors.mkString(", ")}")
    } else {
      val keys = keysResult.collect { case Right(key) => key }
      Right(new MultiFernet(keys))
    }
  }

  /** Syntax extensions for MultiFernet. */
  object syntax {
    implicit class MultiKeyOps(val multiFernet: MultiFernet) extends AnyVal {
      def encrypt(plainText: String): Either[String, String] =
        multiFernet.encrypt(plainText)

      def encryptBytes(payload: Array[Byte]): Either[String, String] =
        multiFernet.encryptBytes(payload)

      def decrypt(
          tokenString: String,
          ttlSeconds: Option[Long] = None
      ): Either[String, String] =
        multiFernet.decrypt(tokenString, ttlSeconds)

      def decryptBytes(
          tokenString: String,
          ttlSeconds: Option[Long] = None
      ): Either[String, Array[Byte]] =
        multiFernet.decryptBytes(tokenString, ttlSeconds)

      def rotate(
          tokenString: String,
          ttlSeconds: Option[Long] = None
      ): Either[String, String] =
        multiFernet.rotate(tokenString, ttlSeconds)
    }
  }
}
