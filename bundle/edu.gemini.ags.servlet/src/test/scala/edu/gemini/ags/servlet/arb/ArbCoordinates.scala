package edu.gemini.ags.servlet.arb

import edu.gemini.spModel.core._

import org.scalacheck._
import org.scalacheck.Arbitrary._

trait ArbCoordinates {

  // N.B., we store Angles in Double degrees but format them with only
  // milliarcsecond precision so I'm simplifying the arbitrary to make it
  // invertible.

  implicit val arbDeclination: Arbitrary[Declination] =
    Arbitrary {
      Gen.choose(-90, 90).map(d => Declination.fromDegrees(d.toDouble)).map(_.get)
    }

  implicit val arbRightAscension: Arbitrary[RightAscension] =
    Arbitrary {
      Gen.choose(0, 359).map(d => RightAscension.fromDegrees(d.toDouble))
    }

  implicit val arbCoordinates: Arbitrary[Coordinates] =
    Arbitrary {
      for {
        ra  <- arbitrary[RightAscension]
        dec <- arbitrary[Declination]
      } yield Coordinates(ra, dec)
    }

}

object coordinates extends ArbCoordinates

