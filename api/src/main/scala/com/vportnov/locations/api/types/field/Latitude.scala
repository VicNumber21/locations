package com.vportnov.locations.api.types.field

import scala.math.BigDecimal
import io.circe.{ Encoder, Decoder }
import sttp.tapir.{ Schema, Validator }

import com.vportnov.locations.model


final case class Latitude(v: Latitude.Underlying)
object Latitude:
  type Underlying = model.Location.Latitude

  def apply(v: Latitude.Underlying): Latitude = new Latitude(v)

  object meta:
    val description = "Latitude of location in decimal format."
    val example = -23.654123
    val underlyingValidator = Validator.inRange(BigDecimal(-90), BigDecimal(90))
    val validator = Validator.Mapped[Latitude, Latitude.Underlying](underlyingValidator, _.v)

  given Schema[Latitude] = Schema.schemaForBigDecimal.as[Latitude]
    .description(meta.description)
    .encodedExample(meta.example)
    .validate(meta.validator)

  given Encoder[Latitude] = Encoder.encodeBigDecimal.contramap(_.v)
  given Decoder[Latitude] = Decoder.decodeBigDecimal.map(Latitude(_))
