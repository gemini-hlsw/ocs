package edu.gemini.gsa.query

import argonaut.Argonaut._
import argonaut._
import edu.gemini.gsa.query.JsonCodecs._
import org.specs2.mutable.Specification

import java.time.Instant

import scalaz._

object TimeInstanceCodecSpec extends Specification {

  val ExampleInstant = Instant.parse("2011-12-03T10:15:30Z")

  "Time instance encode" should {
    "produce a formatted json time string" in {
      ExampleInstant.asJson must_== jString(TimeFormat.format(ExampleInstant))
    }
  }

  "Time instance decode" should {
    "work for valid time strings" in {
      Parse.decodeEither[Instant](""""2011-12-03 10:15:30.000000+00:00"""") must_== \/-(ExampleInstant)
    }

    "handle zone offsets" in {
      Parse.decodeEither[Instant](""""2011-12-03 08:15:30.000000-02:00"""") must_== \/-(ExampleInstant)
    }

    "fail for invalid time strings" in {
      Parse.decodeEither[Instant](""""2011-12-03 10:15:30"""") match {
        case -\/(m) => m.startsWith(invalidTimeInstance("2011-12-03 10:15:30"))
        case _      => failure("expected to fail on input `2011-12-03 10:15:30`")
      }
    }
  }
}
