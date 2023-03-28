package com.vportnov.locations.api.types.field

import io.circe.{ Encoder, Decoder }
import sttp.tapir.{ Schema, Validator, EndpointInput, path, query }

import com.vportnov.locations.model

import com.vportnov.locations.api.types.field.Id

final case class Id(v: Id.Underlying)
final case class Ids(v: List[Id.Underlying])
object Id:
  type Underlying = model.Location.Id

  def apply(v: Id.Underlying): Id = new Id(v)

  def asPath(label: String = meta.label): EndpointInput[Id] =
    path[Id.Underlying](label)
      .validate(meta.underlyingValidator)
      .mapTo[Id]

  def asQuery(label: String = meta.label): EndpointInput[Ids] =
    query[List[Id.Underlying]](label)
      .validateIterable(meta.underlyingValidator)
      .mapTo[Ids]

  object meta:
    val label = "id"
    val description = "Alphanumeric id of location."
    val example = "location123"
    val underlyingValidator = Validator.nonEmptyString[Id.Underlying].and(Validator.maxLength(255)).and(Validator.pattern("^[a-zA-Z0-9]+$"))
    val validator = Validator.Mapped[Id, Id.Underlying](underlyingValidator, _.v)

  given Schema[Id] = Schema.schemaForString.as[Id]
    .description(meta.description)
    .encodedExample(meta.example)
    .validate(meta.validator)

  given Encoder[Id] = Encoder.encodeString.contramap(_.v)
  given Decoder[Id] = Decoder.decodeString.map(Id(_))
