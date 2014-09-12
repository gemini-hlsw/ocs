/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file COPYRIGHT for complete details.
 *
 * $Id: InstMichelleCase.java 15413 2008-11-06 15:35:01Z swalker $
 */

package edu.gemini.spModel.gemini.michelle.test;

import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.michelle.MichelleParams;
import edu.gemini.spModel.gemini.michelle.MichelleParams.Disperser;
import edu.gemini.spModel.gemini.michelle.MichelleParams.Filter;

import static edu.gemini.spModel.test.TestFile.ser;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Class InstMichelleTest tests the InstMichelle class.
 */
public final class InstMichelleCase {

    // XXX allan: Tests are not complete yet: did a quick copy/edit from GMOS version XXX

    private InstMichelle _t1;

    static final private double _ERROR = .000001;

    @Before
    public void setUp() throws Exception {
        _t1 = new InstMichelle();
    }

    // Setup some test objects.
    @Test
    public void testInitial() {
        Disperser d = _t1.getDisperser();
        assertTrue(d == Disperser.DEFAULT);
        assertEquals(d.getLamda(), _t1.getDisperserLambda(), _ERROR);
        assertEquals(Filter.DEFAULT, _t1.getFilter());
    }

    // Test disperser convenience methods
    @Test
    public void testDisperser() {
        Disperser d = _t1.getDisperser();
        assertTrue(d == Disperser.DEFAULT);
        assertEquals(d.getLamda(), _t1.getDisperserLambda(), _ERROR);

        Disperser dispValue = Disperser.MIRROR;
        _t1.setDisperser(dispValue);
        d = _t1.getDisperser();
        assertEquals(dispValue, d);
    }

    @Test
    public void testCentralWavelength() {
        for (MichelleParams.Disperser disp : MichelleParams.Disperser.values()) {
            _t1.setDisperser(disp);
            assertEquals(_t1.getDisperserLambda(), disp.getLamda(), _ERROR);
        }
    }

    /**
     * Test cloneable
     */
    @Test
    public void testCloneable() {
        String title1 = "Initial Michelle";
        // Give the data object a title
        InstMichelle t1 = _t1;
        assertNotNull(t1);
        t1.setTitle(title1);

        // Create change
        Disperser dispValue = Disperser.MIRROR;
        t1.setDisperser(dispValue);
        double lambdaValue = 6500.0;
        t1.setDisperserLambda(lambdaValue);

        Filter filterValue = Filter.SI_2;
        t1.setFilter(filterValue);

        // Check values
        Disperser dtemp = t1.getDisperser();
        assertTrue(dtemp == dispValue);
        assertEquals(lambdaValue, t1.getDisperserLambda(), _ERROR);

        // Now clone and test that changing values don't influence one another
        InstMichelle t2 = (InstMichelle) t1.clone();
        assertNotNull(t2);

        Disperser d2 = t2.getDisperser();
        assertTrue(d2 == dispValue);
        assertEquals(lambdaValue, t2.getDisperserLambda(), _ERROR);
        assertTrue(t2.getFilter() == filterValue);

        Filter filterValue2 = Filter.NONE;
        t2.setFilter(filterValue2);
        assertTrue(t2.getFilter() == filterValue2);
        assertTrue(t1.getFilter() == filterValue);

        // Now change disperser
        Disperser dispValue2 = Disperser.MIRROR;
        double lambdaValue2 = 2500.0;
        t2.setDisperser(dispValue2);
        t2.setDisperserLambda(lambdaValue2);

        dtemp = t1.getDisperser();
        assertTrue(dtemp == dispValue);
        assertEquals(lambdaValue, t1.getDisperserLambda(), _ERROR);
    }

    // Test serialization
    @Test
    public void testSerialization() throws Exception {
        final InstMichelle outObject = new InstMichelle();

        // Create change
        Disperser dispValue = Disperser.MIRROR;
        double lambdaValue = 6500.0;
        outObject.setDisperser(dispValue);
        outObject.setDisperserLambda(lambdaValue);

        Filter filterValue = Filter.SI_1;
        outObject.setFilter(filterValue);

        double expTime = 100.0;
        outObject.setExposureTime(expTime);
        double posAngle = 179.23;
        outObject.setPosAngleDegrees(posAngle);

        final InstMichelle inObject = ser(outObject);

        Disperser dtemp = inObject.getDisperser();
        double lambdaIn = inObject.getDisperserLambda();
        assertTrue(dtemp == dispValue);
        assertEquals(lambdaValue, lambdaIn, _ERROR);

        assertEquals("Filter", filterValue, inObject.getFilter());

        assertEquals("Exp time", expTime, inObject.getExposureTime(), _ERROR);
        assertEquals("PosAngle", posAngle, inObject.getPosAngleDegrees(), _ERROR);
    }
}
