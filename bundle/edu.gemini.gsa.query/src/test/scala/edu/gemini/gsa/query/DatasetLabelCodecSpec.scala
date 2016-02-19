package edu.gemini.gsa.query

import argonaut.Argonaut._
import argonaut._
import edu.gemini.gsa.query.JsonCodecs._
import edu.gemini.spModel.dataset.DatasetLabel
import org.specs2.mutable.Specification

import scalaz._

object DatasetLabelCodecSpec extends Specification {

  val ExampleLabel = new DatasetLabel("GS-2015B-Q-1-2-3")

  "DatasetLabel encode" should {
    "produce a json string" in {
      ExampleLabel.asJson must_== jString("GS-2015B-Q-1-2-003")
    }
  }

  "DatasetLabel decode" should {
    "work for valid dataset labels" in {
      Parse.decodeEither[DatasetLabel](s""""${ExampleLabel.toString}"""") must_== \/-(ExampleLabel)
    }

    "fail for invalid labels" in {
      Parse.decodeEither[DatasetLabel](s""""foo"""") match {
        case -\/(m) => m.startsWith(invalidDatasetLabel("foo"))
        case _      => sys.error("expected to fail on input `foo`")
      }
    }
  }
}
