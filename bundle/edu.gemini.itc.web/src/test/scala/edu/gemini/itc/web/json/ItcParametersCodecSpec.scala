package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._
import edu.gemini.itc.web.arb
import org.scalacheck.Arbitrary
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

object ItcParametersCodecSpec extends Specification with ScalaCheck {

  // Codecs
  import instrumentdetails._
  import observingconditions._
  import observationdetails._
  import sourcedefinition._
  import telescopedetails._
  import itcparameters._

  // Arbitraries
  import arb.instrumentdetails._
  import arb.observingconditions._
  import arb.observationdetails._
  import arb.sourcedefinition._
  import arb.telescopedetails._
  import arb.itcparameters._

  def testInvertibility[A: EncodeJson: DecodeJson: Arbitrary] =
    prop { (t: InstrumentDetails) =>
      Parse.decodeOption[InstrumentDetails](t.asJson.spaces2) should_== Some(t)
    }

  "ItcParametersCodecSpec" >> {
    "InstrumentDetails"   ! testInvertibility[InstrumentDetails]
    "ObservingConditions" ! testInvertibility[ObservingConditions]
    "ObservationDetails"  ! testInvertibility[ObservationDetails]
    "SourceDefinition"    ! testInvertibility[SourceDefinition]
    "TelescopeDetails"    ! testInvertibility[TelescopeDetails]
    "ItcParameters"       ! testInvertibility[ItcParameters]
  }

}
