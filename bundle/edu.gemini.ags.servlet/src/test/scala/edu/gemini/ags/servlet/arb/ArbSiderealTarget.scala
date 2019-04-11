package edu.gemini.ags.servlet.arb

import edu.gemini.spModel.core._

import org.scalacheck._
import org.scalacheck.Arbitrary._


trait ArbSiderealTarget {

  import coordinates._

  implicit val arbProperMotion: Arbitrary[ProperMotion] =
    Arbitrary {
      for {
        r <- Gen.choose(0, 10)
        d <- Gen.choose(0, 10)
        y <- Gen.choose(1950, 2050)
      } yield ProperMotion(
        RightAscensionAngularVelocity(AngularVelocity(r.toDouble)),
        DeclinationAngularVelocity(AngularVelocity(d.toDouble)),
        Epoch(y.toDouble)
      )
    }

  implicit val arbRedshift: Arbitrary[Redshift] =
    Arbitrary {
      Gen.choose(0, 10).map(z => Redshift(z.toDouble))
    }

  implicit val arbParallax: Arbitrary[Parallax] =
    Arbitrary {
      Gen.choose(0, 10).map(mas => Parallax(mas.toDouble))
    }

  implicit val arbMagnitude: Arbitrary[Magnitude] =
    Arbitrary {
      for {
        v <- Gen.choose(0, 20)
        b <- Gen.oneOf(MagnitudeBand.all)
        e <- Gen.oneOf(Gen.const(Option.empty[Double]), Gen.choose(0, 1).map(e => Some(e.toDouble)))
        s <- Gen.oneOf(MagnitudeSystem.all)
      } yield Magnitude(v, b, e, s)
    }

  implicit val arbSiderealTarget: Arbitrary[SiderealTarget] =
    Arbitrary {
      for {
        n <- Gen.alphaStr
        c <- arbitrary[Coordinates]
        p <- arbitrary[Option[ProperMotion]]
        r <- arbitrary[Option[Redshift]]
        x <- arbitrary[Option[Parallax]]
        m <- arbitrary[List[Magnitude]]
      } yield SiderealTarget(n, c, p, r, x, m, None, None)
    }

}

object siderealtarget extends ArbSiderealTarget
