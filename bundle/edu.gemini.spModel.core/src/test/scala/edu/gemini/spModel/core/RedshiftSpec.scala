package edu.gemini.spModel.core

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import squants.motion.KilometersPerSecond
import edu.gemini.spModel.core.AlmostEqual.AlmostEqualOps

class RedshiftSpec extends Specification with ScalaCheck with Arbitraries with Helpers {
  "Redshift" should {
    "support conversion to Radial Velocity" in {
      forAll { (r: Redshift) =>
        val rv = r.toRadialVelocity.value
        !rv.isNaN && !rv.isInfinite && r.toRadialVelocity.unit == KilometersPerSecond
      }
    }
    "support conversion to Apparent Radial Velocity" in {
      forAll { (r: Redshift) =>
        val cz = r.toApparentRadialVelocity.value
        !cz.isNaN && !cz.isInfinite && r.toApparentRadialVelocity.unit == KilometersPerSecond
      }
    }
    "support conversion back and forth to Radial Velocity" in {
      forAll { (r: Redshift) =>
        // There may be some rounding errors on the conversion
        Redshift.fromRadialVelocity(r.toRadialVelocity) ~= r
      }
    }
    "support conversion back and forth to Apparent Radial Velocity" in {
      forAll { (r: Redshift) =>
        // There may be some rounding errors on the conversion
        Redshift.fromApparentRadialVelocity(r.toApparentRadialVelocity) ~= r
      }
    }
  }

  "Redshift Serialization" should {
     "Support Java Binary" !
       forAll { (r: Redshift) =>
         canSerialize(r)
       }

   }
}
