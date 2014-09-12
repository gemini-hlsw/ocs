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

object DeclinationSpec extends Specification with ScalaCheck with Arbitraries {

  "Declination Conversions" should {
   
    "support Angles" !
      forAll { (dec: Declination) =>  
        Declination.fromAngle(dec.toAngle).get == dec
      }

  }

  "Declination Offsetting" should {
   
    "have identity" !
      forAll { (dec: Declination) =>  
        dec.offset(Angle.zero)._1 ~= dec
      }

    "set the carry bit properly" !
      forAll { (dec: Declination, a: Angle) =>  
        dec.offset(a)._2 == Declination.fromAngle(dec.toAngle + a).isEmpty
      }

  }


}


