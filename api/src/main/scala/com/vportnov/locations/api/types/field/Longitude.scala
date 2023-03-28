package com.vportnov.locations.api.types.field

import scala.math.BigDecimal
import io.circe.{ Encoder, Decoder }
import sttp.tapir.{ Schema, Validator }

import com.vportnov.locations.model


final case class Longitude(v: Longitude.Underlying)
object Longitude:
  type Underlying = model.Location.Longitude

  def apply(v: Longitude.Underlying): Longitude = new Longitude(v)

  object meta:
    val description = "Longitude of location in decimal format."
    val example = 154.345987
    val underlyingValidator = Validator.inRange(BigDecimal(-180), BigDecimal(180))
    val validator = Validator.Mapped[Longitude, Longitude.Underlying](underlyingValidator, _.v)

  given Schema[Longitude] = Schema.schemaForBigDecimal.as[Longitude]
    .description(meta.description)
    .encodedExample(meta.example)
    .validate(meta.validator)

  given Encoder[Longitude] = Encoder.encodeBigDecimal.contramap(_.v)
  given Decoder[Longitude] = Decoder.decodeBigDecimal.map(Longitude(_))
