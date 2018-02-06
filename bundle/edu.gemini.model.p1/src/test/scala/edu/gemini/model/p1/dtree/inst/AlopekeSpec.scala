package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.Specification

class AlopekeSpec extends Specification {
  "The 'Alopeke decision tree" should {
    "include 'Alopeke modes" in {
      val alopeke = Alopeke()
      alopeke.title must beEqualTo("Mode")
      alopeke.choices must have size 2
    }
  }
}
