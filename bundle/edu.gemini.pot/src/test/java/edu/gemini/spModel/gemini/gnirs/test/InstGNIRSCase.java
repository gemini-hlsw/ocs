/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file LICENSE for complete details.
 *
 * $Id: InstGNIRSCase.java 7049 2006-05-18 21:22:33Z gillies $
 */
package edu.gemini.spModel.gemini.gnirs.test;


import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.gnirs.GNIRSConstants;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.*;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.xml.PioXmlFactory;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

/**
 * Class InstGNIRSTest tests the InstGNIRS class.
 */
public class InstGNIRSCase extends TestCase {

//    static final private double _ERROR = .00001;

    public InstGNIRSCase(String message) {
        super(message);
    }


    // Setup some test objects.
    public void testInitial() {
        InstGNIRS inst = new InstGNIRS();
        assertEquals(inst.getPixelScale(), PixelScale.DEFAULT);
        assertEquals(inst.getDisperser(), Disperser.DEFAULT);
        assertEquals(inst.getSlitWidth(), SlitWidth.DEFAULT);
        assertEquals(inst.getCrossDispersed(), CrossDispersed.DEFAULT);
        assertEquals(inst.getWollastonPrism(), WollastonPrism.DEFAULT);
        assertEquals(inst.getReadMode(), ReadMode.DEFAULT);

        assertTrue(inst.getExposureTime() == GNIRSConstants.DEF_EXPOSURE_TIME);
        assertTrue(inst.getCoadds() == GNIRSConstants.DEF_COADDS);
       // assertTrue(inst.getCentralWavelength() == GNIRSConstants.DEF_CENTRAL_WAVELENGTH);
        assertTrue(inst.getPosAngleDegrees() == GNIRSConstants.DEF_POS_ANGLE);
    }


    // test I/O
    public void testIO() {
        InstGNIRS inst = new InstGNIRS();
        inst.setPixelScale(PixelScale.PS_005);
        inst.setDisperser(Disperser.D_32);
        inst.setWollastonPrism(WollastonPrism.YES);

        // test get/set paramset
        InstGNIRS copy = new InstGNIRS();
        ParamSet p = inst.getParamSet(new PioXmlFactory());

        copy = new InstGNIRS();
        copy.setParamSet(p);

        assertEquals(copy.getPixelScale(), PixelScale.PS_005);
        assertEquals(copy.getPixelScale(), inst.getPixelScale());

        assertEquals(copy.getDisperser(), Disperser.D_32);
        assertEquals(copy.getDisperser(), inst.getDisperser());

        assertEquals(copy.getWollastonPrism(), WollastonPrism.YES);
        assertEquals(copy.getWollastonPrism(), inst.getWollastonPrism());
    }


    public static Test suite() {
        TestSuite suite = new TestSuite(InstGNIRSCase.class);
        return suite;
    }
}
