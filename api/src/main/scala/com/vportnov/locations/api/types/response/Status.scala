package com.vportnov.locations.api.types.response

import io.circe.{ Encoder, Decoder }
import io.circe.generic.semiauto._
import sttp.tapir.Schema
import java.util.UUID

// TODO improve error response
sealed trait Status:
  val code: Int
  def isError: Boolean = code >= 400

object Status:
  case class Ok(code: Int = 200) extends Status
  given Schema[Ok] = Schema.derived[Ok]
  given Encoder[Ok] = deriveEncoder[Ok]
  given Decoder[Ok] = deriveDecoder[Ok]

  case class NoContent(code: Int = 204) extends Status
  given Schema[NoContent] = Schema.derived[NoContent]
  given Encoder[NoContent] = deriveEncoder[NoContent]
  given Decoder[NoContent] = deriveDecoder[NoContent]

  case class InternalServerError(code: Int = 500, message: Option[String] = None, uuid: UUID = UUID.randomUUID()) extends Status
  object InternalServerError:
    def apply(message: String) = new InternalServerError(message = Some(message))
  given Schema[InternalServerError] = Schema.derived[InternalServerError]
  given Encoder[InternalServerError] = deriveEncoder[InternalServerError]
  given Decoder[InternalServerError] = deriveDecoder[InternalServerError]
