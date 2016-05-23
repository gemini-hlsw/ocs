//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.visitor.VisitorInstrument;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test cases for {@link edu.gemini.wdba.tcc.VisitorInstrumentSupport}.
 */
public final class VisitorInstrumentSupportTest extends InstrumentSupportTestBase<VisitorInstrument> {

    public VisitorInstrumentSupportTest() {
        super(VisitorInstrument.SP_TYPE);
    }

    @Test public void testName() throws Exception {
        VisitorInstrument visitor = getInstrument();
        setInstrument(visitor);

        verifyInstrumentConfig(getSouthResults(), "VISITOR");
    }

    @Test public void testChopState() throws Exception {
        VisitorInstrument visitor = getInstrument();
        setInstrument(visitor);

        verifyInstrumentChopConfig(getSouthResults(), "NoChop");
    }

    @Test public void testPointOrigin() throws Exception {
        VisitorInstrument visitor = getInstrument();
        setInstrument(visitor);

        verifyPointOrig(getSouthResults(), "visitor");
    }

    @Test public void testWavelength() throws Exception {
        VisitorInstrument visitor = getInstrument();
        setInstrument(visitor);

        assertEquals(getWavelength(getSouthResults()), "0.7");
    }

}
