//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.nici.InstNICI;
import edu.gemini.spModel.telescope.IssPort;

import org.junit.Test;

/**
 * Test cases for {@link edu.gemini.wdba.tcc.Flamingos2Support}.
 */
public final class NiciSupportTest extends InstrumentSupportTestBase<InstNICI> {

    public NiciSupportTest() {
        super(InstNICI.SP_TYPE);
    }

    @Test public void testNICI5() throws Exception {
        InstNICI nici = getInstrument();
        nici.setIssPort(IssPort.SIDE_LOOKING);
        setInstrument(nici);

        verifyInstrumentConfig(getSouthResults(), "NICI5");
    }

    @Test public void testNICI1() throws Exception {
        InstNICI nici = getInstrument();
        nici.setIssPort(IssPort.UP_LOOKING);
        setInstrument(nici);

        verifyInstrumentConfig(getSouthResults(), "NICI");
    }
}
