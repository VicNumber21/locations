package com.vportnov.locations.api

import cats.effect.Async
import cats.syntax.all._

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes

import sttp.tapir._
import sttp.tapir.server.http4s.{ Http4sServerInterpreter, Http4sServerOptions }
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler
import sttp.tapir.server.interceptor.exception.{ExceptionHandler, ExceptionContext}
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.model.StatusCode
import io.circe.syntax._

import sttp.capabilities.fs2.Fs2Streams
import fs2.Stream

import com.vportnov.locations.model.StorageExt
import com.vportnov.locations.api.types.{ request, response }
import com.vportnov.locations.utils.fs2stream.syntax._
import com.vportnov.locations.utils. { LoggingIO, ServerError }


final class LocationsRoutes[F[_]: Async](storage: StorageExt[F]) extends Http4sDsl[F] with LoggingIO[F]:
  private def badRequestResponse(message: String): ValuedEndpointOutput[response.Status.BadRequest] =
    ValuedEndpointOutput(response.Status.BadRequest.asJsonBody, response.Status.BadRequest(message))

  private def internalServerErrorResponse(message: String): ValuedEndpointOutput[response.Status.InternalServerError] =
    ValuedEndpointOutput(response.Status.InternalServerError.asStatusCodeWithJsonBody, response.Status.InternalServerError(message))
  
  val serverLogger: DefaultServerLog[F] = DefaultServerLog(
    doLogWhenReceived = log.info(_),
    doLogWhenHandled = (msg: String, error: Option[Throwable]) => if error.isEmpty then log.info(msg) else log.info(error.get)(msg),
    doLogAllDecodeFailures = (msg: String, error: Option[Throwable]) => if error.isEmpty then log.warn(msg) else log.warn(error.get)(msg),
    doLogExceptions = (msg: String, error: Throwable) => log.error(error)(msg),
    noLog = cats.effect.Sync[F].pure(()),
    logWhenReceived = true,
    logWhenHandled = true,
    logAllDecodeFailures = false,
    logLogicExceptions = false
  )
  
  val exceptionHandler: ExceptionHandler[F] =
    new ExceptionHandler[F]:
      override def apply(ctx: ExceptionContext)(implicit monad: sttp.monad.MonadError[F]): F[Option[ValuedEndpointOutput[_]]] =
        val error = ServerError.fromCause(ctx.e)
        val errorResponse = response.Status.InternalServerError(error.message, error.uuid)
        log.error(error)(s"Exception when handling request: ${ctx.request.showShort}, by ${ctx.endpoint.showShort}")
          .flatMap { (x: Unit) =>
            Some(ValuedEndpointOutput(response.Status.InternalServerError.asStatusCodeWithJsonBody, errorResponse)).pure[F]
          }

  // TODO rework as the following:
  // LocationRoutes should be renamed into Endpoints
  // they should return List of ServerEndpoints (including swagger ones)
  // it should be a single Http4sServerInterpreter in Service file which creates al routes from the list 
  // options should be moved there probably
  val options: Http4sServerOptions[F] = Http4sServerOptions
    .customiseInterceptors
    .decodeFailureHandler(DefaultDecodeFailureHandler.default.response(badRequestResponse))
    .exceptionHandler(exceptionHandler)
    .serverLog(serverLogger)
    .options

  val createRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F](options).toRoutes(
      LocationsRoutes.createEndpoint
        .serverLogicSuccess(request => reply(storage.createLocations(request.toModel), response.Location.from, conflictError))
    )
  
  val createOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F](options).toRoutes(
      LocationsRoutes.createOneEndpoint
        .serverLogic(request => reply(storage.createLocation(request.toModel), response.Location.from, conflictError))
    )

  val getRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F](options).toRoutes(
      LocationsRoutes.getEndpoint
        .serverLogicSuccess(request => reply(storage.getLocations(request.period.toModel, request.ids.v), response.Location.from, notFoundError))
    )

  val getOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F](options).toRoutes(
      LocationsRoutes.getOneEndpoint
        .serverLogic(request => reply(storage.getLocation(request.v), response.Location.from, notFoundError))
    )

  val updateRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F](options).toRoutes(
      LocationsRoutes.updateEndpoint
        .serverLogicSuccess(request => reply(storage.updateLocations(request.toModel), response.Location.from, notFoundError))
    )
  
  val updateOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F](options).toRoutes(
      LocationsRoutes.updateOneEndpoint
        .serverLogic(request => reply(storage.updateLocation(request.toModel), response.Location.from, notFoundError))
    )

  val deleteRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F](options).toRoutes(
      LocationsRoutes.deleteEndpoint
        .serverLogic(request => newReply(storage.deleteLocations(request.v), deleteSuccess, commonErrors))
    )

  val deleteOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F](options).toRoutes(
      LocationsRoutes.deleteOneEndpoint
        .serverLogic(request => newReply(storage.deleteLocation(request.v), deleteSuccess, commonErrors))
    )
  
  val statsRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F](options).toRoutes(
      LocationsRoutes.statsEndpoint
        .serverLogicSuccess(request => reply(storage.locationStats(request.toModel), response.Stats.from, commonErrors))
    )
  
  private def reply[SR, O](stream: Stream[F, SR], mapper: SR => O, errorMapper: Throwable => response.Status)
                          (using encoder: io.circe.Encoder[O]): F[Stream[F, Byte]] =
    stream
      .map(mapper)
      .map(_.asJson.noSpaces)
      .logWhenDone
      .recover { error =>
        errorMapper(error).toJson.noSpaces
      }
      .toJsonArray
      .through(fs2.text.utf8.encode)
      .pure[F]
      .logWhenDone

  private def reply[SR, O](storageResponse: F[SR], mapper: SR => O, errorMapper: Throwable => response.Status): F[Either[StatusCode, O]] =
    val result = for
      unpacked <- storageResponse
        .map(mapper)
        .logWhenDone
        .attempt
    yield unpacked.left.map(errorMapper.andThen(_.toStatusCode))
    result.logWhenDone

  // TODO rename to reply when prev reply is removed
  private def newReply[SR, O](storageResponse: F[SR], mapper: SR => O, errorMapper: Throwable => response.Status): F[Either[response.Status, O]] =
    val result = for
      unpacked <- storageResponse
        .map(mapper)
        .logWhenDone
        .attempt
    yield unpacked.left.map(errorMapper)
    result.logWhenDone

  private def deleteSuccess(count: Int): response.Delete = count match
    case 0 => response.Status.Ok()
    case n if n > 0 => response.Status.NoContent()
    case strange => throw ServerError.Internal(s"Count could not be less than 0 (got ${strange})")
  
  private def notFoundError(error: Throwable) =
    val se = ServerError.fromCause(error)
    se.kind match
      case ServerError.Kind.NoSuchElement => response.Status.NotFound(se.message, se.uuid)
      case _ => commonErrors(error)

  private def conflictError(error: Throwable) =
    val se = ServerError.fromCause(error)
    se.kind match
      case ServerError.Kind.NoSuchElement => response.Status.Conflict(se.message, se.uuid)
      case _ => commonErrors(error)

  private def commonErrors(error: Throwable) =
    val se = ServerError.fromCause(error)
    se.kind match
      case ServerError.Kind.IllegalArgument => response.Status.BadRequest(se.message, se.uuid)
      case _ => response.Status.InternalServerError(se.message, se.uuid)
  
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
  val baseEndpoint = endpoint
    .in("api" / "v1.0" / "locations")

  def createEndpoint[F[_]]: PublicEndpoint[request.Create, StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .post
    .description("Create locations in batch.")
    .tag("Create")
    .in(request.Create.input)
    .errorOut(statusCode) // TODO add json error as Status
    .out(response.Location.body.stream)
    .out(statusCode(StatusCode.Created))

  val createOneEndpoint: PublicEndpoint[request.CreateOne, StatusCode, response.Location, Any] = baseEndpoint
    .post
    .description("Create a single location.")
    .tag("Create")
    .in(request.CreateOne.input)
    .errorOut(statusCode)
    .out(response.Location.body.json)
    .out(statusCode(StatusCode.Created))

  def getEndpoint[F[_]]: PublicEndpoint[request.Get, StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .get
    .description("Get list of locations: all, particular ids or created before or after or between dates.")
    .tag("Get")
    .in(request.Get.input)
    .errorOut(statusCode)
    .out(response.Location.body.stream)

  val getOneEndpoint: PublicEndpoint[request.GetOne, StatusCode, response.Location, Any] = baseEndpoint
    .get
    .description("Get particular location by given id.")
    .tag("Get")
    .in(request.GetOne.input)
    .errorOut(statusCode)
    .out(response.Location.body.json)

  def updateEndpoint[F[_]]: PublicEndpoint[request.Update, StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .put
    .description("Update longitude and latitude of given location in batch.")
    .tag("Update")
    .in(request.Update.input)
    .errorOut(statusCode)
    .out(response.Location.body.stream)

  val updateOneEndpoint: PublicEndpoint[request.UpdateOne, StatusCode, response.Location, Any] = baseEndpoint
    .put
    .description("Update longitude and latitude of particular location.")
    .tag("Update")
    .in(request.UpdateOne.input)
    .errorOut(statusCode)
    .out(response.Location.body.json)

  val deleteEndpoint: PublicEndpoint[request.Delete, response.Status, response.Delete, Any] = baseEndpoint
    .delete
    .description("Delete list of given locations in batch.")
    .tag("Delete")
    .in(request.Delete.input)
    .errorOut(response.Delete.error)
    .out(response.Delete.output)

  val deleteOneEndpoint: PublicEndpoint[request.DeleteOne, response.Status, response.Delete, Any] = baseEndpoint
    .delete
    .description("Delete particular location by given id.")
    .tag("Delete")
    .in(request.DeleteOne.input)
    .errorOut(response.Delete.error)
    .out(response.Delete.output)

  def statsEndpoint[F[_]]: PublicEndpoint[request.Stats, StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .get
    .description("Get statistic about count of created locations per day." ++
                 " Statistic could be requested for all locations or created before or after or between dates.")
    .tag("Statistics")
    .in("-" / "stats")
    .in(request.Stats.input)
    .errorOut(statusCode)
    .out(response.Stats.body.stream)
  
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
