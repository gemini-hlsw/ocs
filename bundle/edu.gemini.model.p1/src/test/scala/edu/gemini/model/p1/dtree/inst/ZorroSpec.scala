package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.Specification

class ZorroSpec extends Specification {
  "The Zorro decision tree" should {
    "include Zorro modes" in {
      val zorro = Zorro()
      zorro.title must beEqualTo("Mode")
      zorro.choices must have size 2
    }
  }
}
