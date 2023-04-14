package com.vportnov.locations.api.types.field

import java.time.{ ZonedDateTime, ZoneOffset }
import sttp.tapir.{ Schema, EndpointInput, query, Validator }

import com.vportnov.locations.model
import com.vportnov.locations.api.types.request.Get.meta.validator.explanation
import com.vportnov.locations.api.types.field.Period.meta.validator.fromNotBiggerTo
import sttp.tapir.ValidationResult


final case class Period(from: Period.Underlying, to: Period.Underlying):
  def isEmpty: Boolean = from.isEmpty && to.isEmpty

  def toModel: model.Period = model.Period(
    from.map(_.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime),
    to.map(_.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime)
  )

object Period:
  type Underlying = Option[ZonedDateTime]

  def apply(from: Period.Underlying, to: Period.Underlying): Period = new Period(from, to)

  def asQuery(): EndpointInput[Period] = from.and(to)
    .mapTo[Period]
    .validate(fromNotBiggerTo)

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
    
    object validator:
      def fromNotBiggerTo = Validator.custom(logic, Some(explanation))
      def explanation: String = "'from' must be less or equal to 'to' (dates count and time is not)"
      def logic(period: Period) = period match
        case Period(Some(from), Some(to)) if from.toLocalDate().isAfter(to.toLocalDate()) =>
          ValidationResult.Invalid(explanation)
        case _ => ValidationResult.Valid
