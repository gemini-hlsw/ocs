package edu.gemini.spdb.rapidtoo.www

import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.skyobject.Magnitude.Band
import edu.gemini.spModel.core.{MagnitudeSystem, Angle, RightAscension, Declination}
import edu.gemini.spdb.rapidtoo.TooGuideTarget.GuideProbe

import org.scalacheck._
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._

trait Arbitraries {

  implicit val arbAngle: Arbitrary[Angle] =
    Arbitrary(arbitrary[Short].map(n => n / 10.0).map(Angle.fromDegrees))

  implicit val arbRA: Arbitrary[RightAscension] =
    Arbitrary(arbitrary[Angle].map(RightAscension.fromAngle))

  implicit val arbDec: Arbitrary[Declination] =
    Arbitrary(arbitrary[Angle].map(Declination.zero.offset(_)._1))

  implicit val arbMagnitudeBand: Arbitrary[Band] =
    Arbitrary(oneOf(Band.values))

  implicit val arbMagnitudeSystem: Arbitrary[MagnitudeSystem] =
    Arbitrary(oneOf(MagnitudeSystem.all))

  implicit val arbMagnitude: Arbitrary[Magnitude] =
    Arbitrary {
      for {
        value  <- arbitrary[Short].map(n => n / 10.0)
        band   <- arbitrary[Band]
        system <- arbitrary[MagnitudeSystem]
      } yield new Magnitude(band, value, system)
    }

  implicit val arbGuideProbe: Arbitrary[GuideProbe] =
    Arbitrary(oneOf(GuideProbe.values))

}


