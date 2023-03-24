package com.vportnov.locations.svc

import scala.reflect.ClassTag
import cats.effect.Sync
import cats.syntax.all._
import com.comcast.ip4s._
import pureconfig._


object Config:
  final case class Grpc(port: Port)
  final case class Credentials(login: String, password: String)
  final case class Database(driver: String, url: String, name:String, user: Credentials, admin: Credentials):
    def adminUrl: String = s"${url}/"
    def userUrl: String = s"${url}/${name}"

  final case class Settings(grpc: Grpc, db: Database)

  given ConfigReader[Port] = ConfigReader.fromStringOpt(Port.fromString)
  given ConfigReader[Grpc] = ConfigReader.forProduct1("port")(Grpc(_))
  given ConfigReader[Credentials] = ConfigReader.forProduct2("login", "password")(Credentials(_, _))
  given ConfigReader[Database] = ConfigReader.forProduct5("driver", "url", "name", "user", "admin")(Database(_, _, _, _, _))

  def loadSettings[F[_]: Sync] =
    for
      grpc <- load[F, Grpc]("grpc")
      db <- load[F, Database]("db")
    yield Settings(grpc, db)

  private def load[F[_]: Sync, T: ClassTag](section: String)(using reader: ConfigReader[T]): F[T] =
    Sync[F].delay { ConfigSource.default.at(section).loadOrThrow[T] }