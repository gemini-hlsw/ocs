package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._
import edu.gemini.itc.web.arb
import java.awt.Color
import org.scalacheck.Arbitrary
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

object ItcResultCodecSpec extends Specification with ScalaCheck {

  // Codecs
  import color._
  import disjunction._
  import itcccd._
  import itcerror._
  import itcresult._

  // Arbitraries
  import arb.color._
  import arb.disjunction._
  import arb.itcccd._
  import arb.itcresult._
  import arb.itcerror._

  def testInvertibility[A: EncodeJson: DecodeJson: Arbitrary] =
    prop { (t: A) =>
      Parse.decodeOption[A](t.asJson.spaces2) should_== Some(t)
    }

  "ItcResultCodecSpec" >> {
    "Color"             ! testInvertibility[Color]
    "ItcCcd"            ! testInvertibility[ItcCcd]
    "ItcResult"         ! testInvertibility[ItcResult]
    "ItcService.Result" ! testInvertibility[ItcService.Result]
  }

}
