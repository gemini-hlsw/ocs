//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.gemini.igrins2.Igrins2
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import edu.gemini.spModel.telescope.IssPort
import org.junit.Test

/**
 * Test cases for IGRINS2 instrument support.
 */
class Igrins2SupportTest extends InstrumentSupportTestBase[Igrins2](SPComponentType.INSTRUMENT_IGRINS2) {

  @Test def testIG2_side(): Unit = {
    setPort(IssPort.SIDE_LOOKING)
    verifyInstrumentConfig(getSouthResults(), "IGRINS2_3")
  }

  @Test def testIG2_side_P1(): Unit = {
    setPort(IssPort.SIDE_LOOKING)
    addGuideStar(PwfsGuideProbe.pwfs1)
    verifyInstrumentConfig(getSouthResults(), "IGRINS2_3_P1")
  }

  @Test def testIG2_side_P2(): Unit = {
    setPort(IssPort.SIDE_LOOKING)
    addGuideStar(PwfsGuideProbe.pwfs2)
    verifyInstrumentConfig(getSouthResults(), "IGRINS2_3_P2")
  }

  @Test def testIG2_up(): Unit = {
    setPort(IssPort.UP_LOOKING)
    verifyInstrumentConfig(getSouthResults(), "IGRINS2")
  }

  @Test def testIG2_up_P1(): Unit = {
    setPort(IssPort.UP_LOOKING)
    addGuideStar(PwfsGuideProbe.pwfs1)
    verifyInstrumentConfig(getSouthResults(), "IGRINS2_P1")
  }

  @Test def testIG2_up_P2(): Unit = {
    setPort(IssPort.UP_LOOKING)
    addGuideStar(PwfsGuideProbe.pwfs2)
    verifyInstrumentConfig(getSouthResults(), "IGRINS2_P2")
  }
}
