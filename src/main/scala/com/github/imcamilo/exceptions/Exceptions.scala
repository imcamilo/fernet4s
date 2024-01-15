package com.github.imcamilo.exceptions

class OutputValidationException(message: String)
    extends Token4sException(message)

class Key4sException(message: String) extends RuntimeException(message)

class Token4sException(message: String)
    extends IllegalArgumentException(message)

class InvalidKeyException(message: String) extends Key4sException(message)
