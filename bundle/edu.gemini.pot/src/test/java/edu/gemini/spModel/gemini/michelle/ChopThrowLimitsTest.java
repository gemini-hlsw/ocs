//
// $
//

package edu.gemini.spModel.gemini.michelle;

import edu.gemini.spModel.gemini.michelle.MichelleParams.Disperser;
import static edu.gemini.spModel.gemini.michelle.MichelleParams.DisperserMode.*;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the relationship between the disperser and the "chop throw" limits.
 */
public final class ChopThrowLimitsTest extends TestCase {
    private InstMichelle michelle;
    private List<Disperser> chopDispersers;
    private List<Disperser> nodDispersers;

    private static final double MAX_MINUS = InstMichelle.MAX_CHOP_THROW - 1.0;
    private static final double MAX       = InstMichelle.MAX_CHOP_THROW;
    private static final double MAX_PLUS  = InstMichelle.MAX_CHOP_THROW + 1.0;

    @Override
    public void setUp() throws Exception {
        michelle = new InstMichelle();

        chopDispersers = new ArrayList<Disperser>();
        nodDispersers  = new ArrayList<Disperser>();

        for (Disperser d : Disperser.values()) {
            if (d.getMode() == CHOP) {
                chopDispersers.add(d);
            } else {
                nodDispersers.add(d);
            }
        }
    }

    public void testNothing() {
        
    }


    // disabled for now
    public void disabled_testSetChopThrow() throws Exception {
        for (Disperser d : MichelleParams.Disperser.values()) {
            michelle.setDisperser(d);

            michelle.setChopThrow(MAX_MINUS);
            assertEquals(MAX_MINUS, michelle.getChopThrow());

            michelle.setChopThrow(-MAX_MINUS);
            assertEquals(MAX_MINUS, michelle.getChopThrow());

            michelle.setChopThrow(MAX);
            assertEquals(MAX, michelle.getChopThrow());

            michelle.setChopThrow(-MAX);
            assertEquals(MAX, michelle.getChopThrow());

            michelle.setChopThrow(MAX_PLUS);
            if (d.getMode() == CHOP) {
                // sticks at MAX if chop mode
                assertEquals(MAX, michelle.getChopThrow());
            } else {
                // respects value if NOD
                assertEquals(MAX_PLUS, michelle.getChopThrow());
            }

            michelle.setChopThrow(-MAX_PLUS);
            if (d.getMode() == CHOP) {
                // sticks at MAX if chop mode
                assertEquals(MAX, michelle.getChopThrow());
            } else {
                // respects value if NOD
                assertEquals(MAX_PLUS, michelle.getChopThrow());
            }
        }
    }

    // disabled
    public void disabled_testSetDisperser() throws Exception {
        assertEquals(CHOP, Disperser.LOW_RES_10.getMode());
        assertEquals(NOD,  Disperser.MED_RES.getMode());

        // Move from a chopping disperser to a nodding disperser.
        // No change to chop throw.
        michelle.setDisperser(Disperser.LOW_RES_10);
        michelle.setChopThrow(MAX_MINUS);
        michelle.setDisperser(Disperser.MED_RES);
        assertEquals(MAX_MINUS, michelle.getChopThrow());

        // Move from a nodding disperser to a chopping disperser.
        // Adjust chop throw when necessary
        michelle.setDisperser(Disperser.MED_RES);
        michelle.setChopThrow(MAX_PLUS);
        assertEquals(MAX_PLUS, michelle.getChopThrow());

        michelle.setDisperser(Disperser.LOW_RES_10);
        assertEquals(MAX, michelle.getChopThrow());

        // Leave chop throw alone when not necessary.
        michelle.setDisperser(Disperser.MED_RES);
        michelle.setChopThrow(MAX);
        assertEquals(MAX, michelle.getChopThrow());

        michelle.setDisperser(Disperser.LOW_RES_10);
        assertEquals(MAX, michelle.getChopThrow());
    }
}
