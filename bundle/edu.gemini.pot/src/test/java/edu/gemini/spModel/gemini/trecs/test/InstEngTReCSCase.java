package edu.gemini.spModel.gemini.trecs.test;

import edu.gemini.spModel.gemini.trecs.TReCSParams.*;
import edu.gemini.spModel.gemini.trecs.InstEngTReCS;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

//
// Gemini Observatory/AURA
// $Id: InstEngTReCSCase.java 7051 2006-05-24 21:35:14Z anunez $
//

/**
 * InstEngTReCSTest tests the engineering data model
 */
public final class InstEngTReCSCase {

    private InstEngTReCS _t1;

//    static final private double _ERROR = .00001;

    @Before
    public void setUp() {
        _t1 = new InstEngTReCS();
    }

    // Setup some test objects.
    @Test
    public void testInitial() {
        assertEquals(SectorWheel.DEFAULT, _t1.getSectorWheel());
        assertEquals(LyotWheel.DEFAULT, _t1.getLyotWheel());
        assertEquals(PupilImagingWheel.DEFAULT, _t1.getPupilImagingWheel());
        assertEquals(ApertureWheel.DEFAULT, _t1.getApertureWheel());
    }

    // Test serialization
    @Test
    public void testSerialization() throws Exception {
        final InstEngTReCS outObject = new InstEngTReCS();

        // Create change
        SectorWheel sw = SectorWheel.POLY_115;
        outObject.setSectorWheel(sw);

        LyotWheel lw = LyotWheel.CIARDI;
        outObject.setLyotWheel(lw);

        PupilImagingWheel piw = PupilImagingWheel.OPEN_3;
        outObject.setPupilImagingWheel(piw);

        ApertureWheel aw = ApertureWheel.SPOT_MASK;
        outObject.setApertureWheel(aw);

        final InstEngTReCS inObject = ser(outObject);

        assertEquals("Sector Wheel", sw, inObject.getSectorWheel());
        assertEquals("Lyot Wheel", lw, inObject.getLyotWheel());
        assertEquals("PupilImagingWheel", piw, inObject.getPupilImagingWheel());
        assertEquals("Aperture Wheel", aw, inObject.getApertureWheel());
    }
}
