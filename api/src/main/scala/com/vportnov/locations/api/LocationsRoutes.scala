package com.vportnov.locations.api

import cats.effect.kernel.Async
import cats.syntax.all._

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes

import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.model.StatusCode
import sttp.tapir.json.circe._
import io.circe.syntax._

// TODO try to rework to semiauto or to manual
import sttp.tapir.generic.auto._
import io.circe.generic.auto._

import sttp.capabilities.fs2.Fs2Streams
import fs2.Stream
import java.nio.charset.StandardCharsets

import org.slf4j.LoggerFactory

import com.vportnov.locations.api.types.lib._
import com.vportnov.locations.api.types.api._
import com.vportnov.locations.api.types.structures._


final class LocationsRoutes[F[_]: Async](storage: StorageExt[F]) extends Http4sDsl[F]:
  import StringStreamOps._
  import ThrowableExtraOps._

  val logger = LoggerFactory.getLogger(classOf[LocationsRoutes[F]])

  val createRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.createEndpoint
        .serverLogicSuccess(request => response(storage.createLocations(request.map(_.toLocation)), "createLocations stream is done"))
    )
  
  val createOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.createOneEndpoint
        .serverLogic((id, request) => response(storage.createLocation(request.toLocation(id)), LocationResponse.from, conflictError))
    )

  val getRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.getEndpoint
        .serverLogicSuccess((period, ids) => response(storage.getLocations(period, ids), "getLocations stream is done"))
    )

  val getOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.getOneEndpoint
        .serverLogic(id => response(storage.getLocation(id), LocationResponse.from, notFoundError))
    )

  val updateRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.updateEndpoint
        .serverLogicSuccess(request => response(storage.updateLocations(request.map(_.toLocation)), "updateLocations stream is done"))
    )
  
  val updateOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.updateOneEndpoint
        .serverLogic((id, request) => response(storage.updateLocation(request.toLocation(id)), LocationResponse.from, notFoundError))
    )

  val deleteRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.deleteEndpoint
        .serverLogic(ids => response(storage.deleteLocations(ids), deleteSuccess, commonErrors))
    )

  val deleteOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.deleteOneEndpoint
        .serverLogic(id => response(storage.deleteLocation(id), deleteSuccess, commonErrors))
    )
  
  val statsRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.statsEndpoint
        .serverLogicSuccess(period => response(storage.locationStats(period), "locationStats stream is done"))
    )
  
  private def response[O](stream: Stream[F, O],logMessage: String)(using encoder: io.circe.Encoder[O]) =
    stream
      .onFinalize(logger.debug(logMessage).pure[F]) // TODO it might be better approach to do this logging in storages
      .map(_.asJson.noSpaces)
      .catchError { error =>
        logException(error)
        ServerError(error.getMessage()).asJson.noSpaces
      }
      .toJsonArray
      .through(fs2.text.utf8.encode)
      .pure[F]

  private def response[SR, T, O](storageResponse: F[SR], mapper: SR => O, errorMapper: Throwable => StatusCode)
                             (using scala.util.NotGiven[SR =:= Either[Throwable, T]]): F[Either[StatusCode, O]] =
    for {
      unpacked <- storageResponse.attempt
    } yield unpacked.map(mapper).left.map(errorMapper)

  private def response[SR, T, O](storageResponse: F[SR], mapper: T => O, errorMapper: Throwable => StatusCode)
                             (using SR =:= Either[Throwable, T]): F[Either[StatusCode, O]] =
    for {
      unpacked <- storageResponse.attempt
    } yield unpacked.joinRight.map(mapper).left.map(errorMapper)

  private def deleteSuccess(count: Int): StatusCode = count match
    case 0 => StatusCode.Ok
    case n if n > 0 => StatusCode.NoContent
    case strange =>
      logger.error(s"This should never come -> ${strange.toString}")
      StatusCode.InternalServerError
  
  private def notFoundError(error: Throwable) = error match
    case _ : java.util.NoSuchElementException => StatusCode.NotFound
    case _ => commonErrors(error)

  private def conflictError(error: Throwable) = error match
    case _ : java.util.NoSuchElementException => StatusCode.Conflict
    case _ => commonErrors(error)

  private def commonErrors(error: Throwable) =
    logException(error)

    error match
      case _: IllegalArgumentException => StatusCode.BadRequest
      case _ => StatusCode.InternalServerError
    
  private def logException(error: Throwable) =
    logger.error(s"Exception on storage request\n${error.printableStackTrace}")
  
  val routes =
    createRoute <+>
    createOneRoute <+>
    getRoute <+>
    getOneRoute <+>
    updateRoute <+>
    updateOneRoute <+>
    deleteRoute <+>
    deleteOneRoute <+>
    statsRoute

