package com.github.imcamilo.fernet

import com.github.imcamilo.fernet.Constants.{
  encryptionKeyBytes,
  signingKeyBytes
}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Failure

class KeySpec extends AnyWordSpec with Matchers {

  import Key._

  "Key companion" when {

    "creating key instance" should {

      "return a valid key instance for valid keys" in {
        val signingKey = Array.fill(signingKeyBytes)(0.toByte)
        val encryptionKey = Array.fill(encryptionKeyBytes)(1.toByte)
        val generatedKey = creatingKeyInstance(signingKey, encryptionKey)
        generatedKey.get._1 should contain theSameElementsAs signingKey
        generatedKey.get._2 should contain theSameElementsAs encryptionKey
      }

      "throw an IllegalArgumentException for null signing key" in {
        val encryptionKey = Array.fill(encryptionKeyBytes)(1.toByte)
        val result = creatingKeyInstance(null, encryptionKey)
        result shouldBe a[Failure[_]]
        result.failed.get shouldBe a[IllegalArgumentException]
        result.failed.get.getMessage shouldBe "Signing key must be 128 bits"
      }

      "throw an IllegalArgumentException for null encryption key" in {
        val signingKey = Array.fill(signingKeyBytes)(0.toByte)

        val result = creatingKeyInstance(signingKey, null)

        result shouldBe a[Failure[_]]
        result.failed.get shouldBe a[IllegalArgumentException]
        result.failed.get.getMessage shouldBe "Encryption key must be 128 bits"
      }

      "throw an IllegalArgumentException for an invalid signing key length" in {
        val invalidSigningKey = Array.fill(signingKeyBytes - 1)(0.toByte)

        val result = creatingKeyInstance(
          invalidSigningKey,
          Array.fill(encryptionKeyBytes)(1.toByte)
        )

        result shouldBe a[Failure[_]]
        result.failed.get shouldBe a[IllegalArgumentException]
        result.failed.get.getMessage shouldBe "Signing key must be 128 bits"
      }

      "throw an IllegalArgumentException for an invalid encryption key length" in {
        val invalidEncryptionKey = Array.fill(encryptionKeyBytes - 1)(1.toByte)

        val result = creatingKeyInstance(
          Array.fill(signingKeyBytes)(0.toByte),
          invalidEncryptionKey
        )

        result shouldBe a[Failure[_]]
        result.failed.get shouldBe a[IllegalArgumentException]
        result.failed.get.getMessage shouldBe "Encryption key must be 128 bits"
      }

      "throw an IllegalArgumentException for an invalid signing key and encryption key length" in {
        val invalidSigningKey = Array.fill(signingKeyBytes - 1)(0.toByte)
        val invalidEncryptionKey = Array.fill(encryptionKeyBytes - 1)(1.toByte)

        val result =
          creatingKeyInstance(invalidSigningKey, invalidEncryptionKey)

        result shouldBe a[Failure[_]]
        result.failed.get shouldBe a[IllegalArgumentException]
        result.failed.get.getMessage shouldBe "Signing key must be 128 bits"
      }

      "throw an IllegalArgumentException for an invalid signing key and null encryption key" in {
        val invalidSigningKey = Array.fill(signingKeyBytes - 1)(0.toByte)

        val result = creatingKeyInstance(invalidSigningKey, null)

        result shouldBe a[Failure[_]]
        result.failed.get shouldBe a[IllegalArgumentException]
        result.failed.get.getMessage shouldBe "Signing key must be 128 bits"
      }

      "throw an IllegalArgumentException for null signing key and an invalid encryption key" in {
        val invalidEncryptionKey = Array.fill(encryptionKeyBytes - 1)(1.toByte)

        val result = creatingKeyInstance(null, invalidEncryptionKey)

        result shouldBe a[Failure[_]]
        result.failed.get shouldBe a[IllegalArgumentException]
        result.failed.get.getMessage shouldBe "Signing key must be 128 bits"
      }

    }

  }

}
