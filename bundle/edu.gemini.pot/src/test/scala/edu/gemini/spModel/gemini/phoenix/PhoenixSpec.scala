package edu.gemini.spModel.gemini.phoenix

import edu.gemini.model.p1.immutable.{PhoenixFocalPlaneUnit, PhoenixFilter}
import org.specs2.mutable.Specification

class PhoenixSpec extends Specification {
  "Phoenix" should {
    "Support all phase1 filters" in {
      PhoenixFilter.values.forall(f => Option(PhoenixParams.Filter.valueOf(f.name)).isDefined) should beTrue
    }
    "Support all phase1 fpus" in {
      PhoenixFocalPlaneUnit.values.forall(f => Option(PhoenixParams.Mask.valueOf(f.name)).isDefined) should beTrue
    }
  }
}
