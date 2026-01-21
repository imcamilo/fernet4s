package com.github.imcamilo.exceptions

/** Base exception for key-related errors in Fernet operations.
  *
  * @param message the error message
  * @param cause optional underlying cause
  * @since 0.1.0
  */
class Key4sException(message: String, cause: Throwable = null)
    extends RuntimeException(message, cause)

/** Base exception for token-related errors in Fernet operations.
  *
  * Thrown when token validation, parsing, or decryption fails.
  *
  * @param message the error message
  * @param cause optional underlying cause
  * @since 0.1.0
  */
class Token4sException(message: String, cause: Throwable = null)
    extends IllegalArgumentException(message, cause)

/** Exception thrown when a Fernet key is invalid or malformed.
  *
  * Common causes:
  *  - Key is not 32 bytes (256 bits)
  *  - Invalid Base64 encoding
  *  - Corrupted key data
  *
  * @param message the error message
  * @param cause optional underlying cause
  * @since 0.1.0
  */
class InvalidKeyException(message: String, cause: Throwable = null)
    extends Key4sException(message, cause)

/** Exception thrown when a key format is invalid during parsing.
  *
  * @param message the error message
  * @param cause optional underlying cause
  * @since 0.1.0
  */
class KeyFormatException(message: String, cause: Throwable = null)
    extends Key4sException(message, cause)

/** Exception thrown when token validation fails.
  *
  * Common causes:
  *  - Invalid HMAC signature (tampering detected)
  *  - Malformed token structure
  *  - Unsupported token version
  *
  * @param message the error message
  * @param cause optional underlying cause
  * @since 0.1.0
  */
class TokenValidationException(message: String, cause: Throwable = null)
    extends Token4sException(message, cause)

/** Exception thrown when a token has expired (TTL exceeded).
  *
  * @param message the error message
  * @param cause optional underlying cause
  * @since 0.1.0
  */
class TokenExpiredException(message: String, cause: Throwable = null)
    extends Token4sException(message, cause)

/** Exception thrown when an operation is not permitted.
  *
  * @param message the error message
  * @param cause optional underlying cause
  * @since 0.1.0
  */
class UnauthorizedOperationException(message: String, cause: Throwable = null)
    extends Token4sException(message, cause)

/** Exception thrown when output validation fails after decryption.
  *
  * This can occur when:
  *  - Decrypted data doesn't match expected format
  *  - Custom validator rejects the output
  *  - Data corruption detected
  *
  * @param message the error message
  * @param cause optional underlying cause
  * @since 0.1.0
  */
class OutputValidationException(message: String, cause: Throwable = null)
    extends Token4sException(message, cause)
