package com.vportnov.locations.api.fs2stream.string

import fs2.Stream
import cats.effect.Async
import cats.syntax.all._


object syntax:
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
