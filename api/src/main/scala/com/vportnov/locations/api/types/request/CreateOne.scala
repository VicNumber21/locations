package com.vportnov.locations.api.types.request

import io.circe.{ Encoder, Decoder }
import io.circe.generic.semiauto._
import sttp.tapir.Schema
import sttp.tapir.json.circe.jsonBody

import com.vportnov.locations.api.types.field.{ Id, Longitude, Latitude, OptionalCreated }
import com.vportnov.locations.model


final case class CreateOne(id: Id, data: CreateOne.LocationData):
  def toModel: model.Location.WithOptionalCreatedField =
    model.Location.WithOptionalCreatedField(id.v, data.longitude.v, data.latitude.v, data.created.toModel)

object CreateOne:
  final case class LocationData(longitude: Longitude, latitude: Latitude, created: OptionalCreated)

  object meta:
    val description = "An object with data to create a single location."
    val name = "DataToCreateOneLocation"

  import Longitude.given
  import Latitude.given
  import OptionalCreated.given

  given Schema[LocationData] = Schema.derived[LocationData]
    .name(Schema.SName(meta.name))
    .description(meta.description)

  given Encoder[LocationData] = deriveEncoder[LocationData]
  given Decoder[LocationData] = deriveDecoder[LocationData]

  val body = jsonBody[CreateOne.LocationData]
  val input = Id.asPath().and(body).mapTo[CreateOne]
