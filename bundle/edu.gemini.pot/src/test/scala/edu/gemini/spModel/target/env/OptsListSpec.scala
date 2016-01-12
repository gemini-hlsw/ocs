package edu.gemini.spModel.target.env

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class OptsListSpec extends Specification with ScalaCheck with Arbitraries {

  "OptsList" should {
    "produce a left disjuntion on clearFocus" in
      forAll { (opts: OptsList[Int]) =>
        opts.clearFocus.toDisjunction.isLeft
      }
  }
}
