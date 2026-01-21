package com.github.imcamilo.fernet

import com.github.imcamilo.fernet.MultiFernet.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MultiFernetSpec extends AnyWordSpec with Matchers {

  "MultiFernet" when {

    "created with multiple keys" should {

      "encrypt with the first key" in {
        val key1 = Fernet.generateKey()
        val key2 = Fernet.generateKey()
        val multiFernet = MultiFernet(key1, key2)

        val message = "test message"
        val encrypted = multiFernet.encrypt(message)

        encrypted shouldBe a[Right[_, _]]

        // Should decrypt with first key
        val decrypted = Fernet.decrypt(encrypted.toOption.get, key1)
        decrypted shouldEqual Right(message)
      }

      "decrypt with any available key" in {
        val key1 = Fernet.generateKey()
        val key2 = Fernet.generateKey()
        val key3 = Fernet.generateKey()

        val multiFernet = MultiFernet(key1, key2, key3)

        val message = "encrypted with key2"

        // Encrypt with key2 directly
        val token = Fernet.encrypt(message, key2).toOption.get

        // MultiFernet should decrypt it
        val decrypted = multiFernet.decrypt(token)
        decrypted shouldEqual Right(message)
      }

      "try keys in order until one works" in {
        val key1 = Fernet.generateKey()
        val key2 = Fernet.generateKey()
        val key3 = Fernet.generateKey()

        val multiFernet = MultiFernet(key1, key2, key3)

        // Encrypt with key3
        val token = Fernet.encrypt("data", key3).toOption.get

        // Should find key3 and decrypt
        val result = multiFernet.decrypt(token)
        result shouldEqual Right("data")
      }

      "fail if no key can decrypt" in {
        val key1 = Fernet.generateKey()
        val key2 = Fernet.generateKey()
        val wrongKey = Fernet.generateKey()

        val multiFernet = MultiFernet(key1, key2)

        // Encrypt with a different key
        val token = Fernet.encrypt("secret", wrongKey).toOption.get

        val result = multiFernet.decrypt(token)
        result shouldBe a[Left[_, _]]
      }
    }

    "rotating tokens" should {

      "re-encrypt with primary key" in {
        val oldKey = Fernet.generateKey()
        val newKey = Fernet.generateKey()

        // Start with old key as primary
        val oldMultiFernet = MultiFernet(oldKey)
        val message = "data to rotate"
        val oldToken = oldMultiFernet.encrypt(message).toOption.get

        // Create new MultiFernet with new key as primary, old key as fallback
        val multiFernet = MultiFernet(newKey, oldKey)

        // Rotate the token
        val newToken = multiFernet.rotate(oldToken)
        newToken shouldBe a[Right[_, _]]

        // New token should decrypt with new key
        val decrypted = Fernet.decrypt(newToken.toOption.get, newKey)
        decrypted shouldEqual Right(message)

        // Old key should NOT decrypt new token
        val oldKeyTry = Fernet.decrypt(newToken.toOption.get, oldKey)
        oldKeyTry shouldBe a[Left[_, _]]
      }
    }

    "managing keys" should {

      "add new keys" in {
        val key1 = Fernet.generateKey()
        val key2 = Fernet.generateKey()

        val multiFernet = MultiFernet(key1)
        val updated = multiFernet.addKey(key2)

        updated.keys should have length 2
        updated.keys.head shouldEqual key1
        updated.keys.last shouldEqual key2
      }

      "set new primary key" in {
        val key1 = Fernet.generateKey()
        val key2 = Fernet.generateKey()

        val multiFernet = MultiFernet(key1)
        val updated = multiFernet.setPrimaryKey(key2)

        updated.keys.head shouldEqual key2
        updated.keys should contain(key1)
      }

      "remove a key" in {
        val key1 = Fernet.generateKey()
        val key2 = Fernet.generateKey()
        val key1String = Fernet.keyToString(key1)

        val multiFernet = MultiFernet(key1, key2)
        val removed = multiFernet.removeKey(key1String)

        removed shouldBe a[Right[_, _]]
        removed.map(_.keys should have length 1)
        removed.map(_.keys.head shouldEqual key2)
      }

      "not remove last key" in {
        val key1 = Fernet.generateKey()
        val key1String = Fernet.keyToString(key1)

        val multiFernet = MultiFernet(key1)
        val result = multiFernet.removeKey(key1String)

        result shouldBe a[Left[_, _]]
      }
    }

    "created from strings" should {

      "parse valid key strings" in {
        val key1 = Fernet.generateKey()
        val key2 = Fernet.generateKey()

        val key1String = Fernet.keyToString(key1)
        val key2String = Fernet.keyToString(key2)

        val result = MultiFernet.fromStrings(key1String, key2String)
        result shouldBe a[Right[_, _]]
        result.map(_.keys should have length 2)
      }

      "fail with invalid key strings" in {
        val validKey = Fernet.keyToString(Fernet.generateKey())
        val invalidKey = "invalid-key"

        val result = MultiFernet.fromStrings(validKey, invalidKey)
        result shouldBe a[Left[_, _]]
      }
    }

    "using syntax extensions" should {

      "encrypt and decrypt" in {
        val key1 = Fernet.generateKey()
        val key2 = Fernet.generateKey()
        val multiFernet = MultiFernet(key1, key2)

        val message = "test"

        val result = for {
          token <- multiFernet.encrypt(message)
          plain <- multiFernet.decrypt(token)
        } yield plain

        result shouldEqual Right(message)
      }

      "rotate tokens" in {
        val oldKey = Fernet.generateKey()
        val newKey = Fernet.generateKey()

        val oldToken = Fernet.encrypt("data", oldKey).toOption.get

        val multiFernet = MultiFernet(newKey, oldKey)
        val rotated = multiFernet.rotate(oldToken)

        rotated shouldBe a[Right[_, _]]
      }
    }

    "in real-world scenario" should {

      "support gradual key migration" in {
        // Initial deployment with key1
        val key1 = Fernet.generateKey()
        val tokens = (1 to 10).map { i =>
          Fernet.encrypt(s"user-$i", key1).toOption.get
        }

        // Generate new key for rotation
        val key2 = Fernet.generateKey()

        // Deploy with both keys (new primary, old fallback)
        val multiFernet = MultiFernet(key2, key1)

        // Old tokens still work
        tokens.foreach { token =>
          multiFernet.decrypt(token) shouldBe a[Right[_, _]]
        }

        // New tokens use new key
        val newToken = multiFernet.encrypt("new-user").toOption.get
        Fernet.decrypt(newToken, key2) shouldBe a[Right[_, _]]

        // Rotate old tokens gradually
        val rotatedTokens = tokens.map(multiFernet.rotate(_).toOption.get)

        // All rotated tokens work with new key only
        rotatedTokens.foreach { token =>
          Fernet.decrypt(token, key2) shouldBe a[Right[_, _]]
          Fernet.decrypt(token, key1) shouldBe a[Left[_, _]]
        }

        // Eventually remove old key
        val finalMultiFernet = MultiFernet(key2)
        rotatedTokens.foreach { token =>
          finalMultiFernet.decrypt(token) shouldBe a[Right[_, _]]
        }
      }
    }
  }
}
