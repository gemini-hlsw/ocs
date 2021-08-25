package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality._

import org.scalacheck._
import org.scalacheck.Arbitrary._
import scalaz._
import Scalaz._

trait ArbObservingConditions {

  implicit val arbExactIq: Arbitrary[ExactIq] =
    Arbitrary {
      Gen.chooseNum(0.05, 5.0).map(ExactIq.unsafeFromArcsec)
    }

  implicit val arbExactCc: Arbitrary[ExactCc] =
    Arbitrary {
      Gen.chooseNum(0.0, 5.0).map(ExactCc.unsafeFromExtinction)
    }

  val genObservingConditions: Gen[ObservingConditions] =
    for {
      iq <- Gen.oneOf(arbitrary[ExactIq].map(_.left[ImageQuality]), arbitrary[ImageQuality].map(_.right[ExactIq]))
      cc <- Gen.oneOf(arbitrary[ExactCc].map(_.left[CloudCover]), arbitrary[CloudCover].map(_.right[ExactCc]))
      wv <- arbitrary[WaterVapor]
      sb <- arbitrary[SkyBackground]
      am <- arbitrary[Double]       // should be in the range 1.0 - 3.0
    } yield ObservingConditions(iq, cc, wv, sb, am)

  implicit val arbObservingConditions: Arbitrary[ObservingConditions] =
    Arbitrary(genObservingConditions)

}

object observingconditions extends ArbObservingConditions
