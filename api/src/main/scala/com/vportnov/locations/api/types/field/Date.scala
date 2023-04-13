package com.vportnov.locations.api.types.field

import java.time.{ ZonedDateTime, ZoneOffset }
import io.circe.{ Encoder, Decoder }
import sttp.tapir.Schema

import com.vportnov.locations.model


final case class Date(v: Date.Underlying)
object Date:
  type Underlying = ZonedDateTime

  def apply(v: Date.Underlying): Date = new Date(v)
  def apply(v: model.Location.Timestamp) = new Date(v.atZone(ZoneOffset.UTC))

  object meta:
    val description = "UTC timestamp in ISO format when location created rounded to a date."
    val example = ZonedDateTime.now().toLocalDate().atStartOfDay().atZone(ZoneOffset.UTC)

  given Schema[Date] = Schema.schemaForZonedDateTime.as[Date]
    .description(meta.description)
    .encodedExample(meta.example)

  given Encoder[Date] = Encoder.encodeZonedDateTime.contramap(_.v)
  given Decoder[Date] = Decoder.decodeZonedDateTime.map(Date(_))
