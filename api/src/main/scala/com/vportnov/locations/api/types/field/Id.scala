package com.vportnov.locations.api.types.field

import io.circe.{ Encoder, Decoder }
import sttp.tapir.{ Schema, Validator }

import com.vportnov.locations.model

import com.vportnov.locations.api.types.field.Id

final case class Id(v: Id.Underlying)
object Id:
  type Underlying = model.Location.Id

  def apply(v: Id.Underlying): Id = new Id(v)

  object meta:
    val description = "Alphanameric id of location."
    val example = "location123"
    val underlyingValidator = Validator.nonEmptyString[Id.Underlying].and(Validator.maxLength(255)).and(Validator.pattern("^[a-zA-Z0-9]+$"))
    val validator = Validator.Mapped[Id, Id.Underlying](underlyingValidator, _.v)

  given Schema[Id] = Schema.schemaForString.as[Id]
    .description(meta.description)
    .encodedExample(meta.example)
    .validate(meta.validator)

  given Encoder[Id] = Encoder.encodeString.contramap(_.v)
  given Decoder[Id] = Decoder.decodeString.map(Id(_))
