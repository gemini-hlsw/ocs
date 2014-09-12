package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import org.specs2.mutable._

class GmosSBlueprintSpec extends SpecificationWithJUnit with SemesterProperties {

  "The GmosS Blueprint" should {
    "include the Lya395 filter, REL-1236" in {
      val blueprint = GmosSBlueprintImaging(GmosSFilter.IMAGING)
      blueprint.filters must contain(M.GmosSFilter.Lya395_G0342)
    }
    "include the Z (876 nm) filter, REL-1723" in {
      val blueprint = GmosSBlueprintImaging(GmosSFilter.IMAGING)
      blueprint.filters must contain(M.GmosSFilter.Z_G0343)
    }
    "include the Y (1010 nm) filter, REL-1723" in {
      val blueprint = GmosSBlueprintImaging(GmosSFilter.IMAGING)
      blueprint.filters must contain(M.GmosSFilter.Y_G0344)
    }
  }

}
