package com.vportnov.locations.api.types.response

import io.circe.{ Encoder, Decoder }
import io.circe.generic.semiauto._
import sttp.tapir._

import com.vportnov.locations.api.types.json._
import com.vportnov.locations.api.types.field.{ Date, Count }
import com.vportnov.locations.model


final case class Stats(date: Date, count: Count)
object Stats:
  def from(s: model.Location.Stats) = Stats(Date(s.date), Count(s.count))

  def output[F[_]] = Status.Ok.asStatusCodeWithStatsStream[F](meta.description.Ok)
  
  def error =
    oneOf[Status](
      oneOfVariant(Status.BadRequest.asStatusCodeWithJsonBody),
      oneOfDefaultVariant(Status.InternalServerError.asStatusCodeWithJsonBody)
    )

  object meta:
    object description:
      val Ok = """|Successful response means that valid input was provided.
                  |
                  |A stream of statistic objects is established which eventually transformed into array of the objects.
                  |
                  |Each object represents a count of locations which were created in the given day (in UTC).
                  |
                  |Due to streaming nature of response, it may be followed by error detected after stream is established.
                  |
                  |In such case error is returned as element of the array (see bodies for error cases as examples).
                  |""".stripMargin

    object schema:
      val description = "An object representing statistic of locations created in particular date."
      val name = "Statistic"
  

  import Date.given
  import Count.given

  given Schema[Stats] = Schema.derived[Stats]
    .name(Schema.SName(meta.schema.name))
    .description(meta.schema.description)

  given Encoder[Stats] = deriveEncoder[Stats]
  given Decoder[Stats] = deriveDecoder[Stats]

  object body:
    def stream[F[_]] = fs2StreamJsonBodyUTF8[F, List[Stats]]
