package com.github.imcamilo.fernet

import com.github.imcamilo.validators.StandardValidator
import org.scalatest.wordspec.AnyWordSpec

class TokenSpec extends AnyWordSpec {

  import TokenSpec._

  "when decrypting secret key and a generated token the lib " should {

    def key: Option[Key] = Key(DecrEncryptedKey)
    def token: Option[Token] = Token(key.get, Original)

    "get secret key is invoked and result and should be equal than expected" in {
      val secretKey =
        for {
          key <- Key(DecrEncryptedKey)
          serialized <- Token.serialize(token.get).toOption
          token <- Token.fromString(serialized)
          fKey <- token.validateAndDecrypt(key, StandardValidator.validator)
        } yield fKey

      secretKey match {
        case Some(value) => assert(value == DecrExpected)
        case None        =>
      }
    }

    "get secret key is invoked and result should be equal than second expected" in {
      val secretKey =
        for {
          key <- Key(HackEncryptedKey)
          serialized <- Token.serialize(token.get).toOption
          token <- Token.fromString(serialized)
          fKey <- token.validateAndDecrypt(key, StandardValidator.validator)
        } yield fKey
      secretKey match {
        case Some(value) => assert(value == HackExpected)
        case None        =>
      }
    }

    "fail to generate a token with an invalid key" in {
      val invalidKey = "invalid-key"
      assertThrows[ArrayIndexOutOfBoundsException] {
        Token.apply(Key(invalidKey).get, Original)
      }
    }

  }

}

object TokenSpec {
  val Original = "this-should-be-desencrypted"

  val DecrEncryptedKey = "wz5hami-yvr3zHyzVEiOYFvN9kTzXRW3dP7NcUr9Nvs="
  val DecrExpected = "this-should-be-desencrypted"

  val HackEncryptedKey =
    "gAAAAABhDDN4i36Z-MGugoIpfN6Xij5pWesWOFY0Jj-Gv3rK46uWMo1y3UuhqknT-bUIS5n0zyBtZq05UNR0j88x91FyXBFMDFz_nR1zFmpUeM6X3-OiFb0="
  val HackExpected = "hackable_key_for_bad_people"
}
