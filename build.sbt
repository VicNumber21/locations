lazy val api = Project("api", file("api"))
  .settings(
    name := "locations-api"
  )
  .settings(Settings.common: _*)
  .enablePlugins(JavaAppPackaging, DockerPlugin)

lazy val svc = Project("svc", file("svc"))
  .settings(
    name := "locations-svc"
  )
  .settings(Settings.common: _*)
  .enablePlugins(JavaAppPackaging, DockerPlugin)

lazy val db = Project("db", file("db"))
  .settings(
    name := "locations-deb"
  )
  .enablePlugins(DockerPlugin)

lazy val locations = Project("locations", file("."))
  .aggregate(api, svc, db)