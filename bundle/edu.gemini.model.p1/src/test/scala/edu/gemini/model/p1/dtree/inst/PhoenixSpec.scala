package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.mutable.{PhoenixFocalPlaneUnit, PhoenixFilter}
import org.specs2.mutable.SpecificationWithJUnit

class PhoenixSpec extends SpecificationWithJUnit {
  "The Phoenix decision tree" should {
    "includes Phoenix GPU" in {
      val phoenix = Phoenix()
      phoenix.title must beEqualTo("Focal Plane Unit")
      phoenix.choices must have size 3
      // Check the default
      phoenix.default should beSome(PhoenixFocalPlaneUnit.MASK_3)
    }
    "includes Phoenix filter modes" in {
      val phoenix = Phoenix()
      val filterNode = phoenix.apply(PhoenixFocalPlaneUnit.MASK_1).a
      filterNode.title must beEqualTo("Filter")
      filterNode.choices must have size 20
      // Check the default filter
      filterNode.default must beSome(List(PhoenixFilter.K4396))
    }
  }

}
