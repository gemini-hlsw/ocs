package edu.gemini.spModel.gemini.seqcomp

import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances.IDENTITY_MAP
import edu.gemini.spModel.config2.ItemKey
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.calunit.calibration.CalDictionary
import edu.gemini.spModel.gemini.calunit.CalUnitParams._
import edu.gemini.spModel.gemini.gnirs.InstGNIRS
import edu.gemini.spModel.obsclass.ObsClass
import org.junit.Assert._
import org.junit.Before
import org.junit.Test

/**
 * Tests that ARCs default to PROG_CAL pre-24B.
 */
class Pre2024BArcTest extends SmartGcalArcObsClassBase {
  override val progId = SPProgramID.toProgramID("GS-2024A-Q-1")

  @Test def testArcObsClassPre24B() {
    expect(ObsClass.PROG_CAL)
  }

}