package com.vportnov.locations.api.types.request

import sttp.tapir.{ Validator, ValidationResult }

import com.vportnov.locations.api.types.field.{ Id, Ids, Period }


final case class Get(period: Period, ids: Ids)
object Get:
  val input = Period.asQuery().and(Id.asOptionalQuery())
    .validate(meta.validator.mutuallyExclusive)
    .mapTo[Get]
  
  object meta:
    object validator:
      def mutuallyExclusive = Validator.custom(logic, Some(explanation))
      def explanation: String = "It is not allowed to use id and timestamps filters simultaniously"
      def logic(periodAndIds: (Period, Ids)) = periodAndIds match
        case (period, ids) if !period.isEmpty && !ids.isEmpty => ValidationResult.Invalid(explanation)
        case _ => ValidationResult.Valid
  