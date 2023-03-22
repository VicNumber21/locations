package com.vportnov.locations.api

import cats.effect.Sync
import com.comcast.ip4s._
import pureconfig._


final case class ApiConfig(host: Host, port: Port)

object ApiConfig:
  given ConfigReader[Host] = ConfigReader.fromStringOpt(Host.fromString)
  given ConfigReader[Port] = ConfigReader.fromStringOpt(Port.fromString)
  given ConfigReader[ApiConfig] = ConfigReader.forProduct2("host", "port")(ApiConfig(_, _))

  def load[F[_]: Sync]: F[ApiConfig] =
    Sync[F].delay { ConfigSource.default.at("api").loadOrThrow[ApiConfig] }