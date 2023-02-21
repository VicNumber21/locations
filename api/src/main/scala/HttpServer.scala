package com.vportnov.location.api

import cats.effect._
import com.comcast.ip4s._
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.ember.server._



// TODO here is good example for refactoring:
// https://codeberg.org/wegtam/http4s-tapir.g8/src/branch/main/src/main/g8/src/main/scala/$package__packaged$
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe._

import java.time.LocalDateTime
import scala.math.BigDecimal


case class LocationResponse(id: String, longitude: BigDecimal, latitude: BigDecimal, created: LocalDateTime)

type OptionalDateTime = Option[LocalDateTime]
type OptionalBoolean = Option[Boolean]

case class PeriodQuery(from: OptionalDateTime, to: OptionalDateTime)
val periodQuery: EndpointInput[PeriodQuery] =
  query[OptionalDateTime]("from")
  .and(query[OptionalDateTime]("to"))
  .mapTo[PeriodQuery]

// TODO how to request list of ids?
// type OptionalStrings = Option[Seq[String]]
// case class IdsQuery(id: OptionalStrings)
// val idsQuery: EndpointInput[IdsQuery] = query[OptionalStrings]("ids").mapTo[IdsQuery]

val apiGetLocations: PublicEndpoint[PeriodQuery, String, Seq[LocationResponse], Any] = endpoint
  .get
  .in("locations")
  .in(periodQuery)
  .errorOut(stringBody)
  .out(jsonBody[Seq[LocationResponse]])

case class LocationRequest(id: String, longitude: BigDecimal, latitude: BigDecimal, created: Option[LocalDateTime])

val apiAddLocations: PublicEndpoint[Seq[LocationRequest], String, Seq[LocationResponse], Any] = endpoint
  .post
  .in("locations")
  .in(jsonBody[Seq[LocationRequest]])
  .errorOut(stringBody)
  .out(jsonBody[Seq[LocationResponse]])


val getLocationsRoutes: HttpRoutes[IO] =
  Http4sServerInterpreter[IO]().toRoutes(
    apiGetLocations.serverLogicSuccess(period => period match
      case PeriodQuery(None, None) => IO(Seq(LocationResponse("NoDates", 0, 0, LocalDateTime.now())))
      case PeriodQuery(Some(date), None) => IO(Seq(LocationResponse("OnlyFrom", 0, 0, date)))
      case PeriodQuery(None, Some(date)) => IO(Seq(LocationResponse("OnlyTo", 0, 0, date)))
      case PeriodQuery(Some(_), Some(_)) => IO(Seq(LocationResponse("BothFromAndTo", 0, 0, LocalDateTime.now())))
      case _ => IO(Seq())
    )
  )

val addLocationsRoutes: HttpRoutes[IO] =
  Http4sServerInterpreter[IO]().toRoutes(
    apiAddLocations.serverLogicSuccess(requests => IO(requests.map(request => request match
      case LocationRequest(id, longitude, latitude, None) => LocationResponse(id, longitude, latitude, LocalDateTime.now())
      case LocationRequest(id, longitude, latitude, Some(date)) => LocationResponse(id, longitude, latitude, date)
    )))
  )

val swaggerUIRoutes: HttpRoutes[IO] =
  Http4sServerInterpreter[IO]().toRoutes(
    SwaggerInterpreter().fromEndpoints[IO](List(apiGetLocations, apiAddLocations), "The locations api", "1.0.0")
  )

val routes: HttpRoutes[IO] = getLocationsRoutes <+> addLocationsRoutes <+> swaggerUIRoutes


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