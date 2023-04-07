package com.vportnov.locations.api.types.request

import io.circe.{ Encoder, Decoder }
import io.circe.generic.semiauto._
import sttp.tapir.{ Schema, Validator }
import sttp.tapir.json.circe.jsonBody

import com.vportnov.locations.api.types.schema
import com.vportnov.locations.api.types.field.{ Id, Longitude, Latitude, OptionalCreated }
import com.vportnov.locations.model


final case class Create(locations: List[Create.Location]):
  def toModel: List[model.Location.WithOptionalCreatedField] = locations.map(_.toModel)

object Create:
  final case class Location(id: Id, longitude: Longitude, latitude: Latitude, created: OptionalCreated):
    def toModel = model.Location.WithOptionalCreatedField(id.v, longitude.v, latitude.v, created.toModel)

  object meta:
    val description = "An object with data to create location."
    val name = "LocationToCreate"

  import Id.given
  import Longitude.given
  import Latitude.given
  import OptionalCreated.given

  given Schema[Location] = Schema.derived[Location]
    .name(schema.nameForRequest(meta.name))
    .description(meta.description)

  given Encoder[Location] = deriveEncoder[Location]
  given Decoder[Location] = deriveDecoder[Location]

  val body = jsonBody[List[Create.Location]].validate(Validator.nonEmpty).mapTo[Create]
  val input = body
