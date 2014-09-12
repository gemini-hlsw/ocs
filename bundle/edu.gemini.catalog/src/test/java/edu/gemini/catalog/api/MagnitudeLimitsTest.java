package edu.gemini.catalog.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import edu.gemini.catalog.api.MagnitudeLimits.FaintnessLimit;
import edu.gemini.catalog.api.MagnitudeLimits.SaturationLimit;
import edu.gemini.shared.skyobject.Magnitude;
import static edu.gemini.shared.skyobject.Magnitude.Band.J;
import static edu.gemini.shared.skyobject.Magnitude.Band.R;

import org.junit.Test;

/**
 *
 */
public class MagnitudeLimitsTest {
    @Test public void testContains() {
        MagnitudeLimits ml = new MagnitudeLimits(R, new FaintnessLimit(10.0), new SaturationLimit(5.0));

        Magnitude[] bad = new Magnitude[] {
            new Magnitude(J, 7.0),     // Wrong Band
            new Magnitude(R, 4.9999),  // Too Bright
            new Magnitude(R, 10.001),  // Too Faint
        };
        for (Magnitude m : bad) assertFalse(m.toString(), ml.contains(m));

        Magnitude[] good = new Magnitude[] {
            new Magnitude(R,  5.0),
            new Magnitude(R,  7.0),
            new Magnitude(R, 10.0),
        };
        for (Magnitude m : good) assertTrue(m.toString(), ml.contains(m));
    }
}
