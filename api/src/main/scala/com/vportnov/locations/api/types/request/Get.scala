package com.vportnov.locations.api.types.request

import com.vportnov.locations.api.types.field.{ Id, Ids, Period }


final case class Get(period: Period, ids: Ids)
object Get:
  val input = Period.asQuery().and(Id.asQuery()).mapTo[Get]
  