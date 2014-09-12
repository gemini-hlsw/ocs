/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file COPYRIGHT for complete details.
 *
 * $Id: InstNIRICase.java 7030 2006-05-11 17:55:34Z shane $
 */
package edu.gemini.spModel.gemini.niri.test;

import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.niri.Niri.*;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Class InstNIRITest tests the InstNIRI class.
 */
public final class InstNIRICase {

    private InstNIRI _t1;

    static final private double _ERROR = .00001;

    @Before
    public void setUp() throws Exception {
        _t1 = new InstNIRI();
    }

    @Test
    public void testInitial() {
        assertTrue(_t1.getCamera() == Camera.DEFAULT);
        assertTrue(_t1.getDisperser() == Disperser.DEFAULT);
        assertTrue(_t1.getBeamSplitter() == BeamSplitter.DEFAULT);
        assertTrue(_t1.getReadMode() == ReadMode.DEFAULT);
        assertTrue(_t1.getMask() == Mask.DEFAULT);
        assertTrue(_t1.getFilter() == Filter.DEFAULT);
        assertEquals(InstNIRI.DEF_EXPOSURE_TIME, _t1.getExposureTime(),
                     _ERROR);
    }

    // Test the NIRI slit widths
    @Test
    public void testMaskWidths() {
        assertEquals("Mask None", Mask.Size.IMAGING_SIZE, Mask.MASK_IMAGING.getWidth(), _ERROR);
        assertEquals("Mask 1", Mask.Size.F6_2PIX_CENTERED_WIDTH, Mask.MASK_1.getWidth(), _ERROR);
        assertEquals("Mask 2", Mask.Size.F6_4PIX_CENTERED_WIDTH, Mask.MASK_2.getWidth(), _ERROR);
        assertEquals("Mask 3", Mask.Size.F6_6PIX_CENTERED_WIDTH, Mask.MASK_3.getWidth(), _ERROR);
        assertEquals("Mask 4", Mask.Size.F6_2PIX_BLUE_WIDTH, Mask.MASK_4.getWidth(), _ERROR);
        assertEquals("Mask 5", Mask.Size.F6_4PIX_BLUE_WIDTH, Mask.MASK_5.getWidth(), _ERROR);
        assertEquals("Mask 6", Mask.Size.F6_6PIX_BLUE_WIDTH, Mask.MASK_6.getWidth(), _ERROR);
    }

