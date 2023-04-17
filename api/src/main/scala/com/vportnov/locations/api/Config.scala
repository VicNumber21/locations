package com.vportnov.locations.api

import scala.reflect.ClassTag
import cats.effect.Sync
import cats.syntax.all._
import com.comcast.ip4s._
import pureconfig._



object Config:
  final case class App(isSwaggerUIEnabled: Boolean)
  final case class Address(host: Host, port: Port)
  final case class Settings(app: App, http: Address, grpc: Address)

  given ConfigReader[App] = ConfigReader.forProduct1[App, String]("swaggerUI")(str => App(str.toLowerCase == "on"))
  given ConfigReader[Host] = ConfigReader.fromStringOpt(Host.fromString)
  given ConfigReader[Port] = ConfigReader.fromStringOpt(Port.fromString)
  given ConfigReader[Address] = ConfigReader.forProduct2("host", "port")(Address(_, _))

  def loadSettings[F[_]: Sync] =
    for
      app <- load[F, App]("app")
      http <- load[F, Address]("http")
      grpc <- load[F, Address]("grpc")
    yield Settings(app, http, grpc)

  private def load[F[_]: Sync, T: ClassTag](section: String)(using reader: ConfigReader[T]): F[T] =
    Sync[F].delay { ConfigSource.default.at(section).loadOrThrow[T] }