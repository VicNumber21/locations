package com.vportnov.locations.model

import cats.effect.Sync
import fs2.Stream
import cats.implicits._

object StreamExtOps:
  extension [F[_]: Sync, O] (stream: Stream[F, O])
    def firstEntry: F[O] =
      for {
        list <- stream.take(1).compile.toList
      } yield list.head