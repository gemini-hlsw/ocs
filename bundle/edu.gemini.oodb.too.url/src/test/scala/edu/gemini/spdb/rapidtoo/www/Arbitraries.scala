package edu.gemini.spdb.rapidtoo.www

import edu.gemini.spdb.rapidtoo.TooGuideTarget.GuideProbe

import org.scalacheck._
import org.scalacheck.Gen._

trait Arbitraries {

  implicit val arbGuideProbe: Arbitrary[GuideProbe] =
    Arbitrary(oneOf(GuideProbe.values))

}


