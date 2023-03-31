package com.vportnov.locations.grpc.bindings

import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.ByteString

import cats.effect.Sync
import cats.syntax.all._
import java.time.{ LocalDateTime, ZoneOffset }
import java.util.UUID

import com.vportnov.locations.{ model, grpc, utils }
import com.vportnov.locations.utils.ServerError



extension (timestamp: LocalDateTime)
  def toMessage: Timestamp =
    val utc = timestamp.atZone(ZoneOffset.UTC)
    Timestamp(utc.toEpochSecond, utc.getNano)

extension (timestamp: Timestamp)
  def toModel: LocalDateTime =
    LocalDateTime.ofEpochSecond(timestamp.seconds, timestamp.nanos, ZoneOffset.UTC)

extension (timestamp: model.Location.OptionalTimestamp)
  def toMessage: Option[Timestamp] =
    timestamp.map(_.toMessage)

extension (timestamp: Option[Timestamp])
  def toModel: model.Location.OptionalTimestamp =
    timestamp.map(_.toModel)

extension (period: model.Period)
  def toMessage: grpc.Period =
    grpc.Period(period.from.toMessage, period.to.toMessage)

extension (period: grpc.Period)
  def toModel: model.Period =
    model.Period(period.from.toModel, period.to.toModel)
  
extension (ids: model.Location.Ids)
  def toMessage: grpc.Ids =
    grpc.Ids(ids)
  
extension (ids: grpc.Ids)
  def toModel: model.Location.Ids =
    ids.value.toList

extension (ids: Option[grpc.Ids])
  def toModel: model.Location.Ids = ids match
    case None => List.empty
    case some => some.get.toModel

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

extension (kind: utils.ServerError.Kind)
  def toMessage: grpc.ServerError.Kind = kind match
    case utils.ServerError.Kind.IllegalArgument => grpc.ServerError.Kind.IllegalArgument
    case utils.ServerError.Kind.NoSuchElement => grpc.ServerError.Kind.NoSuchElement
    case utils.ServerError.Kind.Internal => grpc.ServerError.Kind.Internal
  
extension (kind: grpc.ServerError.Kind)
  def toModel: utils.ServerError.Kind = kind match
    case grpc.ServerError.Kind.IllegalArgument => utils.ServerError.Kind.IllegalArgument
    case grpc.ServerError.Kind.NoSuchElement => utils.ServerError.Kind.NoSuchElement
    case grpc.ServerError.Kind.Internal => utils.ServerError.Kind.Internal
    case _ => utils.ServerError.Kind.Internal

extension (error: Throwable)
  def toMessage: grpc.ServerError =
    val serverError = utils.ServerError.fromCause(error)
    grpc.ServerError()
      .withUuid(serverError.uuid.toString)
      .withMessage(serverError.message)
      .withKind(serverError.kind.toMessage)

extension (error: grpc.ServerError)
  def toModel: utils.ServerError =
    ServerError.fromRemoteError(error.message, error.kind.toModel, UUID.fromString(error.uuid))

extension [F[_]: Sync] (count: F[Int])
    def toMessage: F[grpc.CountReply] =
      count
        .map(c => grpc.CountReply().withCount(grpc.Count(c)))
        .recover(e => grpc.CountReply().withServerError(e.toMessage))

extension [F[_]: Sync] (countReply: F[grpc.CountReply])
    def toModel: F[Int] = countReply.map { cr =>
      (cr.message.count, cr.message.serverError) match
        case (Some(count), None) => count.value
        case (None, Some(serverError)) => throw serverError.toModel
        case _ => throw ServerError.Internal("Incorrect format of CountReply received")
    }
