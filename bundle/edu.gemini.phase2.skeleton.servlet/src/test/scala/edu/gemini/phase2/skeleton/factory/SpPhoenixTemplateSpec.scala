package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable.PhoenixBlueprint
import edu.gemini.model.p1.mutable.{PhoenixFilter, PhoenixFocalPlaneUnit}
import edu.gemini.pot.sp.ISPGroup
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.phoenix.PhoenixParams.{Filter, Mask}
import edu.gemini.spModel.gemini.phoenix.blueprint.SpPhoenixBlueprint

import org.scalacheck.{ Properties, Gen, Arbitrary }
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._
import scalaz._, Scalaz._

object SpPhoenixTemplateSpec extends TemplateSpec("PHOENIX_BP.xml") with Specification with ScalaCheck {

  def test(fpu: PhoenixFocalPlaneUnit, filter: PhoenixFilter) =
    expand(proposal(PhoenixBlueprint(fpu, filter), List(1), MagnitudeBand.R)) { (p, sp) =>
      s"Phoenic Blueprint Expansion $fpu $filter " >> {

        "There should be exactly one template group." in {
          groups(sp).size must_== 1
        }

        "It should contain all four observations." in {
          groups(sp).forall(libs(_) == Set(1, 2, 3, 4))
        }

        "It should contain the how-to note." in {
          groups(sp).forall(existsNote(_, "How to use the observations in this folder"))
        }

        "It should contain the calibration note." in {
          groups(sp).forall(existsNote(_, "Darks, Flats, and Arcs"))
        }

        // TODO: check the configuration of the observations

      }
    }


  PhoenixFilter.values.foreach { filter =>
    test(PhoenixFocalPlaneUnit.MASK_1, filter)
  }

}
