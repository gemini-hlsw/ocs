package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.Specification

class Igrins2Spec extends Specification {
  "The IGRINS-2 decision tree" should {
    "include nodding modes" in {
      val alopeke = Igrins2()
      alopeke.title must beEqualTo("Nodding")
      alopeke.choices must have size 2
    }
  }
}
