package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.SpecificationWithJUnit

class TexesSpec extends SpecificationWithJUnit {
  "The Texes decision tree" should {
      "includes a disperser choice" in {
        val texes = Texes()
        texes.title must beEqualTo("Disperser")
        texes.choices must have size(4)
      }
  }

}
