package com.vportnov.locations.api.types.response

import io.circe.{ Encoder, Decoder }
import io.circe.generic.semiauto._
import sttp.tapir.Schema

// TODO improve error response
case class ServerError(code: Int, message: String)
object ServerError:
  def apply(message: String): ServerError = ServerError(500, message)

  given Schema[ServerError] = Schema.derived[ServerError]

  given Encoder[ServerError] = deriveEncoder[ServerError]
  given Decoder[ServerError] = deriveDecoder[ServerError]
