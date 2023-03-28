package com.vportnov.locations.api.types.field

import io.circe.{ Encoder, Decoder }
import sttp.tapir.{ Schema, Validator }


final case class Count(v: Count.Underlying)
object Count:
  type Underlying = Int

  def apply(v: Count.Underlying): Count = new Count(v)

  object meta:
    val description = "Count of locations created at particular date."
    val example = 5
    val underlyingValidator = Validator.positiveOrZero[Int]
    val validator = Validator.Mapped[Count, Count.Underlying](underlyingValidator, _.v)

  given Schema[Count] = Schema.schemaForString.as[Count]
    .description(meta.description)
    .encodedExample(meta.example)
    .validate(meta.validator)

  given Encoder[Count] = Encoder.encodeInt.contramap(_.v)
  given Decoder[Count] = Decoder.decodeInt.map(Count(_))
