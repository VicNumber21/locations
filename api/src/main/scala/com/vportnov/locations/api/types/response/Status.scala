package com.vportnov.locations.api.types.response

import io.circe.{ Encoder, Decoder, Json }
import io.circe.syntax._
import io.circe.generic.semiauto._
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.model.StatusCode
import java.util.UUID
import sttp.tapir.EndpointOutput.OneOf
import sttp.tapir.EndpointOutput.OneOfVariant
import com.vportnov.locations.api.types.response.Status.InternalServerError.asJsonBody


// TODO improve error response
sealed trait Status:
  val code: Int
  def isError: Boolean = code >= 400
  def toStatusCode: StatusCode = StatusCode(code)
  def toJson: Json =
    this match
      case me: Status.Ok => me.asJson
      case me: Status.NoContent => me.asJson
      case me: Status.BadRequest => me.asJson
      case me: Status.NotFound => me.asJson
      case me: Status.Conflict => me.asJson
      case me: Status.InternalServerError => me.asJson
    

object Status:
  sealed trait Success extends Status


  case class Ok(code: Int = 200) extends Success
  object Ok:
    def asStatusCodeWithEmptyBody(description: String): EndpointOutput[Ok] =
      statusCode(StatusCode.Ok).and(
      emptyOutputAs(Status.Ok())
        .description(description))

  given Schema[Ok] = Schema.derived[Ok]
  given Encoder[Ok] = deriveEncoder[Ok]
  given Decoder[Ok] = deriveDecoder[Ok]


  case class NoContent(code: Int = 204) extends Success
  object NoContent:
    def asStatusCodeWithEmptyBody(description: String): EndpointOutput[NoContent] =
      statusCode(StatusCode.NoContent).and(
      emptyOutputAs(Status.NoContent())
        .description(description))

  given Schema[NoContent] = Schema.derived[NoContent]
  given Encoder[NoContent] = deriveEncoder[NoContent]
  given Decoder[NoContent] = deriveDecoder[NoContent]


  sealed trait Error extends Status:
    val uuid: UUID


  case class BadRequest(code: Int = 400, message: Option[String] = None, uuid: UUID) extends Error
  object BadRequest:
    def apply(message: String, uuid: UUID): BadRequest = new BadRequest(message = Some(message), uuid = uuid)
    def apply(message: String): BadRequest = BadRequest(message, UUID.randomUUID())
    def example: BadRequest = BadRequest("Incorrect parameters of request. Details could be found by uuid")
    def asStatusCodeWithJsonBody: EndpointOutput[BadRequest] =
        statusCode(StatusCode.BadRequest).and(asJsonBody)
    def asJsonBody: EndpointOutput[BadRequest] =
      jsonBody[Status.BadRequest]
        .description("invalid value of request parameter.")
        .example(Status.BadRequest.example)

  given Schema[BadRequest] = Schema.derived[BadRequest]
  given Encoder[BadRequest] = deriveEncoder[BadRequest]
  given Decoder[BadRequest] = deriveDecoder[BadRequest]


  case class NotFound(code: Int = 404, message: Option[String] = None, uuid: UUID) extends Error
  object NotFound:
    def apply(message: String, uuid: UUID) = new NotFound(message = Some(message), uuid = uuid)

  given Schema[NotFound] = Schema.derived[NotFound]
  given Encoder[NotFound] = deriveEncoder[NotFound]
  given Decoder[NotFound] = deriveDecoder[NotFound]


  case class Conflict(code: Int = 404, message: Option[String] = None, uuid: UUID) extends Error
  object Conflict:
    def apply(message: String, uuid: UUID) = new Conflict(message = Some(message), uuid = uuid)

  given Schema[Conflict] = Schema.derived[Conflict]
  given Encoder[Conflict] = deriveEncoder[Conflict]
  given Decoder[Conflict] = deriveDecoder[Conflict]


  case class InternalServerError(code: Int = 500, message: Option[String] = None, uuid: UUID) extends Error
  object InternalServerError:
    def apply(message: String, uuid: UUID): InternalServerError = new InternalServerError(message = Some(message), uuid = uuid)
    def apply(message: String): InternalServerError = InternalServerError(message, UUID.randomUUID())
    def example: InternalServerError = InternalServerError("Exception in server log could be found by uuid")
    def asStatusCodeWithJsonBody: EndpointOutput[InternalServerError] =
      statusCode(StatusCode.InternalServerError).and(asJsonBody)
    def asJsonBody: EndpointOutput[InternalServerError] =
      jsonBody[Status.InternalServerError]
        .description("Generic server error.")
        .example(Status.InternalServerError.example)

  given Schema[InternalServerError] = Schema.derived[InternalServerError]
  given Encoder[InternalServerError] = deriveEncoder[InternalServerError]
  given Decoder[InternalServerError] = deriveDecoder[InternalServerError]
