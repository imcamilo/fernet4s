package com.github.imcamilo.checkers

import com.github.imcamilo.exceptions.OutputValidationException
import com.github.imcamilo.fernet.{Key, Token}

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.time.temporal.TemporalAmount
import java.time.{Clock, Duration, Instant, ZoneOffset}
import java.util.function.Predicate
import scala.util.Try;

trait Checker[A] {

  def clock: Clock = Clock.tickSeconds(ZoneOffset.UTC)

  def timeToLive: TemporalAmount = Duration.ofMinutes(30)

  def maxClockSkew: TemporalAmount = Duration.ofMinutes(30)

  def objectChecker: Predicate[A] = (_: A) => true

  def transformer: Array[Byte] => A

  /** Check the validity of the token then decrypt and deserialise the payload.
   *  @param key
   *    the stored shared secret key
   *  @param token
   *    the client-provided token of unknown validity
   *  @return
   *    the deserialized contents of the token
   */
  def validateAndDecrypt(key: Key, token: Token): Try[A] =
    Try {
      val now: Instant = Instant.now(clock)
      val plainText: Array[Byte] = token.validateAndDecrypt(
        key,
        now.minus(timeToLive),
        now.plus(maxClockSkew)
      )
      val finalResponseObject = transformer(plainText)
      val isObjectValid = objectChecker.test(finalResponseObject)
      val response =
        if (isObjectValid) finalResponseObject
        else {
          val cleanedPlainText = plainText.clone().map(_ => 0)
          throw new OutputValidationException("Invalid Fernet token payload.")
        }
      response
    }

}

trait StringChecker extends Checker[String] {

  def getCharset: Charset = UTF_8

  val transformer: Array[Byte] => String = (bytes: Array[Byte]) => {
    val retval = new String(bytes, getCharset)
    val cleanedBytes = bytes.clone().map(_ => 0)
    retval
  }

}

object DefaultChecker {

  /** Default validator, with 1 hour of time.
   */
  val timeChecker: Checker[String] = new StringChecker {
    override def timeToLive: TemporalAmount = {
      Duration.ofHours(1)
    }
  }

}
