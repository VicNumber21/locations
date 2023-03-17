package com.vportnov.locations.api

import scala.util.matching.Regex
import scala.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalDate

import sttp.tapir._
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import io.circe.generic.auto._


object types:
  object lib:
    // TODO move to lib sub-project
    type OptionalDateTime = Option[LocalDateTime]
    final case class Period(from: OptionalDateTime, to: OptionalDateTime):
      def isEmpty: Boolean = from.isEmpty && to.isEmpty

    object Location:
      type Id = String
      type Ids = List[Id]
      type Longitude = BigDecimal
      type Latitude = BigDecimal

      // TODO it could be better to use ZondeDateTime in UTC
      final case class WithCreatedField(id: Id, longitude: Longitude, latitude: Latitude, created: LocalDateTime)
      def apply(id: Id, longitude: Longitude, latitude: Latitude, created: LocalDateTime) =
        WithCreatedField(id, longitude, latitude, created)

      final case class WithOptionalCreatedField(id: Id, longitude: Longitude, latitude: Latitude, created: OptionalDateTime)
      def apply(id: Id, longitude: Longitude, latitude: Latitude, created: OptionalDateTime) =
        WithOptionalCreatedField(id, longitude, latitude, created)

      final case class WithoutCreatedField(id: Id, longitude: Longitude, latitude: Latitude)
      def apply(id: Id, longitude: Longitude, latitude: Latitude) =
        WithoutCreatedField(id, longitude, latitude)

      // TODO check if it is ok to use LocalDate here?
      final case class Stats(date: LocalDate, count: Int)

  object structures:
    // TODO move to lib
    type Id = String

    @description(meta.request.create.description)
    final case class LocationCreateRequest(
      @description(meta.id.description)
      @encodedExample(meta.id.example)
      @validate(meta.id.validator)
      id: Id,

      @description(meta.longitude.description)
      @encodedExample(meta.longitude.example)
      @validate(meta.longitude.validator)
      longitude: BigDecimal,

      @description(meta.latitude.description)
      @encodedExample(meta.latitude.example)
      @validate(meta.latitude.validator)
      latitude: BigDecimal,

      @description(meta.created.optional.description)
      @encodedExample(meta.created.optional.example)
      created: Option[LocalDateTime] = None
    ):
      def toLocation = lib.Location.WithOptionalCreatedField(id, longitude, latitude, created)

    final case class LocationCreateOneRequest(
      @description(meta.longitude.description)
      @encodedExample(meta.longitude.example)
      @validate(meta.longitude.validator)
      longitude: BigDecimal,

      @description(meta.latitude.description)
      @encodedExample(meta.latitude.example)
      @validate(meta.latitude.validator)
      latitude: BigDecimal,

      @description(meta.created.optional.description)
      @encodedExample(meta.created.optional.example)
      created: Option[LocalDateTime] = None
    ):
      def toLocation(id: Id) = lib.Location.WithOptionalCreatedField(id, longitude, latitude, created)

    final case class LocationUpdateRequest(
      @description(meta.id.description)
      @encodedExample(meta.id.example)
      @validate(meta.id.validator)
      id: Id,

      @description(meta.longitude.description)
      @encodedExample(meta.longitude.example)
      @validate(meta.longitude.validator)
      longitude: BigDecimal,

      @description(meta.latitude.description)
      @encodedExample(meta.latitude.example)
      @validate(meta.latitude.validator)
      latitude: BigDecimal,
    ):
      def toLocation = lib.Location.WithoutCreatedField(id, longitude, latitude)

    final case class LocationUpdateOneRequest(
      @description(meta.longitude.description)
      @encodedExample(meta.longitude.example)
      @validate(meta.longitude.validator)
      longitude: BigDecimal,

      @description(meta.latitude.description)
      @encodedExample(meta.latitude.example)
      @validate(meta.latitude.validator)
      latitude: BigDecimal,
    ):
      def toLocation(id: Id) = lib.Location.WithoutCreatedField(id, longitude, latitude)

    @description(meta.response.description)
    final case class LocationResponse(
      @description(meta.id.description)
      @encodedExample(meta.id.example)
      @validate(meta.id.validator)
      id: Id,

      @description(meta.longitude.description)
      @encodedExample(meta.longitude.example)
      @validate(meta.longitude.validator)
      longitude: BigDecimal,

      @description(meta.latitude.description)
      @encodedExample(meta.latitude.example)
      @validate(meta.latitude.validator)
      latitude: BigDecimal,

      @description(meta.created.required.description)
      @encodedExample(meta.created.required.example)
      created: LocalDateTime
    )

    object LocationResponse:
      def from(l: lib.Location.WithCreatedField) = LocationResponse(l.id, l.longitude, l.latitude, l.created)


    // TODO add descriptions
    final case class LocationStats(
      date: LocalDateTime,

      count: Int
    )

    // TODO move to lib
    type OptionalDateTime = Option[LocalDateTime]
    // TODO move to lib
    final case class PeriodQuery(from: OptionalDateTime, to: OptionalDateTime)

    // TODO move to lib
    type IdsQuery = List[Id]

    // TODO improve error response
    case class ServerError(code: Int, message: String)

    object ServerError:
      def apply(message: String): ServerError = ServerError(500, message)


    object meta:
      object request:
        object create:
          val description = "An object with information to create location."

      object response:
        val description = "An object with information about requested location."

      object id:
        val description = "Alphanameric id of location."
        val example = "location123"
        val validator = Validator.nonEmptyString[String].and(Validator.maxLength(255)).and(Validator.pattern("^[a-zA-Z0-9]+$"))

      object longitude:
        val description = "Longitude of location in decimal format."
        val example = 54.345987
        val validator = Validator.inRange(BigDecimal(-180), BigDecimal(180))

      object latitude:
        val description = "Latitude of location in decimal format."
        val example = -23.654123
        val validator = Validator.inRange(BigDecimal(-90), BigDecimal(90))

      object created:
        object optional:
          val description = "Optional time in ISO format when location created. Autogenerated if not given."
          val example = Some(created.required.example)
        object required:
          val description = "Time in ISO format when location created."
          val example = LocalDateTime.now()