import org.scalatest.GivenWhenThen
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalacheck._
import org.scalactic.Snapshots._
import scala.util.Random
import java.io.File

import com.dimafeng.testcontainers.DockerComposeContainer
import com.dimafeng.testcontainers.ExposedService
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.testcontainers.containers.wait.strategy.Wait
import com.dimafeng.testcontainers.ContainerDef
import com.dimafeng.testcontainers.Services
import com.dimafeng.testcontainers.Service
import com.dimafeng.testcontainers.WaitingForService

import java.time.LocalDateTime
import scala.math.BigDecimal

import doobie._
import doobie.implicits._
import doobie.implicits.javasql._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.postgres.pgisimplicits._
import cats._
import cats.implicits._
import cats.effect._
import cats.effect.implicits._
import cats.effect.unsafe.implicits.global

import doobie.scalatest.IOChecker


val isDebug = false


object DbEnv {
  val portBegin = 5000;
  val portEnd = 5999;
  val rnd = new Random;
  val port = portBegin + rnd.nextInt(portEnd - portBegin + 1)
  val path = if (isDebug) "/tmp/locations_db_fast_test"
             else s"/tmp/locations_db_testing/${Random.alphanumeric.take(10).mkString}"
  val waitTimes = (() =>
    var times = if (isDebug) 1 else 2

    () => 
      val ret = times
      times = 1
      ret
  )()

  val random =
    Map(
      "DB_ADMIN" -> s"user_${Random.alphanumeric.take(10).mkString}" ,
      "DB_PASSWORD" -> s"pswd_${Random.alphanumeric.take(10).mkString}",
      "DB_PATH" -> path,
      "DB_NAME" -> "locations",
      "DB_PORT" -> s"${port}"
    )

  val transactor =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      s"jdbc:postgresql://localhost:${port}/locations",
      "locator",
      "locator"
    )
}

case class Location(id: String, longitude: BigDecimal = 0, latitude: BigDecimal = 0, created: Option[LocalDateTime] = None) {
  def withoutDate = Location.WithoutDate(id, longitude, latitude)
}

object Location {
  case class WithoutDate(id: String, longitude: BigDecimal = 0, latitude: BigDecimal = 0)

  def selectAll() = select(sql.query.all)
  def selectAllWithoutDate() = select(sql.query.allWithoutDate)
  def select[T](query: doobie.Query0[T]) : List[T] =
    query
      .to[List]
      .transact(transactor)
      .unsafeRunSync()
  
  def insertWithDate = insert(sql.insert.withDate)
  def insertWithoutDate = insert(sql.insert.withoutDate)
  def insert[T](update: doobie.Update[T])(locations: Seq[T]) : Int =
    update
      .updateMany(locations)
      .transact(DbEnv.transactor)
      .unsafeRunSync()

  def deleteAll() = delete(sql.delete.all)
  def delete(update: doobie.Update0) : Int =
    update
      .run
      .transact(DbEnv.transactor)
      .unsafeRunSync()

  object sql {
    object query {
      val all = sql"SELECT location_id, location_longitude, location_latitude, location_created FROM locations"
          .query[Location]

      val allWithoutDate = sql"SELECT location_id, location_longitude, location_latitude FROM locations"
          .query[Location.WithoutDate]
    }

    object insert {
      val withDate = Update[Location](
        "INSERT INTO locations (location_id, location_longitude, location_latitude, location_created) values (?, ?, ?, ?)"
      )

      val withoutDate = Update[Location.WithoutDate](
        "INSERT INTO locations (location_id, location_longitude, location_latitude) values (?, ?, ?)"
      )
    }

    object delete {
      val all = sql"DELETE FROM locations"
      .update
    }
  }
  
  private val transactor = DbEnv.transactor
}

class LocationsSqlSpec extends AnyFlatSpec
                          with TestContainerForAll
                          with IOChecker {
  override val containerDef =
    DockerComposeContainer.Def(
      composeFiles = new File("./docker/compose.yaml"),
      env = DbEnv.random,
      services = Services.Specific(Seq(Service("db"))),
      waitingFor = Some(WaitingForService("db_1", Wait.forLogMessage(".*database system is ready to accept connections.*", DbEnv.waitTimes())))
    )

  val transactor = DbEnv.transactor

  info("As a developer I need to verify locations sql before to use it in tests")

  "Locations.sql.query.all" should "be valid" in {
      check(Location.sql.query.all)
  }

  "Locations.sql.query.allWithoutDate" should "be valid" in {
      check(Location.sql.query.allWithoutDate)
  }

  "Locations.sql.insert.withDate" should "be valid" in {
      check(Location.sql.insert.withDate)
  }

  "Locations.sql.insert.withoutDate" should "be valid" in {
      check(Location.sql.insert.withoutDate)
  }

  "Locations.sql.delete.all" should "be valid" in {
      check(Location.sql.delete.all)
  }
}

