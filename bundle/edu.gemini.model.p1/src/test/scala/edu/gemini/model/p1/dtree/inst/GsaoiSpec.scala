package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.SpecificationWithJUnit

class GsaoiSpec extends SpecificationWithJUnit {
  "The Gsaoi decision tree" should {
      "includes Gsaoi filters" in {
        val gsaoi = Gsaoi()
        gsaoi.title must beEqualTo("Filters")
        gsaoi.choices must have size(22)
      }
  }

}
