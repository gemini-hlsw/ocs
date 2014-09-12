package edu.gemini.spModel.core

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

  implicit val arbCoords: Arbitrary[Coordinates] =
    Arbitrary { 
      for { 
        ra  <- arbitrary[RightAscension]
        dec <- arbitrary[Declination]
      } yield Coordinates(ra, dec)
    }

}