package com.vportnov.locations.api.types.request

import com.vportnov.locations.api.types.field.{ Id, Ids }


type Delete = Ids
object Delete:
  val input = Id.asNonEmptyQuery()
  