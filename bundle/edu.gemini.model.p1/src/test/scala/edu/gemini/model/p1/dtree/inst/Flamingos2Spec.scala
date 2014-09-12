package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.SpecificationWithJUnit
import edu.gemini.model.p1.immutable.Flamingos2Filter

class Flamingos2Spec extends SpecificationWithJUnit {
  "The Flamingos2 decision tree" should {
    "not include the narrow band filters, REL-1282" in {
      val f2 = new Flamingos2.ImagingFilterNode()
      f2.choices must have size(5)
      // sanity check
      f2.choices must contain (Flamingos2Filter.Y)
    }
  }

}
