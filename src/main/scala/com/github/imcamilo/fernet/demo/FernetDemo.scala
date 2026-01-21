package com.github.imcamilo.fernet.demo

import com.github.imcamilo.fernet.Fernet
import com.github.imcamilo.fernet.Fernet.syntax._

import scala.io.StdIn

/** Interactive demo application for Fernet4s */
object FernetDemo extends App {

  println("=" * 60)
  println("Welcome to Fernet4s Demo!")
  println("=" * 60)
  println()

  def demo1_BasicEncryption(): Unit = {
    println("📝 Demo 1: Basic Encryption/Decryption")
    println("-" * 60)

    val key = Fernet.generateKey()
    println(s"✓ Generated new key")

    print("Enter text to encrypt: ")
    val input = StdIn.readLine()

    val encrypted = key.encrypt(input)
    encrypted match {
      case Right(token) =>
        println(s"\n✓ Encrypted token:")
        println(s"  $token")
        println(s"  (${token.length} characters)")

        val decrypted = key.decrypt(token)
        decrypted match {
          case Right(plain) =>
            println(s"\n✓ Decrypted text:")
            println(s"  $plain")
          case Left(error) =>
            println(s"\n✗ Decryption error: $error")
        }
      case Left(error) =>
        println(s"\n✗ Encryption error: $error")
    }
    println()
  }

  def demo2_KeyPersistence(): Unit = {
    println("🔑 Demo 2: Key Persistence (Save & Load)")
    println("-" * 60)

    val key = Fernet.generateKey()
    val keyString = key.toBase64

    println(s"✓ Generated key: $keyString")
    println(s"  You can save this key to:")
    println(s"  - Environment variable: export FERNET_KEY=\"$keyString\"")
    println(s"  - Config file: fernet.key=$keyString")
    println(s"  - Secret manager: AWS Secrets Manager, etc.")

    print("\nEnter data to encrypt: ")
    val data = StdIn.readLine()

    val token = key.encrypt(data).toOption.get
    println(s"\n✓ Encrypted token: $token")

    println(s"\n[Simulating app restart...]")

    // Simulate loading key from storage
    val loadedKey = keyString.asFernetKey
    loadedKey match {
      case Right(k) =>
        println(s"✓ Key loaded from storage")
        val decrypted = k.decrypt(token)
        println(s"✓ Decrypted: ${decrypted.toOption.get}")
      case Left(error) =>
        println(s"✗ Key load error: $error")
    }
    println()
  }

  def demo3_TTLTokens(): Unit = {
    println("⏰ Demo 3: Time-Limited Tokens (TTL)")
    println("-" * 60)

    val key = Fernet.generateKey()

    print("Enter temporary message: ")
    val message = StdIn.readLine()

    print("Enter TTL in seconds (e.g., 5): ")
    val ttl = StdIn.readLine().toLong

    val token = key.encrypt(message).toOption.get
    println(s"\n✓ Created token valid for $ttl seconds")
    println(s"  Token: ${token.take(40)}...")

    println(s"\n[Waiting 2 seconds...]")
    Thread.sleep(2000)

    val validCheck = key.decrypt(token, ttlSeconds = Some(ttl))
    validCheck match {
      case Right(data) =>
        println(s"✓ Token still valid: $data")
      case Left(error) =>
        println(s"✗ Token validation: $error")
    }

    if (ttl > 2) {
      print(s"\nPress ENTER to wait ${ttl - 1} more seconds and test expiration...")
      StdIn.readLine()

      Thread.sleep((ttl - 1) * 1000)

      val expiredCheck = key.decrypt(token, ttlSeconds = Some(ttl))
      expiredCheck match {
        case Right(data) =>
          println(s"✓ Token valid: $data")
        case Left(error) =>
          println(s"✗ Token expired: $error")
      }
    }
    println()
  }

  def demo4_APIKeys(): Unit = {
    println("🔐 Demo 4: API Key Generation")
    println("-" * 60)

    val masterKey = Fernet.generateKey()
    println(s"✓ Master key created (store this securely!)")

    print("Enter client ID: ")
    val clientId = StdIn.readLine()

    print("Enter scopes (comma-separated, e.g., read,write): ")
    val scopes = StdIn.readLine()

    val apiKeyData = s"client:$clientId|scopes:$scopes|created:${System.currentTimeMillis()}"

    val apiKey = masterKey.encrypt(apiKeyData).toOption.get
    println(s"\n✓ Generated API Key:")
    println(s"  $apiKey")
    println(s"\n  This key can be given to the client.")
    println(s"  It contains encrypted authorization data.")

    println(s"\n[Validating API key...]")
    val validation = masterKey.decrypt(apiKey)
    validation match {
      case Right(data) =>
        println(s"✓ API key valid!")
        println(s"  Decoded data: $data")
      case Left(error) =>
        println(s"✗ Validation error: $error")
    }
    println()
  }

