package com.vportnov.locations.api.types.response

import sttp.tapir._
import sttp.model.StatusCode


object CreateOne:
  def output = statusCode(StatusCode.Created).and(Location.body.json)
  
  def error =
    oneOf[Status](
      oneOfVariant(Status.BadRequest.asStatusCodeWithJsonBody),
      oneOfVariant(Status.Conflict.asStatusCodeWithJsonBody),
      oneOfDefaultVariant(Status.InternalServerError.asStatusCodeWithJsonBody)
    )
