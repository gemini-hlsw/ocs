package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.Specification
import edu.gemini.model.p1.immutable.Flamingos2Filter

class Flamingos2Spec extends Specification {
  "The Flamingos2 decision tree" should {
    "not include the narrow band filters, REL-1282" in {
      val f2 = new Flamingos2.ImagingFilterNode()
      f2.choices must have size 6
      // sanity check
      f2.choices must not contain (Flamingos2Filter.Y)
    }
    "include the K-Long filter, REL-2308" in {
      val f2 = new Flamingos2.ImagingFilterNode()
      f2.choices must have size 6
      // sanity check
      f2.choices must contain (Flamingos2Filter.K_LONG)
    }
  }

}
