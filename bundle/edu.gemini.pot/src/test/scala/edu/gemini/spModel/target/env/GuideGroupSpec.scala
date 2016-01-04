package edu.gemini.spModel.target.env

import edu.gemini.shared.util.immutable.ImOption

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen

import org.specs2.ScalaCheck

import org.specs2.mutable.Specification

class GuideGroupSpec extends Specification with ScalaCheck with Arbitraries {
  "GuideGroup name" should {
    "always be defined for manual groups, undefined for automatic" in
      forAll { (g: GuideGroup) =>
        g.grp match {
          case a: AutomaticGroup => g.getName must_== ImOption.empty
          case _                 => g.getName should_!= ImOption.empty
        }
      }
  }

}
