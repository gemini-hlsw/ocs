/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file LICENSE for complete details.
 *
 * $Id: TReCSParamsCase.java 7051 2006-05-24 21:35:14Z anunez $
 */

package edu.gemini.spModel.gemini.trecs.test;

import edu.gemini.spModel.gemini.trecs.TReCSParams.*;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests TReCSParams.
 */
public final class TReCSParamsCase {

    static final private double _ERROR = .00001;

    // Setup some test objects.
    @Test
    public void testInitial() {
        final InstTReCS inst = new InstTReCS();

        assertTrue(inst.getDisperser() == Disperser.DEFAULT);
        assertTrue(inst.getDisperserLambda() == InstTReCS.DEFAULT_LAMBDA);
    }


    @Test
    public void testSerialization() throws Exception {
        final InstTReCS inst = new InstTReCS();

        double wavelength = 6000.0;
        inst.setDisperser(Disperser.LOW_RES_10);
        inst.setDisperserLambda(wavelength);

        final InstTReCS inObject = ser(inst);
        assertSame("Disperser", Disperser.LOW_RES_10, inObject.getDisperser());
        assertEquals("Wavelength", wavelength, inObject.getDisperserLambda(), _ERROR);

    }
}
