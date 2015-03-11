package edu.gemini.spModel.core

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.scalacheck.Prop.forAll

class AngleVelocitySpec extends Specification with ScalaCheck with Arbitraries {
  "RightAscensionAngleVelocity" should {
    "be always in range" in {
      forAll { (a: RightAscensionAngularVelocity) =>
        a.toMilliArcSecondsPerYear should be_<=(AngularVelocity.MilliArcSecsInADegree)
        a.toMilliArcSecondsPerYear should be_>=(-AngularVelocity.MilliArcSecsInADegree)
        a.toDegreesPerYear should be_<=(360.0)
        a.toDegreesPerYear should be_>=(-360.0)
      }
    }
  }
  "DeclinationAngleVelocity" should {
    "be always in range" in {
      forAll { (a: DeclinationAngularVelocity) =>
        a.toMilliArcSecondsPerYear should be_<=(AngularVelocity.MilliArcSecsInADegree)
        a.toMilliArcSecondsPerYear should be_>=(-AngularVelocity.MilliArcSecsInADegree)
        a.toDegreesPerYear should be_<=(360.0)
        a.toDegreesPerYear should be_>=(-360.0)
      }
    }
  }
}
