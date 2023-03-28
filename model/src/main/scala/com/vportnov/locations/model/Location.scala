package com.vportnov.locations.model

import scala.math.BigDecimal
import java.time.LocalDateTime


object Location:
  type Id = String
  type Ids = List[Id]
  type Longitude = BigDecimal
  type Latitude = BigDecimal
  type Timestamp = LocalDateTime
  type OptionalTimestamp = Option[Timestamp]
  type Date = LocalDateTime

  trait Base

  final case class WithCreatedField(id: Id, longitude: Longitude, latitude: Latitude, created: Timestamp) extends Base
  def apply(id: Id, longitude: Longitude, latitude: Latitude, created: Timestamp) =
    WithCreatedField(id, longitude, latitude, created)

  final case class WithOptionalCreatedField(id: Id, longitude: Longitude, latitude: Latitude, created: OptionalTimestamp) extends Base
  def apply(id: Id, longitude: Longitude, latitude: Latitude, created: OptionalTimestamp) =
    WithOptionalCreatedField(id, longitude, latitude, created)

  final case class WithoutCreatedField(id: Id, longitude: Longitude, latitude: Latitude) extends Base
  def apply(id: Id, longitude: Longitude, latitude: Latitude) =
    WithoutCreatedField(id, longitude, latitude)

  final case class Stats(date: Date, count: Int)
