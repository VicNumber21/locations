package com.vportnov.locations.api.types.response

import io.circe.{ Encoder, Decoder }
import io.circe.generic.semiauto._
import sttp.tapir.Schema
import sttp.tapir.json.circe._

import com.vportnov.locations.api.tapir.fs2stream.json._
import com.vportnov.locations.api.types.field.{ Id, Longitude, Latitude, Created }
import com.vportnov.locations.model


final case class Location(id: Id, longitude: Longitude, latitude: Latitude, created: Created)
object Location:
  def from(l: model.Location.WithCreatedField) = Location(Id(l.id), Longitude(l.longitude), Latitude(l.latitude), Created(l.created))

  object meta:
    val description = "An object with data of requested location."
    val name = "LocationInResponse"

  import Id.given
  import Longitude.given
  import Latitude.given
  import Created.given

  given Schema[Location] = Schema.derived[Location]
    .name(Schema.SName(meta.name))
    .description(meta.description)

  given Encoder[Location] = deriveEncoder[Location]
  given Decoder[Location] = deriveDecoder[Location]

  object body:
    val json = jsonBody[Location]
    def stream[F[_]] = fs2StreamJsonBodyUTF8[F, List[Location]]
