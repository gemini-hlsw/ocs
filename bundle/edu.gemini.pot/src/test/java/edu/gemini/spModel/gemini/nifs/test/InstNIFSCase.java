/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file LICENSE for complete details.
 *
 * $Id: InstNIFSCase.java 7063 2006-05-25 16:17:10Z anunez $
 */
package edu.gemini.spModel.gemini.nifs.test;

import edu.gemini.spModel.gemini.nifs.NIFSParams.*;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


/**
 * Class InstNIFSTest tests the InstNIFS class.
 */
public final class InstNIFSCase {

    private InstNIFS _t1;

    static final private double _ERROR = .00001;

    @Before
    public void setUp() throws Exception {
        _t1 = new InstNIFS();
    }

    @Test
    public void testInitial() {
        assertTrue(_t1.getDisperser() == Disperser.DEFAULT);
        assertTrue(_t1.getReadMode() == ReadMode.DEFAULT);
        assertTrue(_t1.getMask() == Mask.DEFAULT);
        assertTrue(_t1.getFilter() == Filter.DEFAULT);
        assertTrue(_t1.getImagingMirror() == ImagingMirror.DEFAULT);
        assertEquals(_t1.getExposureTime(), _t1.getReadMode().getMinExp(), _ERROR);
    }

    @Test
    public void testSerialization() throws Exception {
        final InstNIFS outObject = new InstNIFS();

        // Create change
        outObject.setDisperser(Disperser.H);
        outObject.setMask(Mask.KG3_ND_FILTER);
        outObject.setFilter(Filter.JH_FILTER);
        double expTime = 100.0;
        outObject.setExposureTime(expTime);
        double posAngle = 179.23;
        outObject.setPosAngleDegrees(posAngle);

        final InstNIFS inObject = ser(outObject);

        assertSame("Disperser", Disperser.H, inObject.getDisperser());
        assertSame("Mask", Mask.KG3_ND_FILTER, inObject.getMask());
        assertSame("Filter", Filter.JH_FILTER, inObject.getFilter());
        assertEquals("ETime", expTime, inObject.getExposureTime(), _ERROR);
        assertEquals("PAngle", posAngle, inObject.getPosAngleDegrees(), _ERROR);
    }
}
