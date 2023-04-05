package com.vportnov.locations.api.types.field

import java.time.{ ZonedDateTime, ZoneOffset }
import sttp.tapir.{ Schema, EndpointInput, query }

import com.vportnov.locations.model


final case class Period(from: Period.Underlying, to: Period.Underlying):
  def isEmpty: Boolean = from.isEmpty && to.isEmpty

  def toModel: model.Period = model.Period(
    from.map(_.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime),
    to.map(_.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime)
  )

object Period:
  type Underlying = Option[ZonedDateTime]

  def apply(from: Period.Underlying, to: Period.Underlying): Period = new Period(from, to)

  def asQuery(): EndpointInput[Period] = from.and(to).mapTo[Period]

  private def from: EndpointInput.Query[Period.Underlying] =
    query[Period.Underlying](meta.from.label)
      .description(meta.from.description)

  private def to: EndpointInput.Query[Period.Underlying] =
    query[Period.Underlying](meta.to.label)
      .description(meta.to.description)

  object meta:
    object from:
      val label = "from"
      val description = s"""|UTC timestampt to filter out locations created before its value.
                           |
                           |${common.format}
                           |""".stripMargin

    object to:
      val label = "to"
      val description = s"""|UTC timestampt to filter out locations created after its value.
                           |
                           |${common.format}
                           |""".stripMargin

    object common:
      val format = "Must be given in ISO format `YYYY-MM-DDTHH:mm[:ss.sss]Z`"
