package com.github.imcamilo.fernet

import com.github.imcamilo.checkers.{Checker, DefaultChecker, StringChecker}
import org.scalatest.wordspec.AnyWordSpec

import java.security.SecureRandom
import java.time.Duration
import java.time.temporal.TemporalAmount

class UsagesSpec extends AnyWordSpec {

  val secretString = "hackable_key_for_bad_people"

  "the key generation" should {
    val key1: Key = Key.generateKey()
    val key2: Key = Key.generateKey(new SecureRandom())
    val key3: Option[Key] = Key.deserialize("wz5hami-yvr3zHyzVEiOYFvN9kTzXRW3dP7NcUr9Nvs=")
    "be ok and the works with the tokens" in {
      val token1: Token = Token.generate(key1, secretString)
      val token2: Token = Token.generate(key2, secretString)
      val token3: Token = Token.generate(key3.get, secretString)
      val token4: Token = Token.generate(new SecureRandom(), key1, secretString)
      val token5: Token = Token.generate(new SecureRandom(), key2, secretString)
      val token6: Token = Token.generate(new SecureRandom(), key3.get, secretString)
      assert(token1 != null)
      assert(token2 != null)
      assert(token3 != null)
      assert(token4 != null)
      assert(token5 != null)
      assert(token6 != null)
    }
  }

  "the key generation" should {
    "fail because the string is old" in {
      val simpleChecker: Checker[String] = new StringChecker {
        override def timeToLive: TemporalAmount = Duration.ofHours(1)
      }
      val result = for {
        key <- Key.deserialize("wz5hami-yvr3zHyzVEiOYFvN9kTzXRW3dP7NcUr9Nvs=")
        token <- Token.fromString(
          "gAAAAABhDDN4i36Z-MGugoIpfN6Xij5pWesWOFY0Jj-Gv3rK46uWMo1y3UuhqknT-bUIS5n0zyBtZq05UNR0j88x91FyXBFMDFz_nR1zFmpUeM6X3-OiFb0="
        )
        fKey <- token.validateAndDecrypt(key, simpleChecker)
      } yield fKey
      assert(result.isEmpty)
    }
  }

}
