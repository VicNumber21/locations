package com.vportnov.locations.utils.fs2stream

import cats.effect.{ Sync, Async }
import fs2.Stream
import cats.implicits._


object syntax:
  extension [F[_]: Sync, O] (stream: Stream[F, O])
    def firstEntry: F[O] =
      for
        list <- stream.take(1).compile.toList
      yield list.head

  extension [F[_]: Async, O] (stream: Stream[F, String])
    def catchError(errorMapper: Throwable => String): Stream[F, String] =
      stream
        .attempt
        .map {
          case Right(s) => s
          case Left(e) => errorMapper(e)
        }

    def toJsonArray: Stream[F, String] =
      val prefix = Stream.eval("[".pure[F])
      val suffix = Stream.eval("]".pure[F])
      prefix ++ stream.intersperse(",") ++ suffix