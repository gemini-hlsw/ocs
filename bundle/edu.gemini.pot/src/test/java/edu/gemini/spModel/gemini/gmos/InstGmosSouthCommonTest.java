package edu.gemini.spModel.gemini.gmos;

import junit.framework.TestCase;

/**
 * Initially implemented in order to place the behavior of the old gain resolution under test and
 * help prepare for the switchover between E2V and Hamamatsu CCDs.
 *
 * @author ddawson
 */
public class InstGmosSouthCommonTest extends TestCase {
    public void testGetActualGain() {
        final int actualGainHighFast = InstGmosSouth.getActualGain(GmosCommonType.AmpGain.HIGH,
                GmosCommonType.AmpReadMode.FAST, GmosCommonType.DetectorManufacturer.E2V);
        final int actualGainLowFast = InstGmosSouth.getActualGain(GmosCommonType.AmpGain.LOW,
                GmosCommonType.AmpReadMode.FAST, GmosCommonType.DetectorManufacturer.E2V);
        final int actualGainHighSlow = InstGmosSouth.getActualGain(GmosCommonType.AmpGain.HIGH,
                GmosCommonType.AmpReadMode.SLOW, GmosCommonType.DetectorManufacturer.E2V);
        final int actualGainLowSlow = InstGmosSouth.getActualGain(GmosCommonType.AmpGain.LOW,
                GmosCommonType.AmpReadMode.SLOW, GmosCommonType.DetectorManufacturer.E2V);

        assertEquals("Problem calculating actual gain.", actualGainHighFast, 5);
        assertEquals("Problem calculating actual gain.", actualGainLowFast, 6);
        assertEquals("Problem calculating actual gain.", actualGainHighSlow, 1);
        assertEquals("Problem calculating actual gain.", actualGainLowSlow, 2);
    }
}
