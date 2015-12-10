package edu.gemini.gsa.query

import argonaut.Argonaut._
import argonaut._
import edu.gemini.gsa.query.JsonCodecs._
import edu.gemini.spModel.dataset.DatasetQaState
import edu.gemini.spModel.dataset.DatasetQaState._
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

    "ignore case when parsing" in {
      Parse.decodeEither[DatasetQaState](s""""pAsS"""") must_== \/-(PASS)
    }

    "fail for invalid qa states" in {
      Parse.decodeEither[DatasetQaState](s""""FOO"""") match {
        case -\/(m) => m.startsWith(invalidDatasetQaState("FOO"))
        case _      => sys.error("expected to fail on input `FOO`")
      }
    }
  }
}