object LocationsRoutes:
  val periodQuery: EndpointInput[Period] =
    query[OptionalDateTime]("from")
    .and(query[OptionalDateTime]("to"))
    .mapTo[Period]

  val idsQuery: EndpointInput[Location.Ids] =
    query[IdsQuery]("id")
      .validateIterable(meta.id.validator)
  
  val idPath: EndpointInput[Location.Id] =
    path[Id]("id")
      .validate(meta.id.validator)

  val baseEndpoint = endpoint
    .in("api" / "v1.0" / "locations")
    .errorOut(statusCode) // TODO add json error as ServerError

  def createEndpoint[F[_]](using schema: Schema[List[LocationResponse]]):
          PublicEndpoint[List[LocationCreateRequest], StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .post
    .description("Create locations in batch")
    .in(jsonBody[List[LocationCreateRequest]])
    .out(fs2StreamJsonBodyUTF8[F, List[LocationResponse]])
    .out(statusCode(StatusCode.Created))

  val createOneEndpoint: PublicEndpoint[(Location.Id, LocationCreateOneRequest), StatusCode, LocationResponse, Any] = baseEndpoint
    .post
    .description("Create a single location")
    .in(idPath)
    .in(jsonBody[LocationCreateOneRequest])
    .out(jsonBody[LocationResponse])
    .out(statusCode(StatusCode.Created))

  def getEndpoint[F[_]]: PublicEndpoint[(Period, Location.Ids), StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .get
    .description("Get list of locations: all, particular ids or created before or after or between dates")
    .in(periodQuery).in(idsQuery)
    .out(fs2StreamJsonBodyUTF8[F, List[LocationResponse]])

  val getOneEndpoint: PublicEndpoint[Location.Id, StatusCode, LocationResponse, Any] = baseEndpoint
    .get
    .description("Get particular location given by id")
    .in(idPath)
    .out(jsonBody[LocationResponse])

  def updateEndpoint[F[_]]: PublicEndpoint[List[LocationUpdateRequest], StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .put
    .description("Update longitude and latitude of given location in batch")
    .in(jsonBody[List[LocationUpdateRequest]])
    .out(fs2StreamJsonBodyUTF8[F, List[LocationResponse]])

  val updateOneEndpoint: PublicEndpoint[(Location.Id, LocationUpdateOneRequest), StatusCode, LocationResponse, Any] = baseEndpoint
    .put
    .description("Update longitude and latitude of particular location")
    .in(idPath)
    .in(jsonBody[LocationUpdateOneRequest])
    .out(jsonBody[LocationResponse])

  val deleteEndpoint: PublicEndpoint[Location.Ids, StatusCode, StatusCode, Any] = baseEndpoint
    .delete
    .description("Delete list of given locations in batch")
    .in(idsQuery)
    .out(deleteSuccessStatusCodes)

  val deleteOneEndpoint: PublicEndpoint[Location.Id, StatusCode, StatusCode, Any] = baseEndpoint
    .delete
    .description("Delete particular location given by id")
    .in(idPath)
    .out(deleteSuccessStatusCodes)

  def statsEndpoint[F[_]]: PublicEndpoint[Period, StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .get
    .description("Get statistic about count of created locations per day." ++
                 " Statistic could be requested for all locations or created before or after or between dates")
    .in("-" / "stats")
    .in(periodQuery)
    .out(fs2StreamJsonBodyUTF8[F, List[LocationStats]])
  
  // TODO implement success / error status codes like this
  def deleteSuccessStatusCodes =
    oneOf[StatusCode](
      oneOfVariant(statusCode(StatusCode.Ok).and(emptyOutputAs(StatusCode.Ok).description("Not found so already removed"))),
      oneOfVariant(statusCode(StatusCode.NoContent).and(emptyOutputAs(StatusCode.NoContent).description("Deleted successfuly")))
    )
  
  val endpoints: List[AnyEndpoint] =
    List(
      createEndpoint,
      createOneEndpoint,
      getEndpoint,
      getOneEndpoint,
      updateEndpoint,
      updateOneEndpoint,
      deleteEndpoint,
      deleteOneEndpoint,
      statsEndpoint
    )

  // TODO move to another object and file
  def fs2StreamJsonBodyUTF8[F[_], T](using schema: Schema[T]) =
    streamBody(Fs2Streams[F])(schema, CodecFormat.Json(), Option(StandardCharsets.UTF_8))


// TODO move to another file
object StringStreamOps:
  extension [F[_]: Async, O] (stream: Stream[F, String])
    def catchError(errorMapper: Throwable => String): Stream[F, String] =
      stream
        .attempt
        .map {
          case Right(s) => s
          case Left(e) => errorMapper(e)
        }

    def toJsonArray: Stream[F, String] =
      val prefix = Stream.eval("[".pure[F])
      val suffix = Stream.eval("]".pure[F])
      prefix ++ stream.intersperse(",") ++ suffix

// TODO move to another file
import java.io.StringWriter
import java.io.PrintWriter
object ThrowableExtraOps:
  extension (error: Throwable)
    def printableStackTrace: String =
      val sw = new StringWriter
      error.printStackTrace(new PrintWriter(sw))
      sw.toString