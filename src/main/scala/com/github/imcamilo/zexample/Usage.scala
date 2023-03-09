package com.github.imcamilo.zexample

import com.github.imcamilo.checkers.{Checker, StringChecker}
import com.github.imcamilo.fernet.{Key, Token}

import java.security.SecureRandom
import java.time.Duration
import java.time.temporal.TemporalAmount

object Usage extends App {

  // generating a new key
  val key1: Key = Key.generateKey()
  val key2: Key = Key.generateKey(new SecureRandom())
  // deserializing an existing key
  val key3: Option[Key] = Key.deserialize("cw_0x689RpI-jtRR7oE8h_eQsKImvJapLeSbXpwF4e4=")

  // create a token
  val token1: Token = Token.generate(key1, "secret message")
  val token2: Token = Token.generate(key2, "secret message")
  val token3: Token = Token.generate(key3.get, "secret message")
  // or
  val token4: Token = Token.generate(new SecureRandom(), key1, "secret message")
  val token5: Token = Token.generate(new SecureRandom(), key2, "secret message")
  val token6: Token = Token.generate(new SecureRandom(), key3.get, "secret message")

  // deserializing an existing token
  val token7: Option[Token] = Token.fromString(
    "gAAAAAAdwJ6wAAECAwQFBgcICQoLDA0ODy021cpGVWKZ_eEwCGM4BLLF_5CV9dOPmrhuVUPgJobwOz7JcbmrR64jVmpU4IwqDA=="
  )

  // validating the token
  val simpleChecker: Checker[String] = new StringChecker {
    override def timeToLive: TemporalAmount = Duration.ofSeconds(60)
  }

  // val result = Token.validateAndDecrypt(key, DefaultChecker.simpleChecker)
  val result: Option[String] = token1.validateAndDecrypt(key1, simpleChecker)

}