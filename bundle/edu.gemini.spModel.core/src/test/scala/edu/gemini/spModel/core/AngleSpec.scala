package edu.gemini.spModel.core

import scalaz._
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import AlmostEqual.AlmostEqualOps

object AngleSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

  "Angle Conversions" should {

    "support Degrees" !
      forAll { (a: Angle) =>
        Angle.fromDegrees(a.toDegrees) ~= a
      }

    "support Arcsecs" !
      forAll { (a: Angle) =>
        Angle.fromArcsecs(a.toDegrees * 3600) ~= a
      }

    "support Arcmins" !
      forAll { (a: Angle) =>
        Angle.fromArcmin(a.toDegrees * 60) ~= a
      }

    "support Radians" !
      forAll { (a: Angle) =>
        Angle.fromRadians(a.toRadians) ~= a
      }

    "support HourAngle" !
      forAll { (a: Angle) =>
        val hms = a.toHourAngle
        Angle.fromHourAngle(hms.hours, hms.minutes, hms.seconds).get ~= a
      }

    "support Sexigesimal" !
      forAll { (a: Angle) =>
        val dms = a.toSexigesimal
        Angle.fromSexigesimal(dms.degrees, dms.minutes, dms.seconds).get ~= a
      }

  }


  "Angle Scalar Multiplication" should {

    "have identity" !
      forAll { (a: Angle) =>
        a * 1 ~= a
      }

    "have void" !
      forAll { (a: Angle) =>
        a * 0 ~= Angle.zero
      }

    "be consistent with addition" !
      forAll { (a: Angle) =>
        a * 2 ~= a + a
      }

  }

  "Angle Scalar Division" should {

    "have identity" !
      forAll { (a: Angle) =>
        a / 1 ~= Some(a)
      }

    "have void" !
      forAll { (a: Angle) =>
        a / 0 ~= None
      }

    "be consistent with multiplication" !
      forAll { (a: Angle) =>
        a * 0.5 ~= (a / 2).getOrElse(Angle.zero)
      }

  }

  "Angle Addition" should {

    "have left identity" !
      forAll { (a: Angle) =>
        Angle.zero + a ~= a
      }

    "have right identity" !
      forAll { (a: Angle) =>
        a + Angle.zero ~= a
      }

    "commute" !
      forAll { (a: Angle, b: Angle) =>
        a + b ~= b + a
      }

    "associate" !
      forAll { (a: Angle, b: Angle, c: Angle) =>
        a + (b + c) ~= (a + b) + c
      }

    "be equivalent to subtraction when negated" !
      forAll { (a: Angle, b: Angle) =>
        a + b ~= a - (b * -1)
      }

  }

  "Angle Subtraction" should {

    "have right identity" !
      forAll { (a: Angle) =>
        a - Angle.zero ~= a
      }

    "be equivalent to addition when negated" !
      forAll { (a: Angle, b: Angle) =>
        a - b ~= a + (b * -1)
      }

  }

  "Angle Formatting/Parsing Roundtrips" should {

    "Support Degrees" !
      forAll { (a: Angle) =>
        val s = a.formatDegrees.init // drop the °
        Angle.parseDegrees(s).fold(_ => false, _ ~= a)
      }

    "Support Radians" !
      forAll { (a: Angle) =>
        val s = a.toRadians.toString
        Angle.parseRadians(s).fold(_ => false, _ ~= a)
      }

    "Support Sexigesimal" !
      forAll { (a: Angle) =>
        val s = a.formatSexigesimal
        Angle.parseSexigesimal(s).fold(_ => false, _ ~= a)
      }

    "Support HourAngle" !
      forAll { (a: Angle) =>
        val s = a.formatHourAngle
        Angle.parseHourAngle(s).fold(_ => false, _ ~= a)
      }

  }

  "Angle Serialization" should {

    "Support Java Binary" !
      forAll { (a: Angle) =>
        canSerialize(a)
      }

  }

}


