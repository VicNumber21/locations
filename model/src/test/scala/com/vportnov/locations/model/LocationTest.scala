package com.vportnov.locations.model

import org.scalatest.matchers.should.Matchers._
import org.scalatest.GivenWhenThen
import org.scalatest.flatspec.AnyFlatSpec

import java.time.LocalDateTime


class LocationTest extends AnyFlatSpec with GivenWhenThen:
  info("As a developer I need Location class to store locations in different formats")

  "Location" should "provide ability to have mandatory 'created' field" in {
    Given("Location is created with mandatory 'created' field")
      val location = Location("location123", 0, 0, LocalDateTime.now())
    
    Then("its type is Location.Base")
      location shouldBe a [Location.Base]

    And("its subtype is Location.WithCreatedField")
      location shouldBe a [Location.WithCreatedField]
  }

  it should "provide ability to have optional 'created' field" in {
    Given("Location is created with optional 'created' field")
      val location = Location("location123", 0, 0, Some(LocalDateTime.now()))
    
    Then("its type is Location.Base")
      location shouldBe a [Location.Base]

    And("its subtype is Location.WithOptionalCreatedField")
      location shouldBe a [Location.WithOptionalCreatedField]
  }

  it should "provide ability to have no 'created' field" in {
    Given("Location is created without 'created' field")
      val location = Location("location123", 0, 0)
    
    Then("its type is Location.Base")
      location shouldBe a [Location.Base]

    And("its subtype is Location.WithoutCreatedField")
      location shouldBe a [Location.WithoutCreatedField]
  }
