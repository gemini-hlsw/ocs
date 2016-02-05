package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.SpecificationWithJUnit
import edu.gemini.model.p1.mutable.GpiObservingMode

class GpiSpec extends SpecificationWithJUnit {
  "The Gpi decision tree" should {
    "includes Gpi observing modes" in {
      val gpi = Gpi()
      gpi.title must beEqualTo("Observing Mode")
      gpi.choices must have size 20
    }
    "includes Gpi disperser modes" in {
      val gpi = Gpi()
      val disperser = gpi.apply(GpiObservingMode.CORON_H_BAND).a
      disperser.title must beEqualTo("Disperser")
      disperser.choices must have size 2
    }
  }

}
