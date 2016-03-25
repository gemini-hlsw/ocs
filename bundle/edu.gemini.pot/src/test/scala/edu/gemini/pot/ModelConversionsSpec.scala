package edu.gemini.pot

import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.skyobject
import edu.gemini.skycalc
import edu.gemini.spModel.core._
import edu.gemini.spModel.target.SPTarget
import org.specs2.ScalaCheck
import org.scalacheck.Prop._
import org.specs2.mutable.Specification
import AlmostEqual.AlmostEqualOps

import scalaz._
import Scalaz._

class ModelConversionsSpec extends Specification with ScalaCheck with Arbitraries {
  "implicit conversions of model classes" should {
    "convert old Angles to new" in {
      forAll { (a: Angle) =>
        val oldAngle = skycalc.Angle.degrees(a.toDegrees)
        oldAngle.toNewModel ~= a
      }
    }
    "convert new Angles to old" in {
      forAll { (a: Angle) =>
        a.toOldModel.toDegrees.getMagnitude should beCloseTo(a.toDegrees, 0.001)
      }
    }
    "convert old Coordinates to new" in {
      forAll { (c: Coordinates) =>
        val ra = skycalc.Angle.degrees(c.ra.toAngle.toDegrees)
        val dec = skycalc.Angle.degrees(c.dec.toAngle.toDegrees)
        val oldCoordinates = new skycalc.Coordinates(ra, dec)
        oldCoordinates.toNewModel ~= c
      }
    }
  }
}
