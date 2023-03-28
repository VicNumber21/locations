package com.vportnov.locations.api.types.request

import com.vportnov.locations.api.types.field.Id


type GetOne = Id
object GetOne:
  val input = Id.asPath()
  