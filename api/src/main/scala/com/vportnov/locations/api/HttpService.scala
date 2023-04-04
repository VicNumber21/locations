package com.vportnov.locations.api

import cats.effect._
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.HttpApp
import org.http4s.server.Router

import sttp.tapir.server.model.{ ValuedEndpointOutput, ServerResponse }
import sttp.tapir.server.http4s.{ Http4sServerInterpreter, Http4sServerOptions }
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler
import sttp.tapir.server.interceptor.exception.{ ExceptionHandler, ExceptionContext }
import sttp.tapir.server.interceptor.log.DefaultServerLog

import com.vportnov.locations.api.HttpEndpoints
import com.vportnov.locations.model.StorageExt
import com.vportnov.locations.api.types.{ request, response }
import com.vportnov.locations.utils. { LoggingIO, ServerError }


final class HttpService[F[_]: Async](storage: StorageExt[F]) extends LoggingIO[F]:
  def app: HttpApp[F] = Router("/" -> (routes)).orNotFound

  private def badRequestResponse(message: String): ValuedEndpointOutput[response.Status.BadRequest] =
    ValuedEndpointOutput(response.Status.BadRequest.asJsonBody, response.Status.BadRequest(message))

  private def internalServerErrorResponse(message: String): ValuedEndpointOutput[response.Status.InternalServerError] =
    ValuedEndpointOutput(response.Status.InternalServerError.asStatusCodeWithJsonBody, response.Status.InternalServerError(message))
  
  private def showResponse(reply: ServerResponse[_]): String = reply match
    case ServerResponse(code, _, _, Some(ValuedEndpointOutput(_, (_, error: response.Status.Error)))) =>
      s"${code}, uuid = ${error.uuid}"
    case _ =>
      reply.showShort
  
  private def serverLogger: DefaultServerLog[F] = DefaultServerLog(
    doLogWhenReceived = log.info(_),
    doLogWhenHandled = (msg: String, error: Option[Throwable]) => if error.isEmpty then log.info(msg) else log.info(error.get)(msg),
    doLogAllDecodeFailures = (msg: String, error: Option[Throwable]) => if error.isEmpty then log.warn(msg) else log.warn(error.get)(msg),
    doLogExceptions = (msg: String, error: Throwable) => log.error(error)(msg),
    noLog = cats.effect.Sync[F].pure(()),
    showResponse = showResponse,
    logWhenReceived = true,
    logWhenHandled = true,
    logAllDecodeFailures = false,
    logLogicExceptions = false
  )
  
  private def exceptionHandler: ExceptionHandler[F] =
    new ExceptionHandler[F]:
      override def apply(ctx: ExceptionContext)(implicit monad: sttp.monad.MonadError[F]): F[Option[ValuedEndpointOutput[_]]] =
        val error = ServerError.fromCause(ctx.e)
        val errorResponse = response.Status.InternalServerError(error.message, error.uuid)
        log.error(error)(s"Exception when handling request: ${ctx.request.showShort}, by ${ctx.endpoint.showShort}")
          .flatMap { (x: Unit) =>
            Some(ValuedEndpointOutput(response.Status.InternalServerError.asStatusCodeWithJsonBody, errorResponse)).pure[F]
          }

  private def options: Http4sServerOptions[F] = Http4sServerOptions
    .customiseInterceptors
    .decodeFailureHandler(DefaultDecodeFailureHandler.default.response(badRequestResponse))
    .exceptionHandler(exceptionHandler)
    .serverLog(serverLogger)
    .options

  private def version = getClass.getPackage.getImplementationVersion
  private def name = "Locations Service"
  private def endpoints = new HttpEndpoints(storage)

  private def routes: HttpRoutes[F] = 
    Http4sServerInterpreter[F](options).toRoutes(
      endpoints.serverEndpoints ++
      endpoints.swaggerEndpoints(name, version)
    )
