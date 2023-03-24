package com.vportnov.locations.utils

import cats.effect.Sync
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import fs2.Stream


trait LoggingIO[F[_]: Sync]:
  given Logger[F] = Slf4jLogger.getLoggerFromClass(getClass())
  val log = Logger[F]

  protected def logStreamError(message: String): PartialFunction[Throwable, Stream[[x] =>> F[x], Unit]] =
    error => Stream.eval(Logger[F].error(error)(message))