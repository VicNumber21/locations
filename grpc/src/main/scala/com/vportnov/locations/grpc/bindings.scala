package com.vportnov.locations.grpc.bindings

import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.ByteString

import java.time.{ LocalDateTime, ZoneOffset }

import com.vportnov.locations.{ model, grpc }



extension (timestamp: LocalDateTime)
  def toMessage: Timestamp =
    val utc = timestamp.atZone(ZoneOffset.UTC)
    Timestamp(utc.toEpochSecond, utc.getNano)

extension (timestamp: Timestamp)
  def toModel: LocalDateTime =
    LocalDateTime.ofEpochSecond(timestamp.seconds, timestamp.nanos, ZoneOffset.UTC)

extension (timestamp: model.OptionalDateTime)
  def toMessage: Option[Timestamp] =
    timestamp.map(_.toMessage)

extension (timestamp: Option[Timestamp])
  def toModel: model.OptionalDateTime =
    timestamp.map(_.toModel)

extension (period: model.Period)
  def toMessage: grpc.Period =
    grpc.Period(period.from.toMessage, period.to.toMessage)

extension (period: grpc.Period)
  def toModel: model.Period =
    model.Period(period.from.toModel, period.to.toModel)

extension (stats: model.Location.Stats)
  def toMessage: grpc.LocationStats =
    grpc.LocationStats()
      .withDate(stats.date.toMessage)
      .withCount(stats.count)

extension (stats: grpc.LocationStats)
  def toModel: model.Location.Stats =
    model.Location.Stats(stats.getDate.toModel, stats.count)

extension (decimal: BigDecimal)
  def toMessage: grpc.BigDecimal =
    grpc.BigDecimal()
      .withUnscaled(ByteString.copyFrom(decimal.underlying.unscaledValue.toByteArray))
      .withScale(decimal.scale)

extension (decimal: grpc.BigDecimal)
  def toModel: BigDecimal =
    BigDecimal(BigInt(decimal.unscaled.toByteArray), decimal.scale)

extension (location: model.Location.WithCreatedField)
  def toMessage: grpc.Location =
    grpc.Location()
      .withId(location.id)
      .withLongitude(location.longitude.toMessage)
      .withLatitude(location.latitude.toMessage)
      .withCreated(location.created.toMessage)

extension (location: model.Location.WithOptionalCreatedField)
  def toMessage: grpc.Location =
    grpc.Location(created = location.created.toMessage)
      .withId(location.id)
      .withLongitude(location.longitude.toMessage)
      .withLatitude(location.latitude.toMessage)

extension (location: model.Location.WithoutCreatedField)
  def toMessage: grpc.Location =
    grpc.Location()
      .withId(location.id)
      .withLongitude(location.longitude.toMessage)
      .withLatitude(location.latitude.toMessage)

extension (l: grpc.Location)
  def toLocationWithCreatedField: model.Location.WithCreatedField =
    model.Location.WithCreatedField(l.id, l.getLongitude.toModel, l.getLatitude.toModel, l.getCreated.toModel)

  def toLocationWithOptionalCreatedField: model.Location.WithOptionalCreatedField =
    model.Location.WithOptionalCreatedField(l.id, l.getLongitude.toModel, l.getLatitude.toModel, l.created.toModel)

  def toLocationWithoutCreatedField: model.Location.WithoutCreatedField =
    model.Location.WithoutCreatedField(l.id, l.getLongitude.toModel, l.getLatitude.toModel)

extension[T <: model.Location.Base] (locations: List[T])
  def toMessage: grpc.Locations = 
    grpc.Locations(locations.map {
      case l: model.Location.WithCreatedField => l.toMessage
      case l: model.Location.WithOptionalCreatedField => l.toMessage
      case l: model.Location.WithoutCreatedField => l.toMessage
    })

extension (locations: grpc.Locations)
  def toLocationsWithOptionalCreatedField: List[model.Location.WithOptionalCreatedField] =
    locations.list.toList.map(_.toLocationWithOptionalCreatedField)

  def toLocationsWithoutCreatedField: List[model.Location.WithoutCreatedField] =
    locations.list.toList.map(_.toLocationWithoutCreatedField)