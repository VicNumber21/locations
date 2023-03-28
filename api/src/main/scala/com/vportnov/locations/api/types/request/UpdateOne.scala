package com.vportnov.locations.api.types.request

import io.circe.{ Encoder, Decoder }
import io.circe.generic.semiauto._
import sttp.tapir.Schema
import sttp.tapir.json.circe.jsonBody

import com.vportnov.locations.api.types.field.{ Id, Longitude, Latitude }
import com.vportnov.locations.model


// TODO move id to field.Id
final case class UpdateOne(id: model.Location.Id, data: UpdateOne.LocationData):
  def toModel: model.Location.WithoutCreatedField =
    model.Location.WithoutCreatedField(id, data.longitude.v, data.latitude.v)

object UpdateOne:
  final case class LocationData(longitude: Longitude, latitude: Latitude)

  object meta:
    val description = "An object with data to update a single location."
    val name = "DataToUpdateOneLocation"

  import Longitude.given
  import Latitude.given

  given Schema[LocationData] = Schema.derived[LocationData]
    .name(Schema.SName(meta.name))
    .description(meta.description)

  given Encoder[LocationData] = deriveEncoder[LocationData]
  given Decoder[LocationData] = deriveDecoder[LocationData]

  val body = jsonBody[UpdateOne.LocationData]
