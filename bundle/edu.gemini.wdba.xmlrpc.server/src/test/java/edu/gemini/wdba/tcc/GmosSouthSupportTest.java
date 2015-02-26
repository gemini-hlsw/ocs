//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.telescope.IssPort;
import org.junit.Test;

/**
 * Test cases for GMOS instrument support.
 */
public class GmosSouthSupportTest extends InstrumentSupportTestBase<InstGmosSouth>{

    public GmosSouthSupportTest() {
        super(InstGmosSouth.SP_TYPE);
    }

    @Test public void testNoAoNoIfuPointOrig() throws Exception {
        verifyPointOrig(getSouthResults(), "gmos");
    }

    @Test public void testNoAoYesIfuPointOrig() throws Exception {
        InstGmosSouth gmos = getInstrument();
        gmos.setFPUnit(GmosSouthType.FPUnitSouth.IFU_1);
        setInstrument(gmos);

        verifyPointOrig(getSouthResults(), "gmos_ifu");
    }

    @Test public void testLgsPointOrig() throws Exception {
        // probably not even legal, but there it is ...
        addGems(); verifyPointOrig(getSouthResults(), "lgs2gmos");
    }

    @Test public void testLgsP1PointOrig() throws Exception {
        addAltair(AltairParams.Mode.LGS_P1);
        verifyPointOrig(getSouthResults(), "lgs2gmos_p1");
    }

    @Test public void testSideLooking() throws Exception {
        setPort(IssPort.SIDE_LOOKING);
        verifyInstrumentConfig(getSouthResults(), "GMOS3");
    }

    @Test public void testUpLooking() throws Exception {
        setPort(IssPort.UP_LOOKING);
        verifyInstrumentConfig(getSouthResults(), "GMOS");
    }
}
