//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.telescope.IssPort;
import org.junit.Test;

import static edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType.LGS;
import static edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType.NGS;

/**
 * Test cases for GMOS instrument support.
 */
public final class GmosNorthSupportTest extends InstrumentSupportTestBase<InstGmosNorth>{

    public GmosNorthSupportTest() {
        super(InstGmosNorth.SP_TYPE);
    }

    @Test public void testNoAoNoIfuPointOrig() throws Exception {
        verifyPointOrig(getSouthResults(), "gmos");
    }

    @Test public void testNoAoYesIfuPointOrig() throws Exception {
        InstGmosNorth gmos = getInstrument();
        gmos.setFPUnit(GmosNorthType.FPUnitNorth.IFU_1);
        setInstrument(gmos);

        verifyPointOrig(getSouthResults(), "gmos_ifu");
    }

    @Test public void testLgsPointOrig() throws Exception {
        addAltair(LGS); verifyPointOrig(getSouthResults(), "lgs2gmos");
    }

    @Test public void testLgsP1PointOrig() throws Exception {
        addAltair(AltairParams.Mode.LGS_P1);
        addGuideStar(PwfsGuideProbe.pwfs1);
        verifyPointOrig(getSouthResults(), "lgs2gmos_p1");
    }

    @Test public void testNgsPointOrig() throws Exception {
        addAltair(NGS); verifyPointOrig(getSouthResults(), "ngs2gmos");
    }

    @Test public void testNoAoSideLooking() throws Exception {
        setPort(IssPort.SIDE_LOOKING);
        verifyInstrumentConfig(getNorthResults(), "GMOS5");
    }

    @Test public void testNoAoUpLooking() throws Exception {
        setPort(IssPort.UP_LOOKING);
        verifyInstrumentConfig(getNorthResults(), "GMOS");
    }

    @Test public void testAoSideLooking() throws Exception {
        setPort(IssPort.SIDE_LOOKING);
        addAltair();
        verifyInstrumentConfig(getNorthResults(), "AO2GMOS5");
    }

    @Test public void testAoUpLooking() throws Exception {
        setPort(IssPort.UP_LOOKING);
        addAltair();
        verifyInstrumentConfig(getNorthResults(), "AO2GMOS");
    }

}
