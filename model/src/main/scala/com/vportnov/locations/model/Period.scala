package com.vportnov.locations.model


import java.time.LocalDateTime


type OptionalDateTime = Option[LocalDateTime]
final case class Period(from: OptionalDateTime, to: OptionalDateTime):
  def isEmpty: Boolean = from.isEmpty && to.isEmpty
