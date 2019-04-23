// Copyright 1997-2002
// Association for Universities for Research in Astronomy, Inc.
//
// $Id: InstPhoenixCase.java 7084 2006-05-29 13:36:02Z anunez $
//
package edu.gemini.spModel.gemini.phoenix.test;

import edu.gemini.spModel.gemini.phoenix.PhoenixParams.*;
import edu.gemini.spModel.gemini.phoenix.InstPhoenix;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Class InstPhoenixTest tests the InstPhoenix class.
 */
public final class InstPhoenixCase {

    private InstPhoenix _ph1;

    static final private double _ERROR = .00001;

    @Before
    public void setUp() throws Exception {
        _ph1 = new InstPhoenix();
    }

    // Setup some test objects.
    @Test
    public void testInitial() {
        assertTrue(_ph1.getSetupTime(null).getSeconds() == 1200L);

        assertTrue(_ph1.getFilter() == Filter.DEFAULT);
        assertEquals(InstPhoenix.DEF_EXPOSURE_TIME, _ph1.getExposureTime(),
                     _ERROR);
        assertTrue(_ph1.getCoadds() == InstPhoenix.DEF_COADDS);
        assertTrue(_ph1.getMask() == Mask.DEFAULT);
        assertEquals(InstPhoenix.DEF_POS_ANGLE, _ph1.getPosAngleDegrees(),
                     _ERROR);
        assertEquals(InstPhoenix.DEF_GRATING_WAVELENGTH, _ph1.getGratingWavelength(),
                     _ERROR);
    }

    /**
     * Test getters and setters
     */
    @Test
    public void testGettersAndSetters() {
        InstPhoenix phOrig = _ph1;
        assertNotNull(phOrig);

        // defined in superclasses of InstPhoenix
        String title = "An Instance of Phoenix";
        phOrig.setTitle(title);
        assertEquals(phOrig.getTitle(), title);
        phOrig.setExposureTime(100.0);
        assertEquals(phOrig.getExposureTime(), 100.0, _ERROR);
        phOrig.setCoadds(2);
        assertTrue(phOrig.getCoadds() == 2);
        phOrig.setPosAngleDegrees(18.0);
        assertEquals(phOrig.getPosAngleDegrees(), 18.0, _ERROR);

        // defined in InstPhoenix
        phOrig.setFilter(Filter.L2462);
        assertTrue(phOrig.getFilter() == Filter.L2462);
        assertTrue(phOrig.getMask() == Mask.DEFAULT);

        phOrig.setGratingWavelength(5.0);
        assertTrue(phOrig.getGratingWavelength() == 5.0);
    }

    /**
     * Test cloneable
     */
    @Test
    public void testCloneable() {
        // Give the data object a title
        String title1 = "Initial Phoenix";
        InstPhoenix phOrig = _ph1;
        assertNotNull(phOrig);
        phOrig.setTitle(title1);
    }
}
