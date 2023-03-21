package com.vportnov.locations.grpc.bindings

import com.vportnov.locations.model
import com.vportnov.locations.grpc

import com.google.protobuf.timestamp.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset



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
