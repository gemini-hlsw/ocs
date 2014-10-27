//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.telescope.IssPort;

import static edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType.LGS;
import static edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType.NGS;

/**
 * Test cases for {@link edu.gemini.wdba.tcc.GNIRSSupport}.
 */
public final class GnirsSupportTest extends InstrumentSupportTestBase<InstGNIRS> {

    public GnirsSupportTest() {
        super(InstGNIRS.SP_TYPE);
    }

    public void testGNIRS_DefaultPort() throws Exception {
        verifyInstrumentConfig(getNorthResults(), "GNIRS" + GNIRSSupport.GNIRS_SIDE_PORT);
    }

    public void testGNIRS_SideLooking() throws Exception {
        setPort(IssPort.SIDE_LOOKING);
        verifyInstrumentConfig(getNorthResults(), "GNIRS" + GNIRSSupport.GNIRS_SIDE_PORT);
    }

    public void testGNIRS_UpLooking() throws Exception {
        setPort(IssPort.UP_LOOKING);
        verifyInstrumentConfig(getNorthResults(), "GNIRS");
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

    public void testNGS2GNIRS() throws Exception {
        // Add Altair to the observation.
        setup(AltairParams.GuideStarType.NGS);
        verifyInstrumentConfig(getNorthResults(), "AO2GNIRS" + GNIRSSupport.GNIRS_SIDE_PORT);
    }

    public void testNGS2GNIRS_UpLooking() throws Exception {
        setPort(IssPort.UP_LOOKING);
        // Add Altair to the observation.
        setup(AltairParams.GuideStarType.NGS);
        verifyInstrumentConfig(getNorthResults(), "AO2GNIRS");
    }

    public void testLGS2GNIRS() throws Exception {
        // Add Altair to the observation.
        setup(AltairParams.GuideStarType.LGS);
        verifyInstrumentConfig(getNorthResults(), "AO2GNIRS" + GNIRSSupport.GNIRS_SIDE_PORT);
    }

    public void testLGS2GNIRS_UpLooking() throws Exception {
        setPort(IssPort.UP_LOOKING);
        // Add Altair to the observation.
        setup(AltairParams.GuideStarType.LGS);
        verifyInstrumentConfig(getNorthResults(), "AO2GNIRS");
    }

    public void testNoAoPointOrig() throws Exception {
        verifyPointOrig(getSouthResults(), "gnirs");
    }

    public void testLgsPointOrig() throws Exception {
        addAltair(LGS); verifyPointOrig(getSouthResults(), "lgs2gnirs");
    }

    public void testLgsP1PointOrig() throws Exception {
        addAltair(AltairParams.Mode.LGS_P1);
        verifyPointOrig(getSouthResults(), "lgs2gnirs_p1");
    }

    public void testNgsPointOrig() throws Exception {
        addAltair(NGS); verifyPointOrig(getSouthResults(), "ngs2gnirs");
    }

}