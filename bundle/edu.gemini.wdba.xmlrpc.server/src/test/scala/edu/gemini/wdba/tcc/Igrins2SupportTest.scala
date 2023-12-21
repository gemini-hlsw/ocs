//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.gemini.igrins2.Igrins2
import edu.gemini.spModel.telescope.IssPort
import org.junit.Test;

/**
 * Test cases for GMOS instrument support.
 */
class Igrins2SupportTest extends InstrumentSupportTestBase[Igrins2](SPComponentType.INSTRUMENT_IGRINS2) {

    @Test def testIG2_5(): Unit = {
        val ig2 = getInstrument()
        ig2.setIssPort(IssPort.SIDE_LOOKING)
        setInstrument(ig2)

        verifyInstrumentConfig(getSouthResults(), "IGRINS2_3")
    }

    @Test def testIG2_1(): Unit = {
        val ig2 = getInstrument()
        ig2.setIssPort(IssPort.UP_LOOKING)
        setInstrument(ig2)

        verifyInstrumentConfig(getSouthResults(), "IGRINS2")
    }
}
