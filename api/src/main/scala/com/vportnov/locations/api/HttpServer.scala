package com.vportnov.locations.api

import cats.effect.{ Async, Sync, ExitCode }
import cats.syntax.all._
import org.http4s.ember.server.EmberServerBuilder

import com.vportnov.locations.api.Config
import com.vportnov.locations.api.HttpService

import com.vportnov.locations.api.GrpcStorage


final class HttpServer[F[_]: Async](settings: Config.Settings):
  def run: F[ExitCode] = server

  private val storage = new GrpcStorage[F](settings.grpc)
  private val service = new HttpService(storage, settings.app.isSwaggerUIEnabled)

  private val server =
    EmberServerBuilder
      .default[F]
      .withHost(settings.http.host)
      .withPort(settings.http.port)
      .withHttpApp(service.app)
      .build
      .use(_ => Async[F].never.as(ExitCode.Success))