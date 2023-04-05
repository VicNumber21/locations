package com.vportnov.locations.api.types.response

import sttp.tapir._


object CreateOne:
  def output = Status.Created.asStatusCodeWithLocationJson(meta.description.Created)
  
  
  def error =
    oneOf[Status](
      oneOfVariant(Status.BadRequest.asStatusCodeWithJsonBody),
      oneOfVariant(Status.Conflict.asStatusCodeWithJsonBody),
      oneOfDefaultVariant(Status.InternalServerError.asStatusCodeWithJsonBody)
    )

  object meta:
    object description:
      val Created = "Successful response with created location."
