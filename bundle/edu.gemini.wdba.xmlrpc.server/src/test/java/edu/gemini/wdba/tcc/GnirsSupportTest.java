//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.telescope.IssPort;
import org.junit.Test;

import static edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType.LGS;
import static edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType.NGS;

/**
 * Test cases for {@link edu.gemini.wdba.tcc.GNIRSSupport}.
 */
public final class GnirsSupportTest extends InstrumentSupportTestBase<InstGNIRS> {

    public GnirsSupportTest() {
        super(InstGNIRS.SP_TYPE);
    }

    @Test public void testGNIRS_DefaultPort() throws Exception {
        verifyInstrumentConfig(getNorthResults(), "GNIRS" + GNIRSSupport.GNIRS_SIDE_PORT);
    }

    @Test public void testGNIRS_SideLooking() throws Exception {
        setPort(IssPort.SIDE_LOOKING);
        verifyInstrumentConfig(getNorthResults(), "GNIRS" + GNIRSSupport.GNIRS_SIDE_PORT);
    }

    @Test public void testGNIRS_UpLooking() throws Exception {
        setPort(IssPort.UP_LOOKING);
        verifyInstrumentConfig(getNorthResults(), "GNIRS");
    }

    @Test public void testGNIRS_up_P2() throws Exception {
        setPort(IssPort.UP_LOOKING);
        addGuideStar(PwfsGuideProbe.pwfs2);
        verifyInstrumentConfig(getNorthResults(), "GNIRS_P2");
    }

    @Test public void testGNIRS_side_P2() throws Exception {
        setPort(IssPort.SIDE_LOOKING);
        addGuideStar(PwfsGuideProbe.pwfs2);
        verifyInstrumentConfig(getNorthResults(), "GNIRS" + GNIRSSupport.GNIRS_SIDE_PORT + "_P2");
    }

    @Test public void testGNIRS_up_P1() throws Exception {
        setPort(IssPort.UP_LOOKING);
        addGuideStar(PwfsGuideProbe.pwfs1);
        verifyInstrumentConfig(getNorthResults(), "GNIRS_P1");
    }

    @Test public void testGNIRS_side_P1() throws Exception {
        setPort(IssPort.SIDE_LOOKING);
        addGuideStar(PwfsGuideProbe.pwfs1);
        verifyInstrumentConfig(getNorthResults(), "GNIRS" + GNIRSSupport.GNIRS_SIDE_PORT + "_P1");
    }

    private void setup(AltairParams.GuideStarType type) throws Exception {
        // Add Altair to the observation.
        ISPObsComponent altair = odb.getFactory().createObsComponent(prog, InstAltair.SP_TYPE, null);

        InstAltair dataObj = (InstAltair) altair.getDataObject();
        if (type == AltairParams.GuideStarType.NGS) {
            dataObj.setMode(AltairParams.Mode.NGS);
        } else {
            dataObj.setMode(AltairParams.Mode.LGS);
        }
        altair.setDataObject(dataObj);

        obs.addObsComponent(altair);
    }

    @Test public void testNGS2GNIRS() throws Exception {
        // Add Altair to the observation.
        setup(AltairParams.GuideStarType.NGS);
        verifyInstrumentConfig(getNorthResults(), "AO2GNIRS" + GNIRSSupport.GNIRS_SIDE_PORT);
    }

    @Test public void testNGS2GNIRS_UpLooking() throws Exception {
        setPort(IssPort.UP_LOOKING);
        // Add Altair to the observation.
        setup(AltairParams.GuideStarType.NGS);
        verifyInstrumentConfig(getNorthResults(), "AO2GNIRS");
    }

    @Test public void testLGS2GNIRS() throws Exception {
        // Add Altair to the observation.
        setup(AltairParams.GuideStarType.LGS);
        verifyInstrumentConfig(getNorthResults(), "AO2GNIRS" + GNIRSSupport.GNIRS_SIDE_PORT);
    }

    @Test public void testLGS2GNIRS_UpLooking() throws Exception {
        setPort(IssPort.UP_LOOKING);
        // Add Altair to the observation.
        setup(AltairParams.GuideStarType.LGS);
        verifyInstrumentConfig(getNorthResults(), "AO2GNIRS");
    }

    @Test public void testNoAoPointOrig() throws Exception {
        verifyPointOrig(getSouthResults(), "gnirs");
    }

    @Test public void testNoAoPointOrigP1() throws Exception {
        addGuideStar(PwfsGuideProbe.pwfs1);
        verifyPointOrig(getSouthResults(), "gnirs_p1");
    }

    @Test public void testNoAoPointOrigP2() throws Exception {
        addGuideStar(PwfsGuideProbe.pwfs2);
        verifyPointOrig(getSouthResults(), "gnirs_p2");
    }

    @Test public void testLgsPointOrig() throws Exception {
        addAltair(LGS); verifyPointOrig(getSouthResults(), "lgs2gnirs");
    }

    @Test public void testLgsP1PointOrig() throws Exception {
        addAltair(AltairParams.Mode.LGS_P1);
        verifyPointOrig(getSouthResults(), "lgs2gnirs_p1");
    }

    @Test public void testNgsPointOrig() throws Exception {
        addAltair(NGS); verifyPointOrig(getSouthResults(), "ngs2gnirs");
    }

}
