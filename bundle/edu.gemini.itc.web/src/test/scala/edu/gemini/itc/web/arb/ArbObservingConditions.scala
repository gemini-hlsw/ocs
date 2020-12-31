package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import edu.gemini.spModel.core._
import org.scalacheck._
import org.scalacheck.Arbitrary._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality._

trait ArbObservingConditions {
  import core._

  val genObservingConditions: Gen[ObservingConditions] =
    for {
      iq <- arbitrary[ImageQuality]
      cc <- arbitrary[CloudCover]
      wv <- arbitrary[WaterVapor]
      sb <- arbitrary[SkyBackground]
      am <- arbitrary[Double]       // should be in the range 1.0 - 3.0
      exactiq <- arbitrary[Double]  // should be in the range 0.05 - 5.0
      exactcc <- arbitrary[Double]  // should be in the range 0.0 - 5.0
    } yield ObservingConditions(iq, cc, wv, sb, am, exactiq, exactcc)

  implicit val arbObservingConditions: Arbitrary[ObservingConditions] =
    Arbitrary(genObservingConditions)

}

object observingconditions extends ArbObservingConditions
