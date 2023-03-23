package com.vportnov.locations.api

import cats.effect.{ IO, IOApp, ExitCode }

import com.vportnov.locations.api.Config
import com.vportnov.locations.api.HttpServer


object App extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    for 
      settings <- Config.loadSettings[IO]
      server = new HttpServer[IO](settings)
      exitCode <- server.run
    yield exitCode