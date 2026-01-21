package com.github.imcamilo.fernet

/** Functional result type for Java/Kotlin interoperability.
  *
  * This is a simple, functional result type that works well from Java, Kotlin, and Scala.
  * It's similar to Either but with a more Java-friendly API.
  *
  * @tparam A the success value type
  *
  * @example Scala usage:
  * {{{
  * val result: Result[String] = Result.success("data")
  * result.map(_.toUpperCase).getOrElse("default")
  * }}}
  *
  * @example Java usage:
  * {{{
  * Result<String> result = Result.success("data");
  * if (result.isSuccess()) {
  *     String value = result.get();
  * } else {
  *     String error = result.getError();
  * }
  * }}}
  *
  * @since 1.0.0
  */
sealed trait Result[+A] {

  /** Returns true if this is a success result. */
  def isSuccess: Boolean

  /** Returns true if this is a failure result. */
  def isFailure: Boolean = !isSuccess

  /** Gets the success value.
    * @throws NoSuchElementException if this is a failure
    */
  def get: A

  /** Gets the error message.
    * @throws NoSuchElementException if this is a success
    */
  def getError: String

  /** Gets the success value or returns a default.
    * @param default the default value to return if this is a failure
    */
  def getOrElse[B >: A](default: => B): B

  /** Gets the success value or returns a default (Java-friendly).
    * @param defaultValue the default value to return if this is a failure
    */
  def getOrElseValue[B >: A](defaultValue: B): B = getOrElse(defaultValue)

  /** Maps the success value.
    * @param f the function to apply to the success value
    */
  def map[B](f: A => B): Result[B]

  /** FlatMaps the success value.
    * @param f the function to apply to the success value
    */
  def flatMap[B](f: A => Result[B]): Result[B]

  /** Converts to Either for Scala interop. */
  def toEither: Either[String, A]

  /** Converts to Option for Scala interop. */
  def toOption: Option[A]
}

object Result {

  /** Creates a success result. */
  def success[A](value: A): Result[A] = Success(value)

  /** Creates a failure result. */
  def failure[A](error: String): Result[A] = Failure(error)

  /** Creates a Result from Either. */
  def fromEither[A](either: Either[String, A]): Result[A] = either match {
    case Right(value) => Success(value)
    case Left(error)  => Failure(error)
  }

  /** Creates a Result from Option. */
  def fromOption[A](option: Option[A], error: => String): Result[A] = option match {
    case Some(value) => Success(value)
    case None        => Failure(error)
  }

  private case class Success[A](value: A) extends Result[A] {
    def isSuccess: Boolean = true
    def get: A = value
    def getError: String = throw new NoSuchElementException("Success.getError")
    def getOrElse[B >: A](default: => B): B = value
    def map[B](f: A => B): Result[B] = Success(f(value))
    def flatMap[B](f: A => Result[B]): Result[B] = f(value)
    def toEither: Either[String, A] = Right(value)
    def toOption: Option[A] = Some(value)
  }

  private case class Failure[A](error: String) extends Result[A] {
    def isSuccess: Boolean = false
    def get: A = throw new NoSuchElementException(s"Failure.get: $error")
    def getError: String = error
    def getOrElse[B >: A](default: => B): B = default
    def map[B](f: A => B): Result[B] = Failure(error)
    def flatMap[B](f: A => Result[B]): Result[B] = Failure(error)
    def toEither: Either[String, A] = Left(error)
    def toOption: Option[A] = None
  }
}
