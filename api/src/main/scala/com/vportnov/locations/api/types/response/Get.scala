package com.vportnov.locations.api.types.response

import sttp.tapir._


object Get:
  def output[F[_]] = Status.Ok.asStatusCodeWithLocationStream[F](meta.description.Ok)
  
  def error =
    oneOf[Status](
      oneOfVariant(Status.BadRequest.asStatusCodeWithJsonBody),
      oneOfDefaultVariant(Status.InternalServerError.asStatusCodeWithJsonBody)
    )
  
  object meta:
    object description:
      val Ok = """|Successful response means that valid input was provided.
                  |
                  |A stream of locations is established which eventually transformed into array of locations.
                  |
                  |Due to streaming nature of response, it may be followed by error detected after stream is established.
                  |
                  |In such case error is returned as element of the array (see bodies for error cases as examples).
                  |
                  |If some locations requested by given ids are not sent back in response, it should be treated as NotFound. 
                  |""".stripMargin
