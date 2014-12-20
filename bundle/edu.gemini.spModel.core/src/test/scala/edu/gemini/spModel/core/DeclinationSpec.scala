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

object DeclinationSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

  "Declination Conversions" should {
   
    "support Angles" !
      forAll { (dec: Declination) =>  
        Declination.fromAngle(dec.toAngle).get == dec
      }

    "declination of 90 is allowed" ! {
      val deg90 = Angle.fromDegrees(90)
      Declination.fromAngle(deg90).get.toAngle == deg90
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

 "Declination Formatting/Parsing Roundtrips" should {

    "Support Degrees" !
      forAll { (dec: Declination) =>
        val s = dec.formatDegrees.init // drop the °
        Angle.parseDegrees(s).fold(_ => false, _ ~= dec.toAngle)
      }

    "Support Sexigesimal" ! 
      forAll { (dec: Declination) =>
        val s = dec.formatSexigesimal
        Angle.parseSexigesimal(s).fold(_ => false, _ ~= dec.toAngle)
      }

  }

  "Declination Serialization" should {

    "Support Java Binary" ! 
      forAll { (dec: Declination) =>
        canSerialize(dec)
      }

  }

}


