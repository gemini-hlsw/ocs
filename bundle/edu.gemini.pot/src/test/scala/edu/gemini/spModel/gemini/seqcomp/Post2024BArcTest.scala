package edu.gemini.spModel.gemini.seqcomp

import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.obsclass.ObsClass
import org.junit.Test

/**
 * Tests that ARCs default to PARTNER_CAL post-24B.
 */
class Post2024BArcTest extends SmartGcalArcObsClassBase {
  override val progId = SPProgramID.toProgramID("GS-2024B-Q-1")

  @Test def testArcObsClassPost24B() {
    expect(ObsClass.PARTNER_CAL)
  }

}