    // Test the science area method
    @Test
    public void testScienceArea() {
        // First test the imaging mode with various beamsplitter and camera
        // combinations.  Note 0 is width, 1 is length/height

        _t1.setMask(Mask.MASK_IMAGING);

        // BS and C both set to F6
        _t1.setCamera(Camera.F6);
        _t1.setBeamSplitter(BeamSplitter.f6);
        double expected[] = new double[2];
        expected[0] = Camera.Size.F6;
        expected[1] = Camera.Size.F6;
        double result[] = _t1.getScienceArea();
        assertEquals("I1w", expected[0], result[0], _ERROR);
        assertEquals("I1h", expected[1], result[1], _ERROR);

        // BS to F14 should be F14 size
        _t1.setBeamSplitter(BeamSplitter.f14);
        expected[0] = Camera.Size.F14;
        expected[1] = Camera.Size.F14;
        result = _t1.getScienceArea();
        assertEquals("I2w", expected[0], result[0], _ERROR);
        assertEquals("I2h", expected[1], result[1], _ERROR);

        // BS F32 should be F32 size
        _t1.setBeamSplitter(BeamSplitter.f32);
        expected[0] = Camera.Size.F32;
        expected[1] = Camera.Size.F32;
        result = _t1.getScienceArea();
        assertEquals("I3w", expected[0], result[0], _ERROR);
        assertEquals("I3h", expected[1], result[1], _ERROR);

        // Now Camera to F14
        _t1.setCamera(Camera.F14);
        // BS to F6 should be F14 size
        _t1.setBeamSplitter(BeamSplitter.f6);
        expected[0] = Camera.Size.F14;
        expected[1] = Camera.Size.F14;
        result = _t1.getScienceArea();
        assertEquals("I4w", expected[0], result[0], _ERROR);
        assertEquals("I4h", expected[1], result[1], _ERROR);

        // BS F14 should be F14 size
        _t1.setBeamSplitter(BeamSplitter.f14);
        expected[0] = Camera.Size.F14;
        expected[1] = Camera.Size.F14;
        result = _t1.getScienceArea();
        assertEquals("I5w", expected[0], result[0], _ERROR);
        assertEquals("I5h", expected[1], result[1], _ERROR);

        // Now BS to F32 should be F32 size
        _t1.setBeamSplitter(BeamSplitter.f32);
        expected[0] = Camera.Size.F32;
        expected[1] = Camera.Size.F32;
        result = _t1.getScienceArea();
        assertEquals("I6w", expected[0], result[0], _ERROR);
        assertEquals("I6h", expected[1], result[1], _ERROR);

        // Now Camera to F32
        _t1.setCamera(Camera.F14);
        // BS to F6 should be F14 size
        _t1.setBeamSplitter(BeamSplitter.f6);
        expected[0] = Camera.Size.F14;
        expected[1] = Camera.Size.F14;
        result = _t1.getScienceArea();
        assertEquals("I4w", expected[0], result[0], _ERROR);
        assertEquals("I4h", expected[1], result[1], _ERROR);

        // BS F14 should be F14 size
        _t1.setBeamSplitter(BeamSplitter.f14);
        expected[0] = Camera.Size.F14;
        expected[1] = Camera.Size.F14;
        result = _t1.getScienceArea();
        assertEquals("I5w", expected[0], result[0], _ERROR);
        assertEquals("I5h", expected[1], result[1], _ERROR);

        // Now BS to F32 should be F32 size
        _t1.setBeamSplitter(BeamSplitter.f32);
        expected[0] = Camera.Size.F32;
        expected[1] = Camera.Size.F32;
        result = _t1.getScienceArea();
        assertEquals("I6w", expected[0], result[0], _ERROR);
        assertEquals("I6h", expected[1], result[1], _ERROR);

        // Now on to spectroscopy
        _t1.setMask(Mask.MASK_IMAGING);

        // First with F6 bs and c and mask 1 since we've tested the height
        // with previous tests only test the width
        // Note that the mask height is limiting the height in some cases
        _t1.setCamera(Camera.F6);
        _t1.setBeamSplitter(BeamSplitter.f6);
        _t1.setMask(Mask.MASK_1);
        expected[0] = Mask.Size.F6_2PIX_CENTERED_WIDTH;
        expected[1] = Mask.MASK_1.getHeight(_t1.getBuiltinROI().getROIDescription());
        result = _t1.getScienceArea();
        assertEquals("S1w", expected[0], result[0], _ERROR);
        assertEquals("S1h", expected[1], result[1], _ERROR);

        _t1.setMask(Mask.MASK_2);
        expected[0] = Mask.Size.F6_4PIX_CENTERED_WIDTH;
        expected[1] = Mask.MASK_2.getHeight(_t1.getBuiltinROI().getROIDescription());
        result = _t1.getScienceArea();
        assertEquals("S2w", expected[0], result[0], _ERROR);
        assertEquals("S2h", expected[1], result[1], _ERROR);

        // Set the Camera or beamsplitter to smaller
        _t1.setMask(Mask.MASK_3);
        _t1.setBeamSplitter(BeamSplitter.f32);
        expected[0] = Mask.Size.F6_6PIX_CENTERED_WIDTH;
        expected[1] = Camera.Size.F32;
        result = _t1.getScienceArea();
        assertEquals("S3w", expected[0], result[0], _ERROR);
        assertEquals("S3h", expected[1], result[1], _ERROR);

        _t1.setMask(Mask.MASK_4);
        expected[0] = Mask.Size.F6_2PIX_BLUE_WIDTH;
        expected[1] = Camera.Size.F32;
        result = _t1.getScienceArea();
        assertEquals("S4w", expected[0], result[0], _ERROR);
        assertEquals("S4h", expected[1], result[1], _ERROR);

        // Set the beamsplitter back
        _t1.setMask(Mask.MASK_5);
        _t1.setBeamSplitter(BeamSplitter.same_as_camera);
        expected[0] = Mask.Size.F6_4PIX_BLUE_WIDTH;
        expected[1] = Mask.MASK_5.getHeight(_t1.getBuiltinROI().getROIDescription());
        result = _t1.getScienceArea();
        assertEquals("S5w", expected[0], result[0], _ERROR);
        assertEquals("S5h", expected[1], result[1], _ERROR);

        _t1.setMask(Mask.MASK_6);
        expected[0] = Mask.Size.F6_6PIX_BLUE_WIDTH;
        expected[1] = Mask.MASK_6.getHeight(_t1.getBuiltinROI().getROIDescription());
        result = _t1.getScienceArea();
        assertEquals("S6w", expected[0], result[0], _ERROR);
        assertEquals("S6h", expected[1], result[1], _ERROR);

        // Just to be sure
        _t1.setCamera(Camera.F6);
        _t1.setBeamSplitter(BeamSplitter.f14);
        _t1.setMask(Mask.MASK_1);
        expected[0] = Mask.Size.F6_2PIX_CENTERED_WIDTH;
        expected[1] = Camera.Size.F14;
        result = _t1.getScienceArea();
        assertEquals("ex1w", expected[0], result[0], _ERROR);
        assertEquals("ex1h", expected[1], result[1], _ERROR);

        // Check use of same as camera
        _t1.setCamera(Camera.F6);
        _t1.setMask(Mask.MASK_1);
        _t1.setBeamSplitter(BeamSplitter.same_as_camera);
        expected[0] = Mask.Size.F6_2PIX_CENTERED_WIDTH;
        expected[1] = Mask.MASK_1.getHeight(_t1.getBuiltinROI().getROIDescription());
        result = _t1.getScienceArea();
        assertEquals("ex2w", expected[0], result[0], _ERROR);
        assertEquals("ex2h", expected[1], result[1], _ERROR);

        _t1.setCamera(Camera.F32);
        _t1.setMask(Mask.MASK_3);
        _t1.setBeamSplitter(BeamSplitter.same_as_camera);
        expected[0] = Mask.Size.F6_6PIX_CENTERED_WIDTH;
        expected[1] = Camera.Size.F32;
        result = _t1.getScienceArea();
        assertEquals("ex3w", expected[0], result[0], _ERROR);
        assertEquals("ex3h", expected[1], result[1], _ERROR);
    }

