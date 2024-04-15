package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.Specification

class Igrins2Spec extends Specification {
  "The IGRINS-2 decision tree" should {
    "include nodding modes" in {
      val igrins2 = Igrins2()
      igrins2.title must beEqualTo("Nodding")
      igrins2.choices must have size 2
    }
  }
}
