package com.vportnov.locations.utils

import java.util.UUID
import cats.effect.Sync
import cats.syntax.all._
import fs2.Stream


final case class ServerError(message: String, kind: ServerError.Kind, uuid: UUID = UUID.randomUUID())
                            extends Exception(s"errorId = ${uuid}, kind = ${kind}\n${message}"):
  def this(message: String, cause: Throwable, kind: ServerError.Kind) =
    this(message, kind)
    initCause(cause)

  def this(cause: Throwable, kind: ServerError.Kind) =
    this(Option(cause).map(_.toString).orNull, cause, kind)

  def this(cause: Throwable) =
    this(Option(cause).map(_.toString).orNull, cause, ServerError.Kind.Internal)

object ServerError:
  def apply() = Internal("")
  def apply(message: String) = Internal(message)

  enum Kind:
    case IllegalArgument
    case NoSuchElement
    case Internal
  
  def IllegalArgument(message: String) = new ServerError(message, ServerError.Kind.IllegalArgument)
  def NoSuchElement(message: String) = new ServerError(message, ServerError.Kind.NoSuchElement)
  def Internal(message: String) = new ServerError(message, ServerError.Kind.Internal)
  def fromCause(cause: Throwable) = cause match
    case cause: IllegalArgumentException => new ServerError(cause, ServerError.Kind.IllegalArgument)
    case cause: NoSuchElementException => new ServerError(cause, ServerError.Kind.NoSuchElement)
    case cause: ServerError => cause
    case _ =>new ServerError(cause)

  object syntax:
    extension [F[_]: Sync, T] (stream: Stream[F, T])
      def failureToServerError: Stream[F, T] =
        stream.handleError(error => throw ServerError.fromCause(error))

    extension [F[_]: Sync, T] (io: F[T])
      def failureToServerError: F[T] =
        io.handleError(error => throw ServerError.fromCause(error))
