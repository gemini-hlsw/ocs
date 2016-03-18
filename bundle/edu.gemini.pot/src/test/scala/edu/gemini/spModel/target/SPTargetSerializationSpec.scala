package edu.gemini.spModel.target

import edu.gemini.spModel.core.Arbitraries
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.Target._
import edu.gemini.spModel.pio.codec._
import edu.gemini.spModel.core.TargetSpec.canSerialize

import scalaz._
import Scalaz._
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import AlmostEqual.AlmostEqualOps

object SPTargetSerializationSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

  "SPTarget Serialization" should {

    "Preserve New Target" !
      forAll { (t: Target) =>
        canSerializeP({
          val spt = new SPTarget
          spt.setNewTarget(t)
          spt
        })((a, b) => a.getTarget == b.getTarget)
      }

  }

}
