package edu.gemini.spModel.core

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

object HorizonsDesignationSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

  "HorizonsDesignation" should {
    "round trip from String" !
      forAll { (hd: HorizonsDesignation) =>
        HorizonsDesignation.read(hd.show).contains(hd)
      }
  }
}
