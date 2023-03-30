package com.vportnov.locations.utils

import java.util.UUID

final class ServerError(message: String, kind: ServerError.Kind, uuid: UUID = UUID.randomUUID())
                            extends Exception(s"errorId = ${uuid}\n${message}"):
  def this(message: String, cause: Throwable) =
    this(message, ServerError.Kind.Internal)
    initCause(cause)

  def this(cause: Throwable) =
    this(Option(cause).map(_.toString).orNull, cause)

object ServerError:
  def apply() = Internal("")
  def apply(message: String) = Internal(message)

  enum Kind:
    case IllegalArgument
    case Internal
  
  def IllegalArgument(message: String) = new ServerError(message, ServerError.Kind.IllegalArgument)
  def Internal(message: String) = new ServerError(message, ServerError.Kind.Internal)
  def Internal(cause: Throwable) = cause match
    case cause: ServerError => cause
    case _ =>new ServerError(cause)
