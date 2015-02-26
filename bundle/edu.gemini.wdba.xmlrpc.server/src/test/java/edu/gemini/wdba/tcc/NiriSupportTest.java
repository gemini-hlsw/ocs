//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.niri.Niri.Camera;

import edu.gemini.spModel.gemini.niri.InstNIRI;
import org.junit.Test;

import static edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType.LGS;
import static edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType.NGS;

/**
 * Test cases for NIFS instrument support.
 */
public class NiriSupportTest extends InstrumentSupportTestBase<InstNIRI> {
    public NiriSupportTest() {
        super(InstNIRI.SP_TYPE);
    }

    private void verify(String... expected) throws Exception {
        int i=0;
        for (Camera c : Camera.values()) {
            InstNIRI niri = getInstrument();
            niri.setCamera(c);
            setInstrument(niri);
            verifyPointOrig(getSouthResults(), expected[i++]);
        }
    }

    @Test public void testNoAoPointOrig() throws Exception {
        verify("nirif6p", "nirif14p", "nirif32p", "unknown");
    }

    @Test public void testLgsPointOrig() throws Exception {
        addAltair(LGS); verify("unknown", "lgs2niri_f14", "lgs2niri_f32", "unknown");
    }

    @Test public void testLgsP1PointOrig() throws Exception {
        addAltair(AltairParams.Mode.LGS_P1); verify("unknown", "lgs2niri_f14_p1", "lgs2niri_f32_p1", "unknown");
    }

    @Test public void testNgsPointOrig() throws Exception {
        addAltair(NGS); verify("unknown", "ngs2niri_f14", "ngs2niri_f32", "unknown");
    }
}
