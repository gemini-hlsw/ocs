/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file LICENSE for complete details.
 *
 * $Id: InstAcqCamCase.java 7038 2006-05-17 14:24:43Z gillies $
 */
package edu.gemini.spModel.gemini.acqcam.test;

import edu.gemini.spModel.gemini.acqcam.AcqCamParams.Binning;
import edu.gemini.spModel.gemini.acqcam.AcqCamParams.ColorFilter;
import edu.gemini.spModel.gemini.acqcam.AcqCamParams.NDFilter;
import edu.gemini.spModel.gemini.acqcam.AcqCamParams.Windowing;
import edu.gemini.spModel.gemini.acqcam.InstAcqCam;
import static edu.gemini.spModel.test.TestFile.ser;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Class InstAcqCamTest tests the InstAcqCam class.
 */
public final class InstAcqCamCase {
    private InstAcqCam _t1;

    static final private double _ERROR = .00001;

    @Before
    public void setUp() throws Exception {
        _t1 = new InstAcqCam();
    }

    // Setup some test objects.
    @Test
    public void testInitial() {
        assertTrue(_t1.getColorFilter() == ColorFilter.DEFAULT);
        assertTrue(_t1.getNdFilter() == NDFilter.DEFAULT);
        assertTrue(_t1.getBinning() == Binning.DEFAULT);
        assertTrue(_t1.getWindowing() == Windowing.DEFAULT);
        assertTrue(_t1.getXStart() == InstAcqCam.DEF_X);
        assertTrue(_t1.getYStart() == InstAcqCam.DEF_Y);
        assertTrue(_t1.getXSize() == InstAcqCam.DEF_WIDTH);
        assertTrue(_t1.getYSize() == InstAcqCam.DEF_HEIGHT);
        //assertTrue(_t1.getOverscan() == Overscan.DEFAULT);
        assertEquals(InstAcqCam.DEF_EXPOSURE_TIME, _t1.getExposureTime(), _ERROR);
    }

    /**
     * Test cloneable
     */
    @Test
    public void testCloneable() {
        String title1 = "Initial AcqCam";
        // Give the data object a title
        InstAcqCam sq1 = _t1;
        assertNotNull(sq1);
        sq1.setTitle(title1);

        // Create change
        sq1.setColorFilter(ColorFilter.U_G0151);
        sq1.setNdFilter(NDFilter.ND001_G0156);

        assertTrue(sq1.getColorFilter() == ColorFilter.U_G0151);
        assertTrue(sq1.getNdFilter() == NDFilter.ND001_G0156);

        InstAcqCam sq2 = (InstAcqCam) sq1.clone();
        assertNotNull(sq2);
        sq2.setColorFilter(ColorFilter.B_G0152);

        assertTrue(sq2.getColorFilter() == ColorFilter.B_G0152);
        assertTrue(sq2.getNdFilter() == NDFilter.ND001_G0156);
        //assertTrue(sq2.getOverscan() == Overscan.DEFAULT);

        assertTrue(sq1.getColorFilter() == ColorFilter.U_G0151);
        assertTrue(sq1.getNdFilter() == NDFilter.ND001_G0156);
    }

    // Test serialization
    @Test
    public void testSerialization() throws Exception {
        InstAcqCam outObject = new InstAcqCam();
        assertNotNull(outObject);

        // Create change
        outObject.setColorFilter(ColorFilter.R_G0154);
        double expTime = 100.0;
        outObject.setExposureTime(expTime);
        double posAngle = 179.23;
        outObject.setPosAngleDegrees(posAngle);

        final InstAcqCam inObject = ser(outObject);
        assertSame("ColorFilter", ColorFilter.R_G0154, inObject.getColorFilter());
        assertSame("NDFilter", NDFilter.DEFAULT, inObject.getNdFilter());
        assertEquals("ETime", expTime, inObject.getExposureTime(), _ERROR);
        assertEquals("PAngle", posAngle, inObject.getPosAngleDegrees(), _ERROR);
    }
}
