package com.vportnov.locations.model


import java.time.LocalDateTime


final case class Period(from: Location.OptionalTimestamp, to: Location.OptionalTimestamp):
  def isEmpty: Boolean = from.isEmpty && to.isEmpty
