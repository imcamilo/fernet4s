package examples

import com.github.imcamilo.fernet.Fernet
import com.github.imcamilo.fernet.Fernet.syntax.*
import com.github.imcamilo.fernet.{MultiFernet, Result}

@main
def completeExample(): Unit =

  println("=== Fernet4s Scala 3 Complete Examples ===\n")

  // Example 1: Basic encryption/decryption
  println("=== Example 1: Basic Encryption/Decryption ===")
  val key = Fernet.generateKey()

  val result = for
    token <- Fernet.encrypt("Hello Fernet from Scala 3!", key)
    plain <- Fernet.decrypt(token, key)
  yield plain

  result match
    case Right(text) => println(s"✓ Decrypted: $text")
    case Left(error) => println(s"✗ Error: $error")

  // Example 2: Syntax extensions
  println("\n=== Example 2: Syntax Extensions ===")
  val token = key.encrypt("Secret message").getOrElse("")
  println(s"Token: ${token.take(40)}...")

  val decrypted = key.decrypt(token).getOrElse("Error")
  println(s"Decrypted: $decrypted")

  // Example 3: Key serialization
  println("\n=== Example 3: Key Serialization ===")
  val keyString = key.toBase64
  println(s"Key as Base64: $keyString")

  keyString.asFernetKey match
    case Right(importedKey) =>
      println("✓ Key imported successfully")
      importedKey.encrypt("Test with imported key") match
        case Right(t) => println(s"Token generated: ${t.take(20)}...")
        case Left(e) => println(s"Error: $e")
    case Left(error) =>
      println(s"✗ Import error: $error")

  // Example 4: TTL (Time-To-Live)
  println("\n=== Example 4: TTL (60 seconds) ===")
  val key4 = Fernet.generateKey()

  for
    token <- key4.encrypt("Temporary data")
    plain <- key4.decrypt(token, Some(60))
  yield println(s"✓ Token valid: $plain")

  // Example 5: Binary data
  println("\n=== Example 5: Binary Data ===")
  val binaryData = Array[Byte](1, 2, 3, 4, 5)

  for
    token <- Fernet.encryptBytes(binaryData, key)
    bytes <- Fernet.decryptBytes(token, key)
  yield println(s"✓ Bytes recovered: ${bytes.mkString(", ")}")

  // Example 6: Verify token without decrypting
  println("\n=== Example 6: Verification ===")
  val tokenToVerify = key.encrypt("Verify me!").toOption.get

  Fernet.verify(tokenToVerify, key) match
    case Right(true) => println("✓ Token valid")
    case Left(error) => println(s"✗ Token invalid: $error")
    case _ => println("✗ Verification failed")

  // Example 7: Error handling
  println("\n=== Example 7: Error Handling ===")
  val invalidToken = "invalid-token"

  key.decrypt(invalidToken) match
    case Right(plain) => println(s"Decrypted: $plain")
    case Left(error) => println(s"✗ Expected error: $error")

  // Example 8: Key rotation with MultiFernet
  println("\n=== Example 8: Key Rotation ===")
  val oldKey = Fernet.generateKey()
  val newKey = Fernet.generateKey()

  val oldToken = oldKey.encrypt("Important data").toOption.get

  val multi = new MultiFernet(List(newKey, oldKey))
  multi.decrypt(oldToken) match
    case Right(data) =>
      println(s"✓ Decrypted with MultiFernet: $data")
      multi.rotate(oldToken) match
        case Right(newToken) => println("✓ Token rotated to new key")
        case Left(e) => println(s"Error: $e")
    case Left(error) => println(s"Error: $error")

  // Example 9: Result API (Java-friendly)
  println("\n=== Example 9: Result API ===")
  val encResult: Result[String] = Fernet.encryptResult("Data", key)
  if encResult.isSuccess then
    println(s"✓ Encrypted: ${encResult.get.take(20)}...")

    val finalResult = encResult
      .flatMap(token => Fernet.decryptResult(token, key))
      .map(_.toUpperCase)

    println(s"✓ Final: ${finalResult.getOrElse("error")}")
