package edu.gemini.ags.servlet.arb

import edu.gemini.spModel.core._

import org.scalacheck._
import org.scalacheck.Arbitrary._


trait ArbSiderealTarget {

  import coordinates._

  implicit val arbSiderealTarget: Arbitrary[SiderealTarget] =
    Arbitrary {
      for {
        n <- Gen.alphaStr
        c <- arbitrary[Coordinates]
      } yield SiderealTarget(n, c, None, None, None, Nil, None, None)
    }

}

object siderealtarget extends ArbSiderealTarget
