package com.vportnov.locations.utils

import cats.effect.Sync
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import fs2.Stream

import com.vportnov.locations.utils.ServerError.syntax._


trait LoggingIO[F[_]: Sync]:
  given Logger[F] = Slf4jLogger.getLoggerFromClass(getClass())
  val log = Logger[F]

  protected def logStreamError(message: String = ""): PartialFunction[Throwable, Stream[[x] =>> F[x], Unit]] =
    error => Stream.eval(Logger[F].error(error)(message))
  
  extension [T] (stream: Stream[F, T])(using log: Logger[F])
    def logWhenDone: Stream[F, T] =
      val parent = Thread.currentThread().getStackTrace()(4)
      stream
        .onFinalize(log.info(s"${parent.getMethodName()} stream is done"))
        .logError

    def logError: Stream[F, T] =
      val parent = Thread.currentThread().getStackTrace()(4)
      stream
        .failureToServerError
        .onError(logStreamError(s"Exception in ${parent}"))

  extension [T] (io: F[T])(using log: Logger[F])
    def logError: F[T] =
      val parent = Thread.currentThread().getStackTrace()(4)
      io
        .failureToServerError
        .onError(error => log.error(error)(s"Exception in ${parent}"))
