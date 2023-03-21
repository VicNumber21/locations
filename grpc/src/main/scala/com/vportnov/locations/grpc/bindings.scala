package com.vportnov.locations.grpc.bindings

import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.ByteString

import java.time.LocalDateTime
import java.time.ZoneOffset

import com.vportnov.locations.model
import com.vportnov.locations.grpc



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
      .withDate(stats.date.atTime(0, 0).toMessage)
      .withCount(stats.count)

extension (stats: grpc.LocationStats)
  def toModel: model.Location.Stats =
    model.Location.Stats(stats.getDate.toModel.toLocalDate, stats.count)

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

extension (l: grpc.Location)
  def toLocationWithCreatedField: model.Location.WithCreatedField =
    model.Location.WithCreatedField(l.id, l.getLongitude.toModel, l.getLatitude.toModel, l.getCreated.toModel)