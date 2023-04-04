package com.vportnov.locations.api.types.response

import sttp.tapir._
import sttp.model.StatusCode


object Create:
  def output[F[_]] = statusCode(StatusCode.Created).and(Location.body.stream[F].toEndpointIO)
  
  def error =
    oneOf[Status](
      oneOfVariant(Status.BadRequest.asStatusCodeWithJsonBody),
      oneOfDefaultVariant(Status.InternalServerError.asStatusCodeWithJsonBody)
    )
