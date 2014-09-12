package edu.gemini.spModel.gemini.obscomp;

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions;
import static edu.gemini.spModel.gemini.obscomp.SPSiteQuality.ImageQuality.PERCENT_70;
import static edu.gemini.spModel.gemini.obscomp.SPSiteQuality.WaterVapor.PERCENT_80;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SPSiteQualityTest {
    @Test public void testToString() {
        assertEquals("CC20, IQ20, SB20, WV20", Conditions.BEST.toString());
        assertEquals("CCAny, IQAny, SBAny, WVAny", Conditions.WORST.toString());
        assertEquals("CCAny, IQ70, SBAny, WV80", Conditions.WORST.iq(PERCENT_70).wv(PERCENT_80).toString());
    }
}
