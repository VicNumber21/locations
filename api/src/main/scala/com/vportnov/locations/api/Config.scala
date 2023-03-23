package com.vportnov.locations.api

import cats.effect.Sync
import cats.syntax.all._
import com.comcast.ip4s._
import pureconfig._



object Config:
  final case class Address(host: Host, port: Port)
  final case class Settings(http: Address, grpc: Address)

  given ConfigReader[Host] = ConfigReader.fromStringOpt(Host.fromString)
  given ConfigReader[Port] = ConfigReader.fromStringOpt(Port.fromString)
  given ConfigReader[Address] = ConfigReader.forProduct2("host", "port")(Address(_, _))

  def loadSettings[F[_]: Sync] =
    for
      apiAddress <- loadApiAddress
      svcAddress <- loadSvcAddress
    yield Settings(apiAddress, svcAddress)

  private def loadApiAddress[F[_]: Sync]: F[Address] = load("http")
  private def loadSvcAddress[F[_]: Sync]: F[Address] = load("grpc")

  private def load[F[_]: Sync](section: String): F[Address] =
    Sync[F].delay { ConfigSource.default.at(section).loadOrThrow[Address] }