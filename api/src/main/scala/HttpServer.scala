package com.vportnov.location.api

import cats.effect._
import com.comcast.ip4s._
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.ember.server._
import scala.concurrent.ExecutionContext



// TODO move to other files
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import io.circe.Encoder.AsArray.importedAsArrayEncoder
import io.circe.Encoder.AsObject.importedAsObjectEncoder
import io.circe.Encoder.AsRoot.importedAsRootEncoder

import java.time.LocalDateTime
import scala.math.BigDecimal


case class Location(id: String, longitude: BigDecimal = 0, latitude: BigDecimal = 0, created: Option[LocalDateTime] = None)


// TODO it seems I need Endcoder and Decoder for Locations
val apiGetLocations: PublicEndpoint[Unit, Unit, Seq[String], Any] = endpoint
  .get
  .in("locations")
  .out(jsonBody[Seq[String]])


implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

val getLocationsRoutes: HttpRoutes[IO] =
  Http4sServerInterpreter[IO]().toRoutes(apiGetLocations.serverLogicSuccess(_ => IO(Seq())))

val swaggerUIRoutes: HttpRoutes[IO] =
  Http4sServerInterpreter[IO]().toRoutes(
    SwaggerInterpreter().fromEndpoints[IO](List(apiGetLocations), "The locations api", "1.0.0")
  )

val routes: HttpRoutes[IO] = getLocationsRoutes <+> swaggerUIRoutes


object LocationApiServer extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(Router("/" -> (routes)).orNotFound)//locationApiService)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
}