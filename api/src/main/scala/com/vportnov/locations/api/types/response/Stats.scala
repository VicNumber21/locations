package com.vportnov.locations.api.types.response

import io.circe.{ Encoder, Decoder }
import io.circe.generic.semiauto._
import sttp.tapir._
import sttp.model.StatusCode

import com.vportnov.locations.api.types.json._
import com.vportnov.locations.api.types.field.{ Date, Count }
import com.vportnov.locations.model


final case class Stats(date: Date, count: Count)
object Stats:
  def from(s: model.Location.Stats) = Stats(Date(s.date), Count(s.count))

  def output[F[_]] = statusCode(StatusCode.Ok).and(Stats.body.stream[F].toEndpointIO)
  
  def error =
    oneOf[Status](
      oneOfVariant(Status.BadRequest.asStatusCodeWithJsonBody),
      oneOfDefaultVariant(Status.InternalServerError.asStatusCodeWithJsonBody)
    )


  object meta:
    val description = "An object representing statistic of locations created in particular date."
    val name = "Statistic"
  
  import Date.given
  import Count.given

  given Schema[Stats] = Schema.derived[Stats]
    .name(Schema.SName(meta.name))
    .description(meta.description)

  given Encoder[Stats] = deriveEncoder[Stats]
  given Decoder[Stats] = deriveDecoder[Stats]

  object body:
    def stream[F[_]] = fs2StreamJsonBodyUTF8[F, List[Stats]]
