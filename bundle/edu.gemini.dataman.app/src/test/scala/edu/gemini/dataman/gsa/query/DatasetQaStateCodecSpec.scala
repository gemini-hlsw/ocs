package edu.gemini.dataman.gsa.query

import edu.gemini.dataman.gsa.query.JsonCodecs._
import edu.gemini.spModel.dataset.DatasetQaState
import edu.gemini.spModel.dataset.DatasetQaState._

import argonaut._
import Argonaut._

import org.specs2.mutable.Specification

import scalaz._

object DatasetQaStateCodecSpec extends Specification {

  "DatasetQaState encode" should {
    "produce a json string based on the display value" in {
      PASS.asJson must_== jString("Pass")
    }
  }

  "DatasetQaState decode" should {
    "work for valid DatasetQaState display values" in {
      Parse.decodeEither[DatasetQaState](s""""${PASS.displayValue}"""") must_== \/-(PASS)
    }

    "fail for invalid qa states" in {
      Parse.decodeEither[DatasetQaState](s""""PASS"""") match {
        case -\/(m) => m.startsWith(invalidDatasetQaState("PASS"))
        case _      => failure("expected to fail on input `PASS`")
      }
    }
  }
}
