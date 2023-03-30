package com.vportnov.locations.api.types.json

import sttp.tapir._
import sttp.capabilities.fs2.Fs2Streams
import java.nio.charset.StandardCharsets


def fs2StreamJsonBodyUTF8[F[_], T](using schema: Schema[T]) =
  streamBody(Fs2Streams[F])(schema, CodecFormat.Json(), Option(StandardCharsets.UTF_8))
