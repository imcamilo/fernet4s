package com.github.imcamilo.exceptions

class Key4sException(message: String, cause: Throwable = null)
  extends RuntimeException(message, cause)

class Token4sException(message: String, cause: Throwable = null)
  extends IllegalArgumentException(message, cause)

class InvalidKeyException(message: String, cause: Throwable = null)
  extends Key4sException(message, cause)

class KeyFormatException(message: String, cause: Throwable = null)
  extends Key4sException(message, cause)

class TokenValidationException(message: String, cause: Throwable = null)
  extends Token4sException(message, cause)

class TokenExpiredException(message: String, cause: Throwable = null)
  extends Token4sException(message, cause)

// Excepción para cuando una operación no está permitida
class UnauthorizedOperationException(message: String, cause: Throwable = null)
  extends Token4sException(message, cause)

// Excepción para errores de validación generales en la salida
class OutputValidationException(message: String, cause: Throwable = null)
  extends Token4sException(message, cause)
