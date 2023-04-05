package com.vportnov.locations.api.types.field

import io.circe.{ Encoder, Decoder }
import sttp.tapir.{ Schema, Validator, EndpointInput, path, query }

import com.vportnov.locations.model

import com.vportnov.locations.api.types.field.Id

final case class Id(v: Id.Underlying)
final case class Ids(v: List[Id.Underlying]):
  def isEmpty: Boolean = v.isEmpty

object Id:
  type Underlying = model.Location.Id

  def apply(v: Id.Underlying): Id = new Id(v)
  def example: Id = Id(meta.example)

  def asPath(label: String = meta.label): EndpointInput[Id] =
    path[Id.Underlying](label)
      .description(meta.description)
      .validate(meta.underlyingValidator)
      .mapTo[Id]

  def asOptionalQuery(label: String = meta.label): EndpointInput[Ids] =
    asQuery(label)
      .description(meta.optionalListDescription)
      .mapTo[Ids]

  def asNonEmptyQuery(label: String = meta.label): EndpointInput[Ids] =
    asQuery(label)
      .description(meta.nonEmptylListDescription)
      .validate(Validator.nonEmpty)
      .mapTo[Ids]

  private def asQuery(label: String = meta.label): EndpointInput.Query[List[Id.Underlying]] =
    query[List[Id.Underlying]](label)
      .validateIterable(meta.underlyingValidator)

  object meta:
    val label = "id"
    val description = "Alphanumeric id of location."
    val optionalListDescription = "Optinal list of alphanumeric ids of required locations."
    val nonEmptylListDescription = "Non empty list of alphanumeric ids of required locations."
    val example = "location123"
    val underlyingValidator = Validator.nonEmptyString[Id.Underlying].and(Validator.maxLength(255)).and(Validator.pattern("^[a-zA-Z0-9]+$"))
    val validator = Validator.Mapped[Id, Id.Underlying](underlyingValidator, _.v)

  given Schema[Id] = Schema.schemaForString.as[Id]
    .description(meta.description)
    .encodedExample(meta.example)
    .validate(meta.validator)

  given Encoder[Id] = Encoder.encodeString.contramap(_.v)
  given Decoder[Id] = Decoder.decodeString.map(Id(_))
