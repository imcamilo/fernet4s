package examples

import com.github.imcamilo.fernet.Fernet
import com.github.imcamilo.fernet.Fernet.syntax._

object SimpleExample extends App {

  // Example 1: Basic encryption/decryption
  println("=== Example 1: Basic Usage ===")
  val key = Fernet.generateKey()
  val message = "Hello, Fernet!"

  val encrypted = Fernet.encrypt(message, key)
  encrypted match {
    case Right(token) =>
      println(s"Encrypted: $token")

      val decrypted = Fernet.decrypt(token, key)
      decrypted match {
        case Right(plain) => println(s"Decrypted: $plain")
        case Left(error) => println(s"Decryption failed: $error")
      }
    case Left(error) => println(s"Encryption failed: $error")
  }

  // Example 2: Using syntax extensions
  println("\n=== Example 2: Syntax Extensions ===")
  val key2 = Fernet.generateKey()
  val result = for {
    token <- key2.encrypt("Secret data")
    plain <- key2.decrypt(token)
  } yield plain

  result match {
    case Right(data) => println(s"Successfully encrypted and decrypted: $data")
    case Left(error) => println(s"Error: $error")
  }

  // Example 3: Key serialization
  println("\n=== Example 3: Key Serialization ===")
  val key3 = Fernet.generateKey()
  val keyString = key3.toBase64
  println(s"Key as Base64: $keyString")

  val importedKey = keyString.asFernetKey
  importedKey match {
    case Right(k) => println("Key successfully imported!")
    case Left(error) => println(s"Import failed: $error")
  }

  // Example 4: TTL (Time-To-Live)
  println("\n=== Example 4: TTL Support ===")
  val key4 = Fernet.generateKey()
  val tokenResult = key4.encrypt("Temporary data")

  tokenResult.flatMap { token =>
    // Decrypt with 60 seconds TTL
    key4.decrypt(token, ttlSeconds = Some(60))
  } match {
    case Right(data) => println(s"Valid token: $data")
    case Left(error) => println(s"Token validation failed: $error")
  }

  // Example 5: Binary data
  println("\n=== Example 5: Binary Data ===")
  val key5 = Fernet.generateKey()
  val binaryData = Array[Byte](1, 2, 3, 4, 5)

  val encryptedBinary = Fernet.encryptBytes(binaryData, key5)
  encryptedBinary.flatMap { token =>
    Fernet.decryptBytes(token, key5)
  } match {
    case Right(data) => println(s"Binary data: ${data.mkString(", ")}")
    case Left(error) => println(s"Error: $error")
  }

  // Example 6: Token verification
  println("\n=== Example 6: Token Verification ===")
  val key6 = Fernet.generateKey()
  val verifyResult = for {
    token <- key6.encrypt("Data to verify")
    isValid <- key6.verify(token)
  } yield isValid

  verifyResult match {
    case Right(valid) => println(s"Token is valid: $valid")
    case Left(error) => println(s"Verification failed: $error")
  }
}
