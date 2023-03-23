package com.vportnov.locations.svc

import cats.effect.{ IO, IOApp, ExitCode }

import com.vportnov.locations.svc.GrpcServer
import com.vportnov.locations.svc.Config


object App extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    for 
      settings <- Config.loadSettings[IO]
      server = new GrpcServer[IO](settings)
      exitCode <- server.run
    yield exitCode