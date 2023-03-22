package com.vportnov.locations.model

import scala.math.BigDecimal
import java.time.LocalDateTime


object Location:
  type Id = String
  type Ids = List[Id]
  type Longitude = BigDecimal
  type Latitude = BigDecimal

  final case class WithCreatedField(id: Id, longitude: Longitude, latitude: Latitude, created: LocalDateTime)
  def apply(id: Id, longitude: Longitude, latitude: Latitude, created: LocalDateTime) =
    WithCreatedField(id, longitude, latitude, created)

  final case class WithOptionalCreatedField(id: Id, longitude: Longitude, latitude: Latitude, created: OptionalDateTime)
  def apply(id: Id, longitude: Longitude, latitude: Latitude, created: OptionalDateTime) =
    WithOptionalCreatedField(id, longitude, latitude, created)

  final case class WithoutCreatedField(id: Id, longitude: Longitude, latitude: Latitude)
  def apply(id: Id, longitude: Longitude, latitude: Latitude) =
    WithoutCreatedField(id, longitude, latitude)

  // TODO rework to LocalDateTime
  import java.time.LocalDate
  final case class Stats(date: LocalDate, count: Int)