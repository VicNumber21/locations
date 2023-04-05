package com.vportnov.locations.api.types.response

import sttp.tapir._


object Create:
  def output[F[_]] = Status.Created.asStatusCodeWithLocationStream[F](meta.description.Created)
  
  def error =
    oneOf[Status](
      oneOfVariant(Status.BadRequest.asStatusCodeWithJsonBody),
      oneOfDefaultVariant(Status.InternalServerError.asStatusCodeWithJsonBody)
    )

  object meta:
    object description:
      val Created = """|Successful response means that valid input was provided.
                  |
                  |A stream of created locations is established which eventually transformed into array of locations.
                  |
                  |Due to streaming nature of response, it may be followed by error detected after stream is established.
                  |
                  |In such case error is returned as element of the array (see bodies for error cases as examples).
                  |
                  |If some requested locations are not sent back in response, it should be treated as Conflict
                  |(meaning that location with such id already exists).
                  |""".stripMargin
