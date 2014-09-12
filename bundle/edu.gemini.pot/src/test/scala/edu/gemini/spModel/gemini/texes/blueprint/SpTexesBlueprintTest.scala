package edu.gemini.spModel.gemini.texes.blueprint

import org.junit.Test
import org.junit.Assert._
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.gemini.gsaoi.blueprint.SpGsaoiBlueprint
import edu.gemini.spModel.gemini.texes.{TexesParams, InstTexes}

class SpTexesBlueprintTest {
  @Test
  def sanityTests() {
    // Check filters are preserved
    val blueprint = new SpTexesBlueprint(TexesParams.Disperser.D_32_LMM)
    assertEquals(TexesParams.Disperser.D_32_LMM, blueprint.disperser)

    // Veriff Texes is in GN
    assertEquals(1, blueprint.toParamSet(new PioXmlFactory).getParams(SpTexesBlueprint.DISPERSER_PARAM_NAME).size())
  }
}