class LocationsTableSpec extends AnyFlatSpec
                          with TestContainerForAll
                          with IOChecker
                          with BeforeAndAfter
                          with GivenWhenThen {
  override val containerDef =
    DockerComposeContainer.Def(
      composeFiles = new File("./docker/compose.yaml"),
      env = DbEnv.random,
      services = Services.Specific(Seq(Service("db"))),
      waitingFor = Some(WaitingForService("db_1", Wait.forLogMessage(".*database system is ready to accept connections.*", DbEnv.waitTimes())))
    )

  val transactor = DbEnv.transactor

  after {
    info("--- cleanup")
    Location.deleteAll()
  }

  "TABLE locations" should "be empty by default" in {
    When("everything is selected from locations table")
      val result = Location.selectAll()

    Then("result is empty list")
      info(snap(result).lines)
      result shouldBe empty
  }

  "location_id field" should "accept alphanumeric values" in {
    val idLengthRange = 1 until 256

    Given(s"random alphanumeric strings with length from ${idLengthRange.start} to ${idLengthRange.end} generated")
      val locationIdGen = (n: Int) => Gen.listOfN(n, Gen.alphaNumChar).map(_.mkString)

      val locationGen  = Gen.sized( size => for {
          locationId <- locationIdGen(size)
        } yield Location(locationId)
      )

      val locations = idLengthRange.toList.map(size => Gen.resize(size, locationGen).sample.get.withoutDate)

    When("all of them used as location_id of entries")
      Location.insertWithoutDate(locations)

    Then("all entries are inserted into db successfully")
      val result = Location.selectAllWithoutDate()

      info(snap(result.length).lines)
      result should contain theSameElementsAs locations
  }

  it should "not accept zero length string" in {
    Given("location has id equal to empty string")
      val location = Location("").withoutDate
    
    When("attempt to insert such location is done")
    Then("error occurs")
      a [java.sql.BatchUpdateException] should be thrownBy Location.insertWithoutDate(Seq(location))

    And("nothing is inserted into db")
      val result = Location.selectAllWithoutDate()

      info(snap(result).lines)
      result shouldBe empty
  }

  it should "not accept string with length more than 255" in {
    Given("location has id with length 256")
      val location = Location("").withoutDate

    When("attempt to insert such location is done")
    Then("error occurs")
      a [java.sql.BatchUpdateException] should be thrownBy Location.insertWithoutDate(Seq(location))

    And("nothing is inserted into db")
      val result = Location.selectAllWithoutDate()

      info(snap(result).lines)
      result shouldBe empty
  }

  it should "not accept non-alphanumeric string" in {
    val locationId = "abc_#$%!@_123"

    Given(s"location has non-alphanumeric id like $locationId")
      val location = Location(locationId).withoutDate

    When("attempt to insert such location is done")
    Then("error occurs")
      a [java.sql.BatchUpdateException] should be thrownBy Location.insertWithoutDate(Seq(location))

    And("nothing is inserted into db")
      val result = Location.selectAllWithoutDate()

      info(snap(result).lines)
      result shouldBe empty
  }

  it should "not accept not a string" in {
    case class FakeLocation(id: Int, longitude: BigDecimal = 0, latitude: BigDecimal = 0)

    Given("location has id which is integer")
      val fakeLocation = FakeLocation(125)

    When("attempt to insert such location is done")
      val fakeWithoutDate = Update[FakeLocation](
        "INSERT INTO locations (location_id, location_longitude, location_latitude) values (?, ?, ?)"
      )

    Then("error occurs on sql check")
      a [org.scalatest.exceptions.TestFailedException] should be thrownBy check(fakeWithoutDate)
  }

  it should "not accept non-unique string if inserted next time" in {
    Given(s"location has a valid id")
      val location = Location("nonUniqueId").withoutDate
      val locations = Seq(location)

    And("first attempt to insert the location is done successfully")
      Location.insertWithoutDate(locations)
    
    When("second attempt to insert the location happens")
    Then("error occurs")
      a [java.sql.BatchUpdateException] should be thrownBy Location.insertWithoutDate(locations)

    And("just first location is inserted into db")
      val result = Location.selectAllWithoutDate()

      info(snap(result).lines)
      result should contain theSameElementsAs locations
  }

  it should "not accept non-unique string in batch mode" in {
    val locationId = "nonUniqueIdInBatch"

    Given(s"two locations have the same id like $locationId")
      val location1 = Location(locationId).withoutDate
      val location2 = Location(locationId).withoutDate

    When("attempt to insert such locations is done")
    Then("error occurs")
      a [java.sql.BatchUpdateException] should be thrownBy Location.insertWithoutDate(Seq(location1, location2))

    And("nothing is inserted into db")
      val result = Location.selectAllWithoutDate()

      info(snap(result).lines)
      result shouldBe empty
  }

  "location_longitude field" should "accept values in [-180, 180]" in {
    Given(s"longitudes as every integer in [-180, 180] are generated")
      val longitudeRange = -180 to 180

    When("all of them used as in locations with unique and valid ids")
      val locationIdGen = (n: Int) => Gen.listOfN(n, Gen.alphaNumChar).map(_.mkString)
      val locations = longitudeRange.toList.map(longitude => Location.WithoutDate(locationIdGen(20).sample.get, longitude))
      Location.insertWithoutDate(locations)

    Then("all entries are inserted into db successfully")
      val result = Location.selectAllWithoutDate()

      info(snap(result.length).lines)
      result should contain theSameElementsAs locations
  }

  it should "allow decimal values" in {
    val longitude: BigDecimal = 15.345

    Given(s"longitude is decimal value like $longitude")
    And("location with such longitude is created")
      val locations = Seq(Location.WithoutDate("decimalLongitude", longitude))
    
    When("attempt to insert such location happens")
      Location.insertWithoutDate(locations)

    Then("it is inserted into db successfully")
      val result = Location.selectAllWithoutDate()

      info(snap(result.length).lines)
      result should contain theSameElementsAs locations
  }

  it should "has 6 digits scale" in {
    lazy val increasingScale: LazyList[BigDecimal] = {
      def loop(x: BigDecimal): LazyList[BigDecimal] = x #:: loop(x.intValue + 1 + (x + 1) / 10 )
      loop(0)
    }

    Given("the following list of longitudes generated")
      lazy val longitudes = increasingScale.take(8)
      info("  " + snap(longitudes).lines)

    And("locations are generated for this list")
      val locations = longitudes.map(longitude => Location.WithoutDate(longitude.intValue.toString, longitude))

    When("attempt to insert such location happens")
      Location.insertWithoutDate(locations)

    Then("it is inserted into db successfully")
      val result = Location.selectAllWithoutDate()
      info(snap(result.length).lines)

      def scaleFilter(scaleRange: Range) =
        (location: Location.WithoutDate) => scaleRange.contains(location.id.toInt)

      result.filter(scaleFilter(0 to 6)) should contain theSameElementsAs locations.filter(scaleFilter(0 to 6))
      result.filter(scaleFilter(7 to Int.MaxValue)).length should be > 0

      val roundedLocations = locations.map(location =>
        Location.WithoutDate(location.id, location.longitude.setScale(6, BigDecimal.RoundingMode.HALF_EVEN)))

      result should contain theSameElementsAs roundedLocations
  }

  it should "not accept values less than -180" in {
    val badLongitude: BigDecimal = -180.01

    Given(s"location has longitude with value $badLongitude")
      val location = Location.WithoutDate("smallLongitude", badLongitude)
    
    When("such location is inserted to db")
    Then("error occurs")
      a [java.sql.BatchUpdateException] should be thrownBy Location.insertWithoutDate(Seq(location))

    And("nothing is inserted into db")
      val result = Location.selectAllWithoutDate()

      info(snap(result).lines)
      result shouldBe empty
  }

  it should "not accept values more than 180" in {
    val badLongitude: BigDecimal = 180.01

    Given(s"location has longitude with value $badLongitude")
      val location = Location.WithoutDate("bigLongitude", badLongitude)
    
    When("such location is inserted to db")
    Then("error occurs")
      a [java.sql.BatchUpdateException] should be thrownBy Location.insertWithoutDate(Seq(location))

    And("nothing is inserted into db")
      val result = Location.selectAllWithoutDate()

      info(snap(result).lines)
      result shouldBe empty
  }

  "location_latitude field" should "accept values in [-90, 90]" in {
    Given(s"longitudes as every integer in [-90, 90] are generated")
      val latitudeRange = -90 to 90

    When("all of them used as in locations with unique and valid ids")
      val locationIdGen = (n: Int) => Gen.listOfN(n, Gen.alphaNumChar).map(_.mkString)
      val locations = latitudeRange.toList.map(latitude => Location.WithoutDate(locationIdGen(20).sample.get, latitude = latitude))
      Location.insertWithoutDate(locations)

    Then("all entries are inserted into db successfully")
      val result = Location.selectAllWithoutDate()

      info(snap(result.length).lines)
      result should contain theSameElementsAs locations
  }

  it should "allow decimal values" in {
    val latitude: BigDecimal = -65.9342

    Given(s"latitude is decimal value like $latitude")
    And("location with such latitude is created")
      val locations = Seq(Location.WithoutDate("decimalLatitude", latitude = latitude))
    
    When("attempt to insert such location happens")
      Location.insertWithoutDate(locations)

    Then("it is inserted into db successfully")
      val result = Location.selectAllWithoutDate()

      info(snap(result.length).lines)
      result should contain theSameElementsAs locations
  }

  it should "has 6 digits scale" in {
    lazy val increasingScale: LazyList[BigDecimal] = {
      def loop(x: BigDecimal): LazyList[BigDecimal] = x #:: loop(x.intValue + 1 + (x + 1) / 10 )
      loop(0)
    }

    Given("the following list of latitudes generated")
      lazy val latitudes = increasingScale.take(8)
      info("  " + snap(latitudes).lines)

    And("locations are generated for this list")
      val locations = latitudes.map(latitude => Location.WithoutDate(latitude.intValue.toString, latitude = latitude))

    When("attempt to insert such location happens")
      Location.insertWithoutDate(locations)

    Then("it is inserted into db successfully")
      val result = Location.selectAllWithoutDate()
      info(snap(result.length).lines)

      def scaleFilter(scaleRange: Range) =
        (location: Location.WithoutDate) => scaleRange.contains(location.id.toInt)

      result.filter(scaleFilter(0 to 6)) should contain theSameElementsAs locations.filter(scaleFilter(0 to 6))
      result.filter(scaleFilter(7 to Int.MaxValue)).length should be > 0

      val roundedLocations = locations.map(location =>
        Location.WithoutDate(location.id, latitude = location.latitude.setScale(6, BigDecimal.RoundingMode.HALF_EVEN)))

      result should contain theSameElementsAs roundedLocations
  }

  it should "not accept values less than -90" in {
    val badLatitude: BigDecimal = -90.01

    Given(s"location has latitude with value $badLatitude")
      val location = Location.WithoutDate("smallLatitude", latitude = badLatitude)
    
    When("such location is inserted to db")
    Then("error occurs")
      a [java.sql.BatchUpdateException] should be thrownBy Location.insertWithoutDate(Seq(location))

    And("nothing is inserted into db")
      val result = Location.selectAllWithoutDate()

      info(snap(result).lines)
      result shouldBe empty
  }

  it should "not accept values more than 90" in {
    val badLatitude: BigDecimal = 90.01

    Given(s"location has latitude with value $badLatitude")
      val location = Location.WithoutDate("bigLatitude", latitude = badLatitude)
    
    When("such location is inserted to db")
    Then("error occurs")
      a [java.sql.BatchUpdateException] should be thrownBy Location.insertWithoutDate(Seq(location))

    And("nothing is inserted into db")
      val result = Location.selectAllWithoutDate()

      info(snap(result).lines)
      result shouldBe empty
  }

  "location_created field" should "be automatically set to current timestamp if not provided" in {
    Given("location does not have created date")
      val location = Location.WithoutDate("withoutDate")
    
    When("such location is inserted into db")
      val insertTime = LocalDateTime.now()
      Location.insertWithoutDate(Seq(location))
    
    Then("location_created automatically generated by db")
      val result = Location.selectAll()
      info(snap(result).lines)

      result should have length 1
      val Location(_, _, _, created) = result.head

      created should not be empty

    And("its value is between insertion time and now")
      val now = LocalDateTime.now() 
      created.get.isAfter(insertTime) shouldBe true
      created.get.isBefore(now) shouldBe true
  }

  it should "allow to add manual timestamp" in {
    val expectedCreated = Some(LocalDateTime.of(2022, 9, 12, 17, 10, 33))

    Given(s"location has location_created as $expectedCreated")
      val location = Location("withDate", created = expectedCreated)
    
    When("such location is inserted into db")
      val insertTime = LocalDateTime.now()
      Location.insertWithDate(Seq(location))
    
    Then("location_created automatically generated by db")
      val result = Location.selectAll()
      info(snap(result).lines)

      result should have length 1
      val Location(_, _, _, created) = result.head

      created shouldBe expectedCreated
  }
}