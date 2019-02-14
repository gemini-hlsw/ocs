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
      am <- arbitrary[Double]
    } yield ObservingConditions(iq, cc, wv, sb, am)

  implicit val arbObservingConditions: Arbitrary[ObservingConditions] =
    Arbitrary(genObservingConditions)

}

object observingconditions extends ArbObservingConditions