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

object CoordinatesSpec extends Specification with ScalaCheck with Arbitraries {

  "Coordinates Offsetting" should {

    "be consistent with diff" !
      forAll { (a: Coordinates, b: Coordinates) =>
        val (dRA, dDec) = a diff b
        a.offset(dRA, dDec) ~= b
      }

    "flip the RA when Dec overflows" !
      forAll { (a: Coordinates, dDec: Angle) => 
        val ra0 = a.ra.toAngle
        val ra1 = a.offset(Angle.zero, dDec).ra.toAngle
        if (a.dec.offset(dDec)._2) ra1 ~= ra0.flip
        else                       ra1 ~= ra0
      }

  }

  "Coordinates Diff" should {

    "have identity (1)" !
      forAll { (a: Coordinates) =>
        val (dRA, dDec) = a diff a
        (dRA ~= Angle.zero) && (dDec ~= Angle.zero)
      }

    "have identity (2)" !
      forAll { (a: Coordinates) =>
        val (dRA, dDec) = Coordinates.zero diff a
        (dRA ~= a.ra.toAngle) && (dDec ~= a.dec.toAngle)
      }

  }

}


