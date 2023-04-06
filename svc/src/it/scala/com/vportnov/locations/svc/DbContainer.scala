package com.vportnov.locations.svc

import com.dimafeng.testcontainers._
import org.testcontainers.containers.wait.strategy.Wait

import scala.util.Random
import java.io.File


object DbContainer:
  def register: Unit = count += 1
  def isRunning: Boolean = container.isDefined
  def config: Config.Database = getConfig.db

  def onSuiteBegin(isFastRun: Boolean): Unit =
    generateConfig(isFastRun)
    if count > 0 then startContainer()

  def onSuiteEnd(isFastRun: Boolean): Unit =
    count -= 1
    if count <= 0 then stopContainer()


  private def dbEnv: Map[String, String] = Map(
      "DB_ADMIN" -> config.admin.login,
      "DB_ADMIN_PASSWORD" -> config.admin.password,
      "DB_VOLUME" -> getConfig.volume,
      "DB_NAME" -> "locations",
      "DB_PORT" -> s"${getConfig.port}"
    )

  private def containerDef =
    DockerComposeContainer.Def(
      composeFiles = new File("./docker/compose.yaml"),
      env = dbEnv,
      services = Services.Specific(Seq(Service("db"))),
      waitingFor = Some(WaitingForService("db_1", Wait.forLogMessage(".*database system is ready to accept connections.*", getConfig.waitTimes)))
    )

  private final class DbConfig(isFastRun: Boolean):
    val voumeBasePath = "/tmp/locations_db_integration_test"
    val volume =
      if isFastRun
        then s"${voumeBasePath}/permanent"
        else s"${voumeBasePath}/${Random.alphanumeric.take(10).mkString}"
    val port = 65432
    val waitTimes = if File(volume).exists() then 1 else 2
    val db = Config.Database(
      driver = "org.postgresql.Driver",
      url = s"jdbc:postgresql://localhost:${port}",
      name = "locations",
      user = Config.Credentials("locator", "locator"),
      admin = Config.Credentials("admin", "admin")
    )

  private def generateConfig(isFastRun: Boolean): Unit =
    if (dbConfig.isEmpty) then dbConfig = Some(new DbConfig(isFastRun))
  
  private def getConfig: DbConfig = dbConfig.get

  private def startContainer(): Unit =
    if !isRunning then container = Some(containerDef.start())
  
  private def stopContainer(): Unit =
    if isRunning then
      container.get.stop()
      container.get.close()

  @volatile private var dbConfig: Option[DbConfig] = None;
  @volatile private var container: Option[Container] = None;
  @volatile private var count: Int = 0;