    /**
     * Test cloneable
     */
    @Test
    public void testCloneable() {
        String title1 = "Initial NIRI";
        // Give the data object a title
        InstNIRI sq1 = _t1;
        assertNotNull(sq1);
        sq1.setTitle(title1);

        // Create change
        sq1.setDisperser(Disperser.L);
        sq1.setFilter(Filter.BBF_KSHORT);

        assertTrue(sq1.getDisperser() == Disperser.L);
        assertTrue(sq1.getFilter() == Filter.BBF_KSHORT);
        assertTrue(sq1.getMask() == Mask.DEFAULT);
        assertTrue(sq1.getCamera() == Camera.DEFAULT);
        assertTrue(sq1.getBeamSplitter() == BeamSplitter.DEFAULT);

        InstNIRI sq2 = (InstNIRI) sq1.clone();
        assertNotNull(sq2);
        sq2.setMask(Mask.MASK_4);
        sq2.setCamera(Camera.F32);
        sq2.setBeamSplitter(BeamSplitter.f32);

        assertTrue(sq2.getDisperser() == Disperser.L);
        assertTrue(sq2.getFilter() == Filter.BBF_KSHORT);
        assertTrue(sq2.getMask() == Mask.MASK_4);
        assertTrue(sq2.getCamera() == Camera.F32);
        assertTrue(sq2.getBeamSplitter() == BeamSplitter.f32);

        assertTrue(sq1.getDisperser() == Disperser.L);
        assertTrue(sq1.getFilter() == Filter.BBF_KSHORT);
        assertTrue(sq1.getMask() == Mask.DEFAULT);
        assertTrue(sq1.getCamera() == Camera.DEFAULT);
    }

    @Test
    public void testSerialization() throws Exception {
        final InstNIRI outObject = new InstNIRI();

        // Create change
        outObject.setDisperser(Disperser.L);
        outObject.setMask(Mask.MASK_4);
        outObject.setFilter(Filter.NBF_KCONT1);
        double expTime = 100.0;
        outObject.setExposureTime(expTime);
        double posAngle = 179.23;
        outObject.setPosAngleDegrees(posAngle);

        final InstNIRI inObject = ser(outObject);
        assertSame("Disperser", Disperser.L, inObject.getDisperser());
        assertSame("Mask", Mask.MASK_4, inObject.getMask());
        assertSame("Filter", Filter.NBF_KCONT1, inObject.getFilter());
        assertSame("Camera", Camera.DEFAULT, inObject.getCamera());
        assertEquals("ETime", expTime, inObject.getExposureTime(), _ERROR);
        assertEquals("PAngle", posAngle, inObject.getPosAngleDegrees(), _ERROR);
    }
}
