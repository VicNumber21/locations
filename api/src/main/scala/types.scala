package com.vportnov.locations.api

import scala.util.matching.Regex
import scala.math.BigDecimal
import java.time.LocalDateTime

import sttp.tapir._
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import io.circe.generic.auto._
import java.time.Period


object types:
  object api:
    object create:
      type Request = List[structures.LocationRequest]
      type Response = List[structures.LocationResponse]
    
    object read:
      type Request = structures.PeriodQuery
      type Response = List[structures.LocationResponse]

    object update:
      type Request = List[structures.LocationRequest]
      type Response = List[structures.LocationResponse]

  object structures:
    @description(meta.request.create.description)
    final case class LocationRequest(
      @description(meta.id.description)
      @encodedExample(meta.id.example)
      @validate(meta.id.validator)
      id: String,

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
    )

    @description(meta.response.description)
    final case class LocationResponse(
      @description(meta.id.description)
      @encodedExample(meta.id.example)
      @validate(meta.id.validator)
      id: String,

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

    type OptionalDateTime = Option[LocalDateTime]
    type OptionalBoolean = Option[Boolean]
    final case class PeriodQuery(from: OptionalDateTime, to: OptionalDateTime)

    object meta:
      object request:
        object create:
          val description = "An object with information to create location."

      object response:
        val description = "An object with information about requested location."

      object id:
        val description = "Alphanameric id of location."
        val example = "location123"
        val validator = Validator.nonEmptyString.and(Validator.maxLength(255)).and(Validator.pattern("^[a-zA-Z0-9]+$"))

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