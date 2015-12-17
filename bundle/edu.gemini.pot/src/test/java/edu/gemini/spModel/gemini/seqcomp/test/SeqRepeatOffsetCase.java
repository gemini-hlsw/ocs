package edu.gemini.spModel.gemini.seqcomp.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import edu.gemini.spModel.target.offset.OffsetPos;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffset;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.xml.PioXmlFactory;

/**
 * Class SeqOffsetTest tests the SeqOffset
 * OffsetPosList classes.
 */
public class SeqRepeatOffsetCase extends TestCase {
    private SeqRepeatOffset _i1;
    private OffsetPosList<OffsetPos> _l1;

    public SeqRepeatOffsetCase(String message) {
        super(message);
    }

    // No setup needed right now.
    protected void setUp()
            throws Exception {
        _i1 = new SeqRepeatOffset();
        assertNotNull(_i1);
        _l1 = _i1.getPosList();
        assertNotNull(_l1);

        OffsetPos op;
        op = _l1.addPosition(10, -10);
        assertNotNull(op);

        op = _l1.addPosition(20, -20);
        assertNotNull(op);

        op = _l1.addPosition(30.0, -30.0);
        assertNotNull(op);

        op = _l1.addPosition(40.0, -40.0);
        assertNotNull(op);

        op = _l1.addPosition(50.0, -50.0);
        assertNotNull(op);

        assertEquals(5, _l1.size());
    }

    /* Setup an av list with three attributes each with three values
     * Tests set/add/fetch
     */
    public void testSetup() {
        // Should have size 0
        assertEquals(5, _l1.size());
    }

    // Test paramset I/O both ways
    public void testParamSet() {
        ParamSet p = _i1.getParamSet(new PioXmlFactory());

        //  Now ingest the new map
        SeqRepeatOffset i2 = new SeqRepeatOffset();
        assertNotNull(i2);

        i2.setParamSet(p);

        OffsetPosList<OffsetPos> l1 = _i1.getPosList();
        OffsetPosList<OffsetPos> l2 = i2.getPosList();
        assertEquals("Two poslists equal", l1.toString(), l2.toString());
    }

    public static Test suite() {
        return new TestSuite(SeqRepeatOffsetCase.class);
    }

}
