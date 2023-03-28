package com.vportnov.locations.api.types.request

import com.vportnov.locations.api.types.field.Period


type Stats = Period
object Stats:
  val input = Period.asQuery()
