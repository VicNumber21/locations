package com.vportnov.locations.api.types.request

import io.circe.{ Encoder, Decoder }
import io.circe.generic.semiauto._
import sttp.tapir.Schema
import sttp.tapir.json.circe.jsonBody

import com.vportnov.locations.api.types.schema
import com.vportnov.locations.api.types.field.{ Id, Longitude, Latitude }
import com.vportnov.locations.model


final case class UpdateOne(id: Id, data: UpdateOne.LocationData):
  def toModel: model.Location.WithoutCreatedField =
    model.Location.WithoutCreatedField(id.v, data.longitude.v, data.latitude.v)

object UpdateOne:
  final case class LocationData(longitude: Longitude, latitude: Latitude)

  object meta:
    val description = "An object with data to update a single location."
    val name = "DataToUpdateOneLocation"

  import Longitude.given
  import Latitude.given

  given Schema[LocationData] = Schema.derived[LocationData]
    .name(schema.nameForRequest(meta.name))
    .description(meta.description)

  given Encoder[LocationData] = deriveEncoder[LocationData]
  given Decoder[LocationData] = deriveDecoder[LocationData]

  val body = jsonBody[UpdateOne.LocationData]
  val input = Id.asPath().and(body).mapTo[UpdateOne]
