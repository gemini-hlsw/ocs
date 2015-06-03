package edu.gemini.spModel.core

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import AlmostEqual.AlmostEqualOps

object WavelengthSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

  "Wavelength Conversions" should {
   
    "support nanometers" !
      forAll { (w: Wavelength) =>
        Wavelength.fromNanometers(w.toNanometers) ~= w
      }

    "support microns" !
      forAll { (w: Wavelength) =>
        Wavelength.fromMicrons(w.toMicrons) ~= w
      }

  }

 "Wavelength Serialization" should {

    "Support Java Binary" ! 
      forAll { (w: Wavelength) =>
        canSerialize(w)
      }

  }

}


