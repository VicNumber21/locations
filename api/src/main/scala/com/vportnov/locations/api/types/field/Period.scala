package com.vportnov.locations.api.types.field

import java.time.{ ZonedDateTime, ZoneOffset }
import sttp.tapir.{ Schema, EndpointInput, query }

import com.vportnov.locations.model


final case class Period(from: Period.Underlying, to: Period.Underlying):
  def toModel: model.Period = model.Period(
    from.map(_.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime),
    to.map(_.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime)
  )

object Period:
  type Underlying = Option[ZonedDateTime]

  def apply(from: Period.Underlying, to: Period.Underlying): Period = new Period(from, to)

  def asQuery(): EndpointInput[Period] =
    query[Period.Underlying]("from")
    .and(query[Period.Underlying]("to"))
    .mapTo[Period]
