package com.vportnov.locations.api.types.response

import io.circe.{ Encoder, Decoder }
import io.circe.generic.semiauto._
import sttp.tapir.Schema

import com.vportnov.locations.api.tapir.fs2stream.json._
import com.vportnov.locations.api.types.field.{ Date, Count }
import com.vportnov.locations.model


final case class Stats(date: Date, count: Count)
object Stats:
  def from(s: model.Location.Stats) = Stats(Date(s.date), Count(s.count))

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
