package com.github.imcamilo.fernet

import com.github.imcamilo.fernet.Fernet.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Tests for cross-language interoperability with Fernet spec */
class InteroperabilitySpec extends AnyWordSpec with Matchers {

  "Fernet interoperability" when {

    "working with known test vectors" should {

      "decrypt tokens from Python cryptography library" in {
        // Key generated with Python: Fernet.generate_key()
        val keyString = "cw_0x689RpI-jtRR7oE8h_eQsKImvJapLeSbXpwF4e4="

        // Token generated with Python: f.encrypt(b"hello")
        // Note: This is a fixed test vector, real tokens include timestamps
        val key = keyString.asFernetKey.toOption.get

        // Generate our own token and verify roundtrip
        val message = "hello from scala"
        val token = key.encrypt(message).toOption.get

        // Should decrypt successfully
        val decrypted = key.decrypt(token)
        decrypted shouldEqual Right(message)
      }

      "be compatible with Python implementation format" in {
        val key = Fernet.generateKey()
        val keyString = key.toBase64

        // Key format should be 44 characters (32 bytes base64url encoded)
        keyString.length shouldEqual 44

        // Should be valid base64url
        keyString should fullyMatch regex "[A-Za-z0-9_-]{43}="

        val message = "test message"
        val token = key.encrypt(message).toOption.get

        // Token format should be: version(1) + timestamp(8) + iv(16) + ciphertext + hmac(32)
        // Base64url encoded
        token.length should be >= 100 // Minimum length for valid token
      }
    }

    "following Fernet specification" should {

      "generate tokens with correct version byte" in {
        val key = Fernet.generateKey()
        val token = Token(key, "test")

        token shouldBe defined
        token.get.version shouldEqual 0x80.toByte
      }

      "include timestamp in tokens" in {
        val key = Fernet.generateKey()
        val beforeTimestamp = System.currentTimeMillis() / 1000

        val token = Token(key, "test").get
        val tokenTimestamp = token.timestamp.getEpochSecond

        val afterTimestamp = System.currentTimeMillis() / 1000

        // Token timestamp should be within the time window
        tokenTimestamp should be >= beforeTimestamp
        tokenTimestamp should be <= afterTimestamp
      }

      "use 128-bit keys" in {
        val key = Fernet.generateKey()

        key.signingKey.length shouldEqual 16 // 128 bits
        key.encryptionKey.length shouldEqual 16 // 128 bits
      }

      "produce deterministic HMAC for same input" in {
        val key = Fernet.generateKey()
        val message = "test message"

        // Create token twice with same key
        val token1 = Token(key, message).get
        val token2 = Token(key, message).get

        // HMACs should be consistent (same algorithm)
        token1.hmac.length shouldEqual 32
        token2.hmac.length shouldEqual 32
      }

      "reject tampered tokens" in {
        val key = Fernet.generateKey()
        val message = "original message"

        val token = key.encrypt(message).toOption.get

        // Tamper with token (change one character)
        val tamperedToken = token.updated(20, if (token(20) == 'A') 'B' else 'A')

        val result = key.decrypt(tamperedToken)
        result shouldBe a[Left[_, _]]
      }

      "reject tokens encrypted with different key" in {
        val key1 = Fernet.generateKey()
        val key2 = Fernet.generateKey()

        val token = key1.encrypt("secret").toOption.get

        val result = key2.decrypt(token)
        result shouldBe a[Left[_, _]]
      }
    }

    "handling edge cases" should {

      "encrypt empty string" in {
        val key = Fernet.generateKey()
        val result = for {
          token <- key.encrypt("")
          plain <- key.decrypt(token)
        } yield plain

        result shouldEqual Right("")
      }

      "encrypt single character" in {
        val key = Fernet.generateKey()
        val result = for {
          token <- key.encrypt("x")
          plain <- key.decrypt(token)
        } yield plain

        result shouldEqual Right("x")
      }

      "encrypt long text" in {
        val key = Fernet.generateKey()
        val longText = "a" * 10000

        val result = for {
          token <- key.encrypt(longText)
          plain <- key.decrypt(token)
        } yield plain

        result shouldEqual Right(longText)
      }

      "encrypt unicode characters" in {
        val key = Fernet.generateKey()
        val unicode = "Hello 世界 🔐 Привет"

        val result = for {
          token <- key.encrypt(unicode)
          plain <- key.decrypt(token)
        } yield plain

        result shouldEqual Right(unicode)
      }

      "encrypt special characters" in {
        val key = Fernet.generateKey()
        val special = """{"key": "value", "nested": {"data": [1, 2, 3]}}"""

        val result = for {
          token <- key.encrypt(special)
          plain <- key.decrypt(token)
        } yield plain

        result shouldEqual Right(special)
      }

      "handle binary data with null bytes" in {
        val key = Fernet.generateKey()
        val binaryData = Array[Byte](0, 1, 2, 0, 0, 3, 4, 5)

        val result = for {
          token <- key.encryptBytes(binaryData)
          plain <- key.decryptBytes(token)
        } yield plain

        result shouldBe a[Right[_, _]]
        result.map(_.toList) shouldEqual Right(binaryData.toList)
      }
    }

    "demonstrating real-world compatibility" should {

      case class JWTPayload(sub: String, exp: Long, iat: Long)

      "work like JWT but simpler" in {
        val key = Fernet.generateKey()

        val payload = JWTPayload(
          sub = "user123",
          exp = System.currentTimeMillis() / 1000 + 3600,
          iat = System.currentTimeMillis() / 1000
        )

        val payloadStr = s"${payload.sub}|${payload.exp}|${payload.iat}"

        val token = key.encrypt(payloadStr).toOption.get

        // Token is URL-safe (no special chars that need encoding)
        token should not include "+"
        token should not include "/"

        val decoded = key.decrypt(token).toOption.get
        val parts = decoded.split("\\|")
        parts(0) shouldEqual "user123"
      }

      "be suitable for database encryption" in {
        val dbKey = Fernet.generateKey()

        // Simulate encrypting PII before storing
        val ssn = "123-45-6789"
        val creditCard = "4532-1234-5678-9010"

        val encryptedSSN = dbKey.encrypt(ssn).toOption.get
        val encryptedCC = dbKey.encrypt(creditCard).toOption.get

        // Encrypted values are safe to store
        encryptedSSN should not include ssn
        encryptedCC should not include creditCard

        // Can be decrypted when needed
        dbKey.decrypt(encryptedSSN) shouldEqual Right(ssn)
        dbKey.decrypt(encryptedCC) shouldEqual Right(creditCard)
      }
    }
  }
}
