package com.vportnov.locations.api.types.request

import io.circe.{ Encoder, Decoder }
import io.circe.generic.semiauto._
import sttp.tapir.Schema
import sttp.tapir.json.circe.jsonBody

import com.vportnov.locations.api.types.field.{ Id, Longitude, Latitude }
import com.vportnov.locations.model


final case class Update(locations: List[Update.Location]):
  def toModel: List[model.Location.WithoutCreatedField] = locations.map(_.toModel)

object Update:
  final case class Location(id: Id, longitude: Longitude, latitude: Latitude):
    def toModel = model.Location.WithoutCreatedField(id.v, longitude.v, latitude.v)

  object meta:
    val description = "An object with data to update location."
    val name = "LocationToUpdate"

  import Id.given
  import Longitude.given
  import Latitude.given

  given Schema[Location] = Schema.derived[Location]
    .name(Schema.SName(meta.name))
    .description(meta.description)

  given Encoder[Location] = deriveEncoder[Location]
  given Decoder[Location] = deriveDecoder[Location]

  val body = jsonBody[List[Update.Location]].mapTo[Update]
  val input = body
