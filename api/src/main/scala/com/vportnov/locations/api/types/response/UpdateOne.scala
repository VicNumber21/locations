package com.vportnov.locations.api.types.response

import sttp.tapir._


object UpdateOne:
  def output = Status.Ok.asStatusCodeWithLocationJson(meta.description.Ok)
  
  def error =
    oneOf[Status](
      oneOfVariant(Status.BadRequest.asStatusCodeWithJsonBody),
      oneOfVariant(Status.NotFound.asStatusCodeWithJsonBody),
      oneOfDefaultVariant(Status.InternalServerError.asStatusCodeWithJsonBody)
    )

  object meta:
    object description:
      val Ok = "Successful response with updated location."
