//
// $
//

package edu.gemini.wdba.tcc;

import static edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType.*;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.nifs.NIFSParams;
import org.junit.Test;

/**
 * Test cases for NIFS instrument support.
 */
public class NifsSupportTest extends InstrumentSupportTestBase<InstNIFS> {
    public NifsSupportTest() {
        super(InstNIFS.SP_TYPE);
    }

    @Test public void testNoAoPointOrig() throws Exception {
        verifyPointOrig(getSouthResults(), "nifs");
    }

    @Test public void testLgsPointOrig() throws Exception {
        addAltair(LGS); verifyPointOrig(getSouthResults(), "lgs2nifs");
    }

    @Test public void testNgsPointOrig() throws Exception {
        addAltair(NGS); verifyPointOrig(getSouthResults(), "ngs2nifs");
    }

    @Test public void testWavelength() throws Exception {
        InstNIFS nifs = getInstrument();
        nifs.setDisperser(NIFSParams.Disperser.MIRROR);
        nifs.setFilter(NIFSParams.Filter.JH_FILTER);
        setInstrument(nifs);

        assertEquals(NIFSParams.Filter.JH_FILTER.getWavelength(), getWavelength(getSouthResults()));
    }

    private void verifyDefaultWavelength() throws Exception {
        String exp = String.valueOf(InstNIFS.DEF_CENTRAL_WAVELENGTH);
        assertEquals(exp, getWavelength(getSouthResults()));
    }

    @Test public void testWavelengthWithNulls() throws Exception {
        // Null Disperser
        InstNIFS nifs = getInstrument();
        nifs.setDisperser(null);
        nifs.setFilter(NIFSParams.Filter.JH_FILTER);
        setInstrument(nifs);
        verifyDefaultWavelength();

        // Null Filter
        nifs.setDisperser(NIFSParams.Disperser.MIRROR);
        nifs.setFilter((NIFSParams.Filter) null);
        setInstrument(nifs);
        verifyDefaultWavelength();
    }

    @Test public void testMirrorWithFilterSameAsDisperser() throws Exception {
        InstNIFS nifs = getInstrument();
        nifs.setDisperser(NIFSParams.Disperser.MIRROR);
        nifs.setFilter(NIFSParams.Filter.SAME_AS_DISPERSER);
        setInstrument(nifs);
        verifyDefaultWavelength();
    }
}
