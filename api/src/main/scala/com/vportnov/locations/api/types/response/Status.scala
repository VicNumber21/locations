package com.vportnov.locations.api.types.response

import io.circe.{ Encoder, Decoder, Json }
import io.circe.syntax._
import io.circe.generic.semiauto._
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.model.StatusCode
import java.util.UUID

import com.vportnov.locations.api.types.field


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
    val errorId: UUID


  case class BadRequest(code: Int = 400, message: Option[String] = None, errorId: UUID) extends Error
  object BadRequest:
    def apply(message: String, errorId: UUID): BadRequest = new BadRequest(message = Some(message), errorId = errorId)
    def apply(message: String): BadRequest = BadRequest(message, UUID.randomUUID())
    def example: BadRequest = BadRequest("Incorrect parameters of request. Details could be found in server log by errorId")
    def asStatusCodeWithJsonBody: EndpointOutput[BadRequest] =
        statusCode(StatusCode.BadRequest).and(asJsonBody)
    def asJsonBody: EndpointOutput[BadRequest] =
      jsonBody[Status.BadRequest]
        .description("Invalid value of request parameter or body.")
        .example(Status.BadRequest.example)

  given Schema[BadRequest] = Schema.derived[BadRequest]
  given Encoder[BadRequest] = deriveEncoder[BadRequest]
  given Decoder[BadRequest] = deriveDecoder[BadRequest]


  case class NotFound(code: Int = 404, message: Option[String] = None, errorId: UUID) extends Error
  object NotFound:
    def apply(message: String, errorId: UUID) = new NotFound(message = Some(message), errorId = errorId)

  given Schema[NotFound] = Schema.derived[NotFound]
  given Encoder[NotFound] = deriveEncoder[NotFound]
  given Decoder[NotFound] = deriveDecoder[NotFound]


  case class Conflict(code: Int = 409, message: Option[String] = None, errorId: UUID) extends Error
  object Conflict:
    def apply(id: field.Id, errorId: UUID): Conflict = new Conflict(message = Some(meta.description(id)), errorId = errorId)
    def apply(id: field.Id): Conflict = Conflict(id, UUID.randomUUID())
    def example: Conflict = Conflict(field.Id.example)
    def asStatusCodeWithJsonBody: EndpointOutput[Conflict] =
        statusCode(StatusCode.Conflict).and(asJsonBody)
    def asJsonBody: EndpointOutput[Conflict] =
      jsonBody[Status.Conflict]
        .description("Location cannot be created since another location with given id already exists")
        .example(Status.Conflict.example)
    object meta:
      def description(id: field.Id) = s"Location with id '${id.v}' cannot be created since already exists."


  given Schema[Conflict] = Schema.derived[Conflict]
  given Encoder[Conflict] = deriveEncoder[Conflict]
  given Decoder[Conflict] = deriveDecoder[Conflict]


  case class InternalServerError(code: Int = 500, message: Option[String] = None, errorId: UUID) extends Error
  object InternalServerError:
    def apply(message: String, errorId: UUID): InternalServerError = new InternalServerError(message = Some(message), errorId = errorId)
    def apply(message: String): InternalServerError = InternalServerError(message, UUID.randomUUID())
    def example: InternalServerError = InternalServerError("Exception could be found in server log by errorId")
    def asStatusCodeWithJsonBody: EndpointOutput[InternalServerError] =
      statusCode(StatusCode.InternalServerError).and(asJsonBody)
    def asJsonBody: EndpointOutput[InternalServerError] =
      jsonBody[Status.InternalServerError]
        .description("Generic server error.")
        .example(Status.InternalServerError.example)

  given Schema[InternalServerError] = Schema.derived[InternalServerError]
  given Encoder[InternalServerError] = deriveEncoder[InternalServerError]
  given Decoder[InternalServerError] = deriveDecoder[InternalServerError]
