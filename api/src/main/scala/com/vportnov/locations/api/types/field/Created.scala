package com.vportnov.locations.api.types.field

import java.time.{ ZonedDateTime, ZoneOffset }
import io.circe.{ Encoder, Decoder }
import sttp.tapir.Schema

import com.vportnov.locations.model

import com.vportnov.locations.api.types.field.Created

final case class Created(v: Created.Underlying):
  def toModel: model.Location.Timestamp = v.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime

object Created:
  type Underlying = ZonedDateTime

  def apply(v: Created.Underlying): Created = new Created(v)
  def apply(v: model.Location.Timestamp) = new Created(v.atZone(ZoneOffset.UTC))

  object meta:
    val description = "UTC timestamp in ISO format when location created."
    val example = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)

  given Schema[Created] = Schema.schemaForZonedDateTime.as[Created]
    .description(meta.description)
    .encodedExample(meta.example)

  given Encoder[Created] = Encoder.encodeZonedDateTime.contramap(_.v)
  given Decoder[Created] = Decoder.decodeZonedDateTime.map(Created(_))
