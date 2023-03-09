package com.github.imcamilo.exceptions

class OutputValidationException(message: String) extends TokenException(message)

class KeyException(message: String) extends RuntimeException(message)

class TokenException(message: String) extends IllegalArgumentException(message)
