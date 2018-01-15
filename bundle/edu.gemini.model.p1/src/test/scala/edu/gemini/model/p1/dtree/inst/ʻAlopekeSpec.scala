package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.Specification

class ʻAlopekeSpec extends Specification {
  "The ʻAlopeke decision tree should" {
    "include ʻAlopeke modes" in {
      val ʻalopeke = ʻAlopeke()
      ʻalopeke.title must beEqualTo("Mode")
      ʻalopeke.choices must have size 2
    }
  }
}
