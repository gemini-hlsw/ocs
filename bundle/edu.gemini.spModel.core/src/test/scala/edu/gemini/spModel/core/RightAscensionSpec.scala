package edu.gemini.spModel.core

import scalaz._
import Scalaz._
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import AlmostEqual.AlmostEqualOps

object RightAscensionSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

  "RightAscension Conversions" should {
   
    "support Angles" !
      forAll { (ra: RightAscension) =>  
        RightAscension.fromAngle(ra.toAngle) == ra
      }

  }

  "RightAscension Offsetting" should {
   
    "have identity" !
      forAll { (ra: RightAscension) =>  
        ra.offset(Angle.zero) ~= ra
      }

    "be invertible" !
      forAll { (ra: RightAscension, a: Angle) =>  
        ra.offset(a).offset(a * -1) ~= ra
      }

    "distribute" !
      forAll { (ra: RightAscension, a: Angle, b: Angle) =>  
        ra.offset(a).offset(b) ~= ra.offset(a + b)
      }

  }

 "RightAscension Serialization" should {

    "Support Java Binary" ! 
      forAll { (ra: RightAscension) =>
        canSerialize(ra)
      }

  }

}


