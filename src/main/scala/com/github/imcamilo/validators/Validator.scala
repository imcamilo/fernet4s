package com.github.imcamilo.validators

import com.github.imcamilo.exceptions.OutputValidationException
import com.github.imcamilo.fernet.{Key, Token}

import java.nio.charset.{Charset, StandardCharsets}
import java.time.temporal.TemporalAmount
import java.time.{Clock, Duration, Instant}
import java.util.function.Predicate
import scala.util.{Failure, Success, Try}

trait Validator[A] {

  // Variables de tiempo por defecto
  private val defaultDuration = Duration.ofSeconds(60)

  def getClock: Clock = Clock.systemUTC() // Uso de la zona horaria UTC

  def getTimeToLive: TemporalAmount = defaultDuration

  def getMaxClockSkew: TemporalAmount = defaultDuration

  def getObjectValidator: Predicate[A] = (_: A) => true

  def getTransformer: Array[Byte] => A

  /** Check the validity of the token then decrypt and deserialize the payload.
    *
    * @param key
    *   the stored shared secret key
    * @param token
    *   the client-provided token of unknown validity
    * @return
    *   the deserialized contents of the token
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

      // Valida el objeto transformado
      if (getObjectValidator.test(finalResponseObject)) {
        finalResponseObject
      } else {
        // Limpieza de datos sensibles
        java.util.Arrays.fill(plainText, 0.toByte)
        throw new OutputValidationException("Invalid Fernet token payload.")
      }
    } match {
      // Manejo de excepción general para evitar errores no controlados
      case Success(result)                        => Success(result)
      case Failure(ex: OutputValidationException) => Failure(ex)
      case Failure(ex) =>
        Failure(
          new OutputValidationException(
            s"Error during validation: ${ex.getMessage}",
            ex
          )
        )
    }

}

trait StringValidator extends Validator[String] {

  def getCharset: Charset = StandardCharsets.UTF_8

  val getTransformer: Array[Byte] => String = (bytes: Array[Byte]) => {
    val result = new String(bytes, getCharset)
    // Limpieza de datos sensibles
    java.util.Arrays.fill(bytes, 0.toByte)
    result
  }

}

object StandardValidator {

  val validator: Validator[String] = new StringValidator {
    override def getTimeToLive: TemporalAmount = {
      Duration.ofSeconds(Instant.MAX.getEpochSecond) // Tiempo de vida "máximo"
    }
  }

}
