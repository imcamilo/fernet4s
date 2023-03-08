package com.github.imcamilo.fernet

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64.{Decoder, Encoder, getUrlDecoder, getUrlEncoder}

object Constants {

  val decoder: Decoder = getUrlDecoder
  val encoder: Encoder = getUrlEncoder
  val charset: Charset = UTF_8
  val signingKeyBytes: Int = 16
  val supportedVersion: Byte = 0x80.toByte
  val signingAlgorithm: String = "HmacSHA256"
  val encryptionAlgorithm: String = "AES";
  val cipherTransformation: String = encryptionAlgorithm + "/CBC/PKCS5Padding";
  val cipherTextBlockSize: Int = 16
  val signatureBytes: Int = 32
  val timestampBytes: Int = 8
  val initializationVectorBytes: Int = 16
  val encryptionKeyBytes: Int = 16
  val versionBytes: Int = 1
  val tokenStaticBytes: Int = versionBytes + timestampBytes + initializationVectorBytes + signatureBytes;
  val minimumTokenBytes: Int = tokenStaticBytes + cipherTextBlockSize;
  val tokenPrefixBytes: Int = versionBytes + timestampBytes + initializationVectorBytes;
  val fernetKeyBytes: Int = signingKeyBytes + encryptionKeyBytes;

}
