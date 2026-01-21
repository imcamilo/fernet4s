package com.github.imcamilo.fernet

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64.{Decoder, Encoder, getUrlDecoder, getUrlEncoder}

/** Constants and specifications for Fernet token format.
  *
  * This object contains all the constants defined in the
  * [[https://github.com/fernet/spec/blob/master/Spec.md Fernet specification]].
  *
  * ==Token Format==
  * {{{
  * Version (1 byte) || Timestamp (8 bytes) || IV (16 bytes) || Ciphertext (variable) || HMAC (32 bytes)
  * }}}
  *
  * @since 0.1.0
  */
object Constants {

  /** Base64 URL decoder for Fernet tokens and keys. */
  val decoder: Decoder = getUrlDecoder

  /** Base64 URL encoder for Fernet tokens and keys. */
  val encoder: Encoder = getUrlEncoder

  /** Character set used for text encoding (UTF-8). */
  val charset: Charset = UTF_8

  /** Size of signing key in bytes (128 bits). */
  val signingKeyBytes: Int = 16

  /** Supported Fernet token version (0x80). */
  val supportedVersion: Byte = 0x80.toByte

  /** HMAC algorithm used for signing (HMAC-SHA256). */
  val signingAlgorithm: String = "HmacSHA256"

  /** Encryption algorithm (AES). */
  val encryptionAlgorithm: String = "AES"

  /** Full cipher transformation (AES/CBC/PKCS5Padding). */
  val cipherTransformation: String = encryptionAlgorithm + "/CBC/PKCS5Padding"

  /** Ciphertext block size in bytes (128 bits for AES). */
  val cipherTextBlockSize: Int = 16

  /** HMAC signature size in bytes (256 bits for SHA256). */
  val signatureBytes: Int = 32

  /** Timestamp field size in bytes. */
  val timestampBytes: Int = 8

  /** Initialization vector size in bytes (128 bits). */
  val initializationVectorBytes: Int = 16

  /** Encryption key size in bytes (128 bits). */
  val encryptionKeyBytes: Int = 16

  /** Version field size in bytes. */
  val versionBytes: Int = 1

  /** Total size of static token fields (version + timestamp + IV + HMAC). */
  val tokenStaticBytes: Int = versionBytes + timestampBytes + initializationVectorBytes + signatureBytes

  /** Minimum valid token size in bytes. */
  val minimumTokenBytes: Int = tokenStaticBytes + cipherTextBlockSize

  /** Size of token prefix (version + timestamp + IV). */
  val tokenPrefixBytes: Int = versionBytes + timestampBytes + initializationVectorBytes

  /** Total Fernet key size in bytes (signing key + encryption key). */
  val fernetKeyBytes: Int = signingKeyBytes + encryptionKeyBytes
}
