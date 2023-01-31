import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import sbt.Keys._

object Settings {
  lazy val common = Seq(
    organization := "com.vportnov",
    version := "1.0",
    scalaVersion := "3.2.1",
    scalacOptions := Seq(
      "-encoding", "utf8",
      "-feature",
      "-deprecation",
      "-unchecked",
      "-Werror",
      "-Xlint"
    )
  )
}