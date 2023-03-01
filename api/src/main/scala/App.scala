package com.vportnov.locations.api

import cats.effect._
import com.comcast.ip4s._
import org.http4s.ember.server._

import com.vportnov.locations.api.Service


object App extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    Server.use(_ => IO.never.as(ExitCode.Success))

  private val Server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8080")
    .withHttpApp(Service.app)
    .build