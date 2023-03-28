package com.vportnov.locations.api.types.request

import com.vportnov.locations.api.types.field.Id


type DeleteOne = Id
object DeleteOne:
  val input = Id.asPath()
  