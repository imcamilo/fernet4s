package com.github.imcamilo.validators

import com.github.imcamilo.exceptions.OutputValidationException
import com.github.imcamilo.fernet.{Key, Token}

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.time.temporal.TemporalAmount
import java.time.{Clock, Duration, Instant, ZoneOffset}
import java.util.function.Predicate
import scala.util.Try;

trait Validator[A] {

  private val sixtySecs = Duration.ofSeconds(60)

  def getClock: Clock = Clock.tickSeconds(ZoneOffset.UTC)

  def getTimeToLive: TemporalAmount = sixtySecs

  def getMaxClockSkew: TemporalAmount = sixtySecs

  def getObjectValidator: Predicate[A] = (payload: A) => true

  def getTransformer: Array[Byte] => A

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
      val now = Instant.now(getClock)
      val plainText = token.validateAndDecrypt(
        key,
        now.minus(getTimeToLive),
        now.plus(getMaxClockSkew)
      )
      val finalResponseObject = getTransformer(plainText)
      val isObjectValid = getObjectValidator.test(finalResponseObject)
      val response =
        if (isObjectValid) finalResponseObject
        else {
          val cleanedPlainText = plainText.clone().map(_ => 0)
          throw new OutputValidationException("Invalid Fernet token payload.")
        }
      response
    }

}

trait StringValidator extends Validator[String] {

  def getCharset: Charset = UTF_8

  val getTransformer: Array[Byte] => String = (bytes: Array[Byte]) => {
    val retval = new String(bytes, getCharset)
    val cleanedBytes = bytes.clone().map(_ => 0)
    retval
  }

}

object StandardValidator {

  val validator: Validator[String] = new StringValidator {
    override def getTimeToLive: TemporalAmount = {
      Duration.ofSeconds(Instant.MAX.getEpochSecond)
    }
  }

}
