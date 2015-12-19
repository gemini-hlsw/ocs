package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.immutable.Site
import edu.gemini.model.p1.mutable.{PhoenixFocalPlaneUnit, PhoenixFilter}
import org.specs2.mutable.SpecificationWithJUnit

class PhoenixSpec extends SpecificationWithJUnit {
  "The Phoenix decision tree" should {
    "includes a Site" in {
      val phoenix = Phoenix()
      phoenix.title must beEqualTo("Site")
      phoenix.choices must have size 2
      // Check no default site
      phoenix.default must beNone
    }
    "includes Phoenix FPU" in {
      val phoenix = Phoenix()
      val fpuNode = phoenix.apply(Site.GN).a
      fpuNode.title must beEqualTo("Focal Plane Unit")
      fpuNode.choices must have size 3
      // Check the default
      fpuNode.default should beSome(PhoenixFocalPlaneUnit.MASK_3)
    }
    "includes Phoenix filter modes" in {
      val phoenix = Phoenix()
      val fpuNode = phoenix.apply(Site.GN).a
      val filterNode = fpuNode.apply(PhoenixFocalPlaneUnit.MASK_1).a
      filterNode.title must beEqualTo("Filter")
      filterNode.choices must have size 21
      // Check the default filter
      filterNode.default must beSome(PhoenixFilter.K4396)
    }
  }

}
