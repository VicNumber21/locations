package com.vportnov.locations.model

import fs2.Stream


trait Storage[F[_]]:
  type LocationStream[F[_]] = Stream[F, Location.WithCreatedField]
  type LocationStatsStream[F[_]] = Stream[F, Location.Stats]

  def createLocations(locations: List[Location.WithOptionalCreatedField]): LocationStream[F]

  def getLocations(period: Period, ids: Location.Ids): LocationStream[F]

  def updateLocations(locations: List[Location.WithoutCreatedField]): LocationStream[F]

  def deleteLocations(ids: Location.Ids): F[Either[Throwable, Int]]
  
  def locationStats(period: Period): LocationStatsStream[F]