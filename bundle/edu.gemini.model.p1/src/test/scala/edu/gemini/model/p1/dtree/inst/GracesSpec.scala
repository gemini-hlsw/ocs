package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.SpecificationWithJUnit
import edu.gemini.model.p1.immutable.{GracesReadMode, GracesFiberMode}

class GracesSpec extends SpecificationWithJUnit {
  "The Graces decision tree" should {
    "includes Graces fiber modes" in {
      val graces = Graces()
      graces.title must beEqualTo("Fiber Mode")
      graces.choices must have size 2
    }
    "includes Graces read modes" in {
      val graces = Graces()
      val readModeChoice = graces.apply(GracesFiberMode.forName("ONE_FIBER"))
      readModeChoice must beLeft
      val readMode = readModeChoice.left.get
      readMode.title must beEqualTo("Read Mode")
      readMode.choices must have size 3
    }
    "build a Graces blueprint" in {
      val graces = Graces()
      val readModeChoice = graces.apply(GracesFiberMode.forName("ONE_FIBER"))
      readModeChoice must beLeft
      val readMode = readModeChoice.left.get

      val blueprint = readMode.apply(GracesReadMode.forName("SLOW"))
      blueprint must beRight
    }
  }

}
