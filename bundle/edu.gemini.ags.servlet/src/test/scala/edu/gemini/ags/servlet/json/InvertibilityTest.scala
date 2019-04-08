package edu.gemini.ags.servlet.json

import argonaut._, Argonaut._
import org.scalacheck.Arbitrary
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

trait InvertibilityTest extends Specification with ScalaCheck {

  def testInvertibility[A: EncodeJson: DecodeJson: Arbitrary] =
    prop { (a: A) =>
      Parse.decodeOption[A](a.asJson.spaces2) should_== Some(a)
    }

}
