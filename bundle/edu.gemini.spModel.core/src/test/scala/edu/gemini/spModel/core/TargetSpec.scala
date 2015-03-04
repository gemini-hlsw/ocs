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

object TargetSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

 "TargetSpec Serialization" should {

    "Support Java Binary" !
      forAll { (t: Target) =>
        canSerialize(t)
      }

  }

}
