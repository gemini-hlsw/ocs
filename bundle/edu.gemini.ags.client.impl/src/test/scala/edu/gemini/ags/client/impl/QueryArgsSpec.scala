package edu.gemini.ags.client.impl

import org.specs2.mutable.SpecificationWithJUnit
import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.mutable.TexesDisperser

class QueryArgsSpec extends SpecificationWithJUnit {
  "The QueryArgs" should {
    "should map texes to Nifs. REL-1062" in {
      QueryArgs.instId(new TexesBlueprint(TexesDisperser.D_32_LMM)) must beRight.like {
        case i => i must beEqualTo(Instrument.Nifs.id)
      }
    }
    "should map Speckle to Nifs. REL-1061" in {
      QueryArgs.instId(new DssiBlueprint(Site.GN)) must beRight.like {
        case i => i must beEqualTo(Instrument.Nifs.id)
      }
    }
    "should map Visitor to Niri f/6. REL-1090" in {
      QueryArgs.instId(new VisitorBlueprint(Site.GN, "name")) must beRight.like {
        case i => i must beEqualTo(Instrument.Niri.id)
      }
      QueryArgs.instSpecificArgs(new VisitorBlueprint(Site.GN, "name")) must contain("niriCamera" -> "F6")
    }
  }
}
