package edu.gemini.ags.servlet.arb

import edu.gemini.spModel.core._

import org.scalacheck._
import org.scalacheck.Arbitrary._


trait ArbSiderealTarget {

  import coordinates._

  implicit val arbProperMotion: Arbitrary[ProperMotion] =
    Arbitrary {
      for {
        r <- Gen.choose(1, 10)
        d <- Gen.choose(1, 10)
        y <- Gen.choose(1950, 2050)
      } yield ProperMotion(
        RightAscensionAngularVelocity(AngularVelocity(r.toDouble)),
        DeclinationAngularVelocity(AngularVelocity(d.toDouble)),
        Epoch(y.toDouble)
      )
    }

  implicit val arbSiderealTarget: Arbitrary[SiderealTarget] =
    Arbitrary {
      for {
        n <- Gen.alphaStr
        c <- arbitrary[Coordinates]
        p <- arbitrary[Option[ProperMotion]]
      } yield SiderealTarget(n, c, p, None, None, Nil, None, None)
    }

}

object siderealtarget extends ArbSiderealTarget