  def demo5_SecureMessages(): Unit = {
    println("💬 Demo 5: Secure Message Exchange")
    println("-" * 60)

    val sharedKey = Fernet.generateKey()
    val keyString = sharedKey.toBase64

    println(s"✓ Shared secret key: ${keyString.take(30)}...")
    println(s"  (Both services must have this key)")

    print("\nService A - Enter message to send: ")
    val message = StdIn.readLine()

    val encrypted = sharedKey.encrypt(message).toOption.get
    println(s"\n✓ Service A encrypted message:")
    println(s"  $encrypted")

    println(s"\n[Message sent over network...]")
    println(s"\n✓ Service B received message")
    println(s"  Decrypting with shared key...")

    val decrypted = sharedKey.decrypt(encrypted)
    decrypted match {
      case Right(plain) =>
        println(s"✓ Service B decrypted message:")
        println(s"  $plain")
      case Left(error) =>
        println(s"✗ Decryption failed: $error")
    }
    println()
  }

  def demo6_BinaryData(): Unit = {
    println("📦 Demo 6: Binary Data Encryption")
    println("-" * 60)

    val key = Fernet.generateKey()

    print("Enter text to treat as binary: ")
    val text = StdIn.readLine()
    val bytes = text.getBytes

    println(s"\n✓ Original bytes (${bytes.length}): ${bytes.take(20).mkString("[", ", ", "...]")}")

    val encrypted = key.encryptBytes(bytes).toOption.get
    println(s"\n✓ Encrypted token: ${encrypted.take(50)}...")

    val decrypted = key.decryptBytes(encrypted)
    decrypted match {
      case Right(data) =>
        println(s"\n✓ Decrypted bytes: ${data.take(20).mkString("[", ", ", "...]")}")
        println(s"✓ Converted back: ${new String(data)}")
      case Left(error) =>
        println(s"\n✗ Decryption error: $error")
    }
    println()
  }

  def demo7_KeyRotation(): Unit = {
    println("🔄 Demo 7: Key Rotation")
    println("-" * 60)

    val oldKey = Fernet.generateKey()
    val newKey = Fernet.generateKey()

    println(s"✓ Old key: ${oldKey.toBase64.take(30)}...")
    println(s"✓ New key: ${newKey.toBase64.take(30)}...")

    print("\nEnter data encrypted with old key: ")
    val data = StdIn.readLine()

    val oldToken = oldKey.encrypt(data).toOption.get
    println(s"\n✓ Old token: ${oldToken.take(50)}...")

    println(s"\n[Rotating to new key...]")

    val rotated = for {
      decrypted <- oldKey.decrypt(oldToken)
      newToken <- newKey.encrypt(decrypted)
    } yield newToken

    rotated match {
      case Right(newToken) =>
        println(s"✓ New token: ${newToken.take(50)}...")
        println(s"\n[Verifying...]")

        val verified = newKey.decrypt(newToken)
        println(s"✓ Decrypted with new key: ${verified.toOption.get}")

        val oldKeyFail = oldKey.decrypt(newToken)
        if (oldKeyFail.isLeft) {
          println(s"✓ Old key can't decrypt new token (as expected)")
        }
      case Left(error) =>
        println(s"✗ Rotation error: $error")
    }
    println()
  }

  // Main menu
  def showMenu(): Unit = {
    println("\n" + "=" * 60)
    println("Choose a demo:")
    println("=" * 60)
    println("1. Basic Encryption/Decryption")
    println("2. Key Persistence (Save & Load)")
    println("3. Time-Limited Tokens (TTL)")
    println("4. API Key Generation")
    println("5. Secure Message Exchange")
    println("6. Binary Data Encryption")
    println("7. Key Rotation")
    println("0. Exit")
    println("-" * 60)
    print("Enter choice: ")
  }

  // Run interactive demo
  var running = true
  while (running) {
    showMenu()
    val choice = StdIn.readLine().trim

    println()
    choice match {
      case "1" => demo1_BasicEncryption()
      case "2" => demo2_KeyPersistence()
      case "3" => demo3_TTLTokens()
      case "4" => demo4_APIKeys()
      case "5" => demo5_SecureMessages()
      case "6" => demo6_BinaryData()
      case "7" => demo7_KeyRotation()
      case "0" =>
        println("Thanks for trying Fernet4s! 👋")
        running = false
      case _ =>
        println("Invalid choice. Please try again.")
    }
  }
}
