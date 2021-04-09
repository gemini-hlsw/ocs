//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.telescope.IssPort;
import org.junit.Test;

/**
 * Test cases for GMOS instrument support.
 */
public final class GmosSouthSupportTest extends InstrumentSupportTestBase<InstGmosSouth>{

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

    @Test public void testIfuP1PointOrig() throws Exception {
        InstGmosSouth gmos = getInstrument();
        gmos.setFPUnit(GmosSouthType.FPUnitSouth.IFU_1);
        setInstrument(gmos);
        addGuideStar(PwfsGuideProbe.pwfs1);

        verifyPointOrig(getSouthResults(), "gmos_ifu_p1");
    }

    @Test public void testLgsPointOrig() throws Exception {
        // probably not even legal, but there it is ...
        addGems(); verifyPointOrig(getSouthResults(), "lgs2gmos");
    }

    @Test public void testLgsP1PointOrig() throws Exception {
        addAltair(AltairParams.Mode.LGS_P1);
        addGuideStar(PwfsGuideProbe.pwfs1);
        verifyPointOrig(getSouthResults(), "lgs2gmos_p1");
    }

    @Test public void testSideLooking() throws Exception {
        setPort(IssPort.SIDE_LOOKING);
        verifyInstrumentConfig(getSouthResults(), "GMOS3");
    }

    @Test public void testSideLookingP1() throws Exception {
        setPort(IssPort.SIDE_LOOKING);
        addGuideStar(PwfsGuideProbe.pwfs1);
        verifyInstrumentConfig(getSouthResults(), "GMOS3_P1");
    }

    @Test public void testSideLookingP2() throws Exception {
        setPort(IssPort.SIDE_LOOKING);
        addGuideStar(PwfsGuideProbe.pwfs2);
        verifyInstrumentConfig(getSouthResults(), "GMOS3_P2");
    }

    @Test public void testSideLookingO1() throws Exception {
        setPort(IssPort.SIDE_LOOKING);
        addGuideStar(GmosOiwfsGuideProbe.instance);
        verifyInstrumentConfig(getSouthResults(), "GMOS3_OI");
    }

    @Test public void testSideLookingP1O1() throws Exception {
        setPort(IssPort.SIDE_LOOKING);
        addGuideStar(PwfsGuideProbe.pwfs1);
        addGuideStar(GmosOiwfsGuideProbe.instance);
        verifyInstrumentConfig(getSouthResults(), "GMOS3_P1_OI");
    }

    @Test public void testUpLooking() throws Exception {
        setPort(IssPort.UP_LOOKING);
        verifyInstrumentConfig(getSouthResults(), "GMOS");
    }

    @Test public void testUpLookingP2() throws Exception {
        setPort(IssPort.UP_LOOKING);
        addGuideStar(PwfsGuideProbe.pwfs2);
        verifyInstrumentConfig(getSouthResults(), "GMOS_P2");
    }
}
