package com.vportnov.locations.model

import org.scalatest.matchers.should.Matchers._
import org.scalatest.GivenWhenThen
import org.scalatest.flatspec.AnyFlatSpec

import java.time.LocalDateTime


class PeriodTest extends AnyFlatSpec with GivenWhenThen:
  info("As a developer I need Period class to store timestamps")

  "Period" should "be empty if neither 'from' nor 'to' are set" in {
    Given("neither 'from' nor 'to' are set during creation")
      val period = Period(None, None)
    
    Then("period is empty")
      period shouldBe empty
  }

  it should "not be empty if 'from' is set" in {
    Given("'from' is set during creation")
      val period = Period(Some(LocalDateTime.now()), None)
    
    Then("period is not empty")
      period should not be empty
  }

  it should "not be empty if 'to' is set" in {
    Given("'to' is set during creation")
      val period = Period(None, Some(LocalDateTime.now()))
    
    Then("period is not empty")
      period should not be empty
  }

  it should "not be empty if both 'from' and 'to' are set" in {
    Given("both 'from' and 'to' are set during creation")
      val period = Period(Some(LocalDateTime.now()), Some(LocalDateTime.now()))
    
    Then("period is not empty")
      period should not be empty
  }
