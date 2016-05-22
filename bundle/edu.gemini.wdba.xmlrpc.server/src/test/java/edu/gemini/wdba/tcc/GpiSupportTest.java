//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.gpi.Gpi;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test cases for {@link GpiSupport}.
 */
public final class GpiSupportTest extends InstrumentSupportTestBase<Gpi> {

    public GpiSupportTest() {
        super(Gpi.SP_TYPE);
    }

    @Test public void testName() throws Exception {
        Gpi gpi = getInstrument();
        setInstrument(gpi);

        verifyInstrumentConfig(getSouthResults(), "GPI");
    }

    @Test public void testChopState() throws Exception {
        Gpi gpi = getInstrument();
        setInstrument(gpi);

        verifyInstrumentChopConfig(getSouthResults(), "NoChop");
    }

    @Test public void testPointOrigin() throws Exception {
        Gpi gpi = getInstrument();
        gpi.setAdc(Gpi.Adc.IN);
        setInstrument(gpi);

        verifyPointOrig(getSouthResults(), "gpi_adc");

        gpi.setAdc(Gpi.Adc.OUT);
        setInstrument(gpi);

        verifyPointOrig(getSouthResults(), "gpi");
    }

    @Test public void testWavelength() throws Exception {
        Gpi gpi = getInstrument();
        setInstrument(gpi);

        assertEquals(getWavelength(getSouthResults()), "0.806");
    }

}
