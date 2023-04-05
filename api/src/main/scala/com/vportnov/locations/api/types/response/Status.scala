package com.vportnov.locations.api.types.response

import io.circe.{ Encoder, Decoder, Json }
import io.circe.syntax._
import io.circe.generic.semiauto._
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.model.StatusCode
import java.util.UUID
import fs2.Stream

import com.vportnov.locations.api.types.{ field, schema }


sealed trait Status:
  val code: Int
  def isError: Boolean = code >= 400
  def toStatusCode: StatusCode = StatusCode(code)
    

object Status:
  sealed trait Success extends Status


  case class Ok(code: Int = 200) extends Success
  object Ok:
    def asStatusCodeWithEmptyBody(description: String): EndpointOutput[Ok] =
      statusCode(StatusCode.Ok).and(
      emptyOutputAs(Status.Ok())
        .description(description))
    def asStatusCodeWithLocationStream[F[_]](description: String): EndpointOutput[Stream[F, Byte]] =
      statusCode(StatusCode.Ok).and(
      Location.body.stream[F].toEndpointIO
        .description(description))
    def asStatusCodeWithStatsStream[F[_]](description: String): EndpointOutput[Stream[F, Byte]] =
      statusCode(StatusCode.Ok).and(
      Stats.body.stream[F].toEndpointIO
        .description(description))
    def asStatusCodeWithLocationJson(description: String) =
      statusCode(StatusCode.Ok).and(
      Location.body.json
        .description(description))


  case class Created(code: Int = 201) extends Success
  object Created:
    def asStatusCodeWithLocationStream[F[_]](description: String): EndpointOutput[Stream[F, Byte]] =
      statusCode(StatusCode.Created).and(
      Location.body.stream[F].toEndpointIO
        .description(description))
    def asStatusCodeWithLocationJson(description: String) =
      statusCode(StatusCode.Created).and(
      Location.body.json
        .description(description))


  case class NoContent(code: Int = 204) extends Success
  object NoContent:
    def asStatusCodeWithEmptyBody(description: String): EndpointOutput[NoContent] =
      statusCode(StatusCode.NoContent).and(
      emptyOutputAs(Status.NoContent())
        .description(description))


  sealed trait Error extends Status:
    val errorId: UUID
    def toJson: Json =
      this match
        case me: Status.BadRequest => me.asJson
        case me: Status.NotFound => me.asJson
        case me: Status.Conflict => me.asJson
        case me: Status.InternalServerError => me.asJson


  case class BadRequest(code: Int = 400, message: Option[String] = None, errorId: UUID) extends Error
  object BadRequest:
    def apply(message: String, errorId: UUID): BadRequest = new BadRequest(message = Some(message), errorId = errorId)
    def apply(message: String): BadRequest = BadRequest(message, UUID.randomUUID())
    def example: BadRequest = BadRequest(meta.exampleMessage)
    def asStatusCodeWithJsonBody: EndpointOutput[BadRequest] =
        statusCode(StatusCode.BadRequest).and(asJsonBody)
    def asJsonBody: EndpointOutput[BadRequest] =
      jsonBody[Status.BadRequest]
        .description(meta.description)
        .example(Status.BadRequest.example)
    object meta:
      def exampleMessage = "Invalid value of id"
      def description = "Invalid value of request parameter or body.\n\nDetails could be found in server log by errorId."

  given Schema[BadRequest] = Schema.derived[BadRequest]
    .name(schema.nameForError(BadRequest))

  given Encoder[BadRequest] = deriveEncoder[BadRequest]
  given Decoder[BadRequest] = deriveDecoder[BadRequest]


  case class NotFound(code: Int = 404, message: Option[String] = None, errorId: UUID) extends Error
  object NotFound:
    def apply(id: field.Id, errorId: UUID): NotFound = new NotFound(message = Some(meta.message(id)), errorId = errorId)
    def apply(id: field.Id): NotFound = NotFound(id, UUID.randomUUID())
    def example: NotFound = NotFound(field.Id.example)
    def asStatusCodeWithJsonBody: EndpointOutput[NotFound] =
        statusCode(StatusCode.NotFound).and(asJsonBody)
    def asJsonBody: EndpointOutput[NotFound] =
      jsonBody[Status.NotFound]
        .description(meta.description)
        .example(Status.NotFound.example)
    object meta:
      def message(id: field.Id) = s"Location with id '${id.v}' does not exist"
      def description = "Location with given id does not exist."

  given Schema[NotFound] = Schema.derived[NotFound]
    .name(schema.nameForError(NotFound))

  given Encoder[NotFound] = deriveEncoder[NotFound]
  given Decoder[NotFound] = deriveDecoder[NotFound]


  case class Conflict(code: Int = 409, message: Option[String] = None, errorId: UUID) extends Error
  object Conflict:
    def apply(id: field.Id, errorId: UUID): Conflict = new Conflict(message = Some(meta.message(id)), errorId = errorId)
    def apply(id: field.Id): Conflict = Conflict(id, UUID.randomUUID())
    def example: Conflict = Conflict(field.Id.example)
    def asStatusCodeWithJsonBody: EndpointOutput[Conflict] =
        statusCode(StatusCode.Conflict).and(asJsonBody)
    def asJsonBody: EndpointOutput[Conflict] =
      jsonBody[Status.Conflict]
        .description(meta.description)
        .example(Status.Conflict.example)
    object meta:
      def message(id: field.Id) = s"Location with id '${id.v}' cannot be created since already exists"
      def description = "Location cannot be created since another location with given id already exists."


  given Schema[Conflict] = Schema.derived[Conflict]
    .name(schema.nameForError(Conflict))

  given Encoder[Conflict] = deriveEncoder[Conflict]
  given Decoder[Conflict] = deriveDecoder[Conflict]


  case class InternalServerError(code: Int = 500, message: Option[String] = None, errorId: UUID) extends Error
  object InternalServerError:
    def apply(errorId: UUID): InternalServerError = new InternalServerError(message = Some(meta.message), errorId = errorId)
    def apply(): InternalServerError = InternalServerError(UUID.randomUUID())
    def example: InternalServerError = InternalServerError()
    def asStatusCodeWithJsonBody: EndpointOutput[InternalServerError] =
      statusCode(StatusCode.InternalServerError).and(asJsonBody)
    def asJsonBody: EndpointOutput[InternalServerError] =
      jsonBody[Status.InternalServerError]
        .description(meta.description)
        .example(Status.InternalServerError.example)
    object meta:
      def message = "Internal Server Error"
      def description = "Default server-side error.\n\nDetails could be found in server log by errorId."

  given Schema[InternalServerError] = Schema.derived[InternalServerError]
    .name(schema.nameForError(InternalServerError))

  given Encoder[InternalServerError] = deriveEncoder[InternalServerError]
  given Decoder[InternalServerError] = deriveDecoder[InternalServerError]
