package com.github.imcamilo.exceptions

class OutputValidationException(message: String) extends WHTokenException(message)

class WHKeyException(message: String) extends RuntimeException(message)

class WHTokenException(message: String) extends IllegalArgumentException(message)
