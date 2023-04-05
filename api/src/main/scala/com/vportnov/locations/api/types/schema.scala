package com.vportnov.locations.api.types

import sttp.tapir.Schema


object schema:
  def nameForRequest(name: String): Schema.SName = schemaName("Request", name)
  def nameForRequest[T](obj: T): Schema.SName = nameForRequest(className(obj))

  def nameForResponse(name: String): Schema.SName = schemaName("Response", name)
  def nameForResponse[T](obj: T): Schema.SName = nameForRequest(className(obj))

  def nameForError(name: String): Schema.SName = schemaName("Failure", name)
  def nameForError[T](obj: T): Schema.SName = nameForError(className(obj))

  private def schemaName(prefix: String, name: String): Schema.SName = Schema.SName(s"${prefix}: ${name}")
  private def className[T](obj: T): String = obj.getClass.getSimpleName.replace("$", "")
