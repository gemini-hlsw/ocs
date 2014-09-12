package edu.gemini.spModel.gemini.visitor.blueprint

import org.junit.Test
import org.junit.Assert._
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.gemini.texes.TexesParams

class SpVisitorBlueprintTest {
  @Test
  def sanityTests() {
    // Check filters are preserved
    val blueprint = new SpVisitorBlueprint("name")
    assertEquals("name", blueprint.name)

    // Verify Could be in GN
    assertEquals(1, blueprint.toParamSet(new PioXmlFactory).getParams(SpVisitorBlueprint.NAME_PARAM_NAME).size())
  }
}
