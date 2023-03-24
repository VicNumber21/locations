package com.vportnov.locations.api.throwable

import java.io.StringWriter
import java.io.PrintWriter


object syntax:
  extension (error: Throwable)
    def printableStackTrace: String =
      val sw = new StringWriter
      error.printStackTrace(new PrintWriter(sw))
      sw.toString
