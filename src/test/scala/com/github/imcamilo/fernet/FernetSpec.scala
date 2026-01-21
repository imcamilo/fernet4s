package com.github.imcamilo.fernet

import com.github.imcamilo.fernet.Fernet.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FernetSpec extends AnyWordSpec with Matchers {

  "Fernet object" when {

    "generating keys" should {

      "create a valid key" in {
        val key = Fernet.generateKey()
        key should not be null
        key.signingKey.length shouldBe 16
        key.encryptionKey.length shouldBe 16
      }

      "create different keys on each generation" in {
        val key1 = Fernet.generateKey()
        val key2 = Fernet.generateKey()

        val key1String = Fernet.keyToString(key1)
        val key2String = Fernet.keyToString(key2)

        key1String should not equal key2String
      }
    }

    "serializing keys" should {

      "convert key to Base64 string" in {
        val key = Fernet.generateKey()
        val keyString = Fernet.keyToString(key)

        keyString should not be empty
        keyString.length should be > 0
      }

      "import key from Base64 string" in {
        val key = Fernet.generateKey()
        val keyString = Fernet.keyToString(key)

        val imported = Fernet.keyFromString(keyString)

        imported shouldBe a[Right[_, _]]
        imported.map(k => Fernet.keyToString(k)) shouldEqual Right(keyString)
      }

      "fail to import invalid key string" in {
        val result = Fernet.keyFromString("invalid-key")
        result shouldBe a[Left[_, _]]
      }
    }

    "encrypting and decrypting" should {

      "encrypt plaintext successfully" in {
        val key = Fernet.generateKey()
        val plaintext = "Hello, Fernet!"

        val result = Fernet.encrypt(plaintext, key)

        result shouldBe a[Right[_, _]]
        result.map(_.length should be > 0)
      }

      "decrypt ciphertext successfully" in {
        val key = Fernet.generateKey()
        val plaintext = "Secret message"

        val result = for {
          token <- Fernet.encrypt(plaintext, key)
          decrypted <- Fernet.decrypt(token, key)
        } yield decrypted

        result shouldEqual Right(plaintext)
      }

      "fail to decrypt with wrong key" in {
        val key1 = Fernet.generateKey()
        val key2 = Fernet.generateKey()
        val plaintext = "Secret"

        val result = for {
          token <- Fernet.encrypt(plaintext, key1)
          decrypted <- Fernet.decrypt(token, key2)
        } yield decrypted

        result shouldBe a[Left[_, _]]
      }

      "encrypt and decrypt binary data" in {
        val key = Fernet.generateKey()
        val binaryData = Array[Byte](1, 2, 3, 4, 5)

        val result = for {
          token <- Fernet.encryptBytes(binaryData, key)
          decrypted <- Fernet.decryptBytes(token, key)
        } yield decrypted

        result shouldBe a[Right[_, _]]
        result.map(_.toList) shouldEqual Right(binaryData.toList)
      }
    }

    "verifying tokens" should {

      "verify valid token" in {
        val key = Fernet.generateKey()
        val plaintext = "Data to verify"

        val result = for {
          token <- Fernet.encrypt(plaintext, key)
          isValid <- Fernet.verify(token, key)
        } yield isValid

        result shouldEqual Right(true)
      }

      "fail to verify invalid token" in {
        val key = Fernet.generateKey()
        val result = Fernet.verify("invalid-token", key)

        result shouldBe a[Left[_, _]]
      }
    }

    "using syntax extensions" should {

      "encrypt using key syntax" in {
        val key = Fernet.generateKey()
        val plaintext = "Hello"

        val result = key.encrypt(plaintext)

        result shouldBe a[Right[_, _]]
      }

      "decrypt using key syntax" in {
        val key = Fernet.generateKey()
        val plaintext = "Secret"

        val result = for {
          token <- key.encrypt(plaintext)
          decrypted <- key.decrypt(token)
        } yield decrypted

        result shouldEqual Right(plaintext)
      }

      "convert key to Base64 using syntax" in {
        val key = Fernet.generateKey()
        val keyString = key.toBase64

        keyString should not be empty
      }

      "import key using string syntax" in {
        val key = Fernet.generateKey()
        val keyString = key.toBase64

        val imported = keyString.asFernetKey

        imported shouldBe a[Right[_, _]]
      }

      "encrypt and decrypt bytes using syntax" in {
        val key = Fernet.generateKey()
        val data = Array[Byte](10, 20, 30)

        val result = for {
          token <- key.encryptBytes(data)
          decrypted <- key.decryptBytes(token)
        } yield decrypted

        result shouldBe a[Right[_, _]]
        result.map(_.toList) shouldEqual Right(data.toList)
      }

      "verify token using syntax" in {
        val key = Fernet.generateKey()

        val result = for {
          token <- key.encrypt("Data")
          isValid <- key.verify(token)
        } yield isValid

        result shouldEqual Right(true)
      }
    }

    "handling TTL" should {

      "decrypt with valid TTL" in {
        val key = Fernet.generateKey()
        val plaintext = "Temporary data"

        val result = for {
          token <- Fernet.encrypt(plaintext, key)
          decrypted <- Fernet.decrypt(token, key, ttlSeconds = Some(60))
        } yield decrypted

        result shouldEqual Right(plaintext)
      }

      "decrypt bytes with TTL" in {
        val key = Fernet.generateKey()
        val data = Array[Byte](1, 2, 3)

        val result = for {
          token <- Fernet.encryptBytes(data, key)
          decrypted <- Fernet.decryptBytes(token, key, ttlSeconds = Some(60))
        } yield decrypted

        result shouldBe a[Right[_, _]]
        result.map(_.toList) shouldEqual Right(data.toList)
      }
    }

    "complete workflow" should {

      "encrypt, export key, import key, and decrypt" in {
        val key = Fernet.generateKey()
        val keyString = Fernet.keyToString(key)
        val plaintext = "Complete workflow test"

        val result = for {
          token <- Fernet.encrypt(plaintext, key)
          importedKey <- Fernet.keyFromString(keyString)
          decrypted <- Fernet.decrypt(token, importedKey)
        } yield decrypted

        result shouldEqual Right(plaintext)
      }

      "work with syntax extensions throughout" in {
        val key = Fernet.generateKey()
        val keyString = key.toBase64
        val plaintext = "Syntax workflow"

        val result = for {
          token <- key.encrypt(plaintext)
          importedKey <- keyString.asFernetKey
          decrypted <- importedKey.decrypt(token)
        } yield decrypted

        result shouldEqual Right(plaintext)
      }
    }
  }
}
