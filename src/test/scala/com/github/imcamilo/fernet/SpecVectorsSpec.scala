package com.github.imcamilo.fernet

import io.circe.parser._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.time.format.DateTimeFormatter
import scala.io.Source
import scala.util.Using

/** Tests using official Fernet specification test vectors from github.com/fernet/spec */
class SpecVectorsSpec extends AnyWordSpec with Matchers {

  private def loadJson(filename: String): String = {
    Using(Source.fromResource(filename))(_.mkString).get
  }

  private def parseTimestamp(iso8601: String): Instant = {
    Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(iso8601))
  }

  "Official Fernet specification vectors" when {

    "verifying tokens (verify.json)" should {

      "pass all verification test vectors" in {
        val json = loadJson("verify.json")
        val vectors = parse(json).getOrElse(fail("Failed to parse verify.json"))

        vectors.asArray.get.foreach { vector =>
          val token = vector.hcursor.get[String]("token").toOption.get
          val secret = vector.hcursor.get[String]("secret").toOption.get
          val expectedSrc = vector.hcursor.get[String]("src").toOption.get
          val ttlSec = vector.hcursor.get[Int]("ttl_sec").toOption.get
          val now = vector.hcursor.get[String]("now").toOption.get

          val key = Fernet.keyFromString(secret).toOption.get

          // Verify the token decrypts to expected plaintext
          val tokenObj = Token.fromString(token).get
          val decrypted = new String(
            tokenObj.validateAndDecrypt(
              key,
              parseTimestamp(now).minusSeconds(ttlSec),
              parseTimestamp(now).plusSeconds(60) // max clock skew
            ),
            Constants.charset
          )

          decrypted shouldEqual expectedSrc
        }
      }
    }

    "rejecting invalid tokens (invalid.json)" should {

      "reject all invalid test vectors" in {
        val json = loadJson("invalid.json")
        val vectors = parse(json).getOrElse(fail("Failed to parse invalid.json"))

        vectors.asArray.get.foreach { vector =>
          val desc = vector.hcursor.get[String]("desc").toOption.get
          val token = vector.hcursor.get[String]("token").toOption.get
          val secret = vector.hcursor.get[String]("secret").toOption.get
          val ttlSec = vector.hcursor.get[Int]("ttl_sec").toOption.get
          val now = vector.hcursor.get[String]("now").toOption.get

          withClue(s"Test case: $desc - ") {
            val result = for {
              key <- Fernet.keyFromString(secret)
              tokenObj <- Token.fromString(token).toRight("Invalid token")
              decrypted <- scala.util.Try {
                tokenObj.validateAndDecrypt(
                  key,
                  parseTimestamp(now).minusSeconds(ttlSec),
                  parseTimestamp(now).plusSeconds(60)
                )
              }.toOption.toRight("Validation failed")
            } yield decrypted

            result shouldBe a[Left[_, _]]
          }
        }
      }
    }
  }
}
