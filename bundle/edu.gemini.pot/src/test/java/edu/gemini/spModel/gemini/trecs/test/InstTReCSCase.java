/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file COPYRIGHT for complete details.
 *
 * $Id: InstTReCSCase.java 7051 2006-05-24 21:35:14Z anunez $
 */

package edu.gemini.spModel.gemini.trecs.test;

import edu.gemini.spModel.gemini.trecs.TReCSParams.Disperser;
import edu.gemini.spModel.gemini.trecs.TReCSParams.Filter;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.gemini.trecs.TReCSParams;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


/**
 * Class InstTReCSTest tests the InstTReCS class.
 */
public final class InstTReCSCase {

    //TODO: XXX allan: Tests are not complete yet: did a quick copy/edit from GMOS version XXX

    private InstTReCS _t1;

    static final private double _ERROR = .00001;

    @Before
    public void setUp() throws Exception {
        _t1 = new InstTReCS();
    }

    // Setup some test objects.
    @Test
    public void testInitial() {
        assertTrue(_t1.getDisperser() == TReCSParams.Disperser.DEFAULT);
        assertEquals(InstTReCS.DEFAULT_LAMBDA, _t1.getDisperserLambda(), _ERROR);
        assertEquals(Filter.DEFAULT, _t1.getFilter());
    }

    // Test disperser convenience methods
    @Test
    public void testDisperser() {
        assertTrue(_t1.getDisperser() == TReCSParams.Disperser.DEFAULT);
        assertEquals(InstTReCS.DEFAULT_LAMBDA, _t1.getDisperserLambda(), _ERROR);

        Disperser dispValue = Disperser.LOW_RES_10;
        _t1.setDisperser(dispValue);
        Disperser d = _t1.getDisperser();
        assertEquals(dispValue, d);
        assertEquals(InstTReCS.DEFAULT_LAMBDA, _t1.getDisperserLambda(), _ERROR);

        // Now set with a name
        String dispName = Disperser.LOW_RES_20.name();
        _t1.setDisperserName(dispName);
        assertEquals(dispName, _t1.getDisperser().name());
        assertEquals(InstTReCS.DEFAULT_LAMBDA, _t1.getDisperserLambda(), _ERROR);
    }

    /**
     * Test cloneable
     */
    @Test
    public void testCloneable() {
        String title1 = "Initial TReCS";
        // Give the data object a title
        InstTReCS t1 = _t1;
        assertNotNull(t1);
        t1.setTitle(title1);

        // Create change

        Disperser dispValue = Disperser.LOW_RES_10;
        double lambdaValue = 6500.0;
        t1.setDisperserLambda(lambdaValue);
        t1.setDisperser(dispValue);

        Filter filterValue = Filter.SI_2;
        t1.setFilter(filterValue);

        // Check values

        assertTrue(t1.getDisperser() == dispValue);
        assertEquals(lambdaValue, t1.getDisperserLambda(), _ERROR);

        // Now clone and test that changing values don't influence one another
        InstTReCS t2 = (InstTReCS) t1.clone();
        assertNotNull(t2);

        assertTrue(t2.getDisperser() == dispValue);
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

        assertTrue(t1.getDisperser() == dispValue);
        assertEquals(lambdaValue, t1.getDisperserLambda(), _ERROR);
        assertTrue(t2.getDisperser() == dispValue2);
        assertEquals(lambdaValue2, t2.getDisperserLambda(), _ERROR);

    }

    @Test
    public void testSerialization() throws Exception {
        final InstTReCS outObject = new InstTReCS();

        // Create change
        Disperser dispValue = Disperser.LOW_RES_10;
        double lambdaValue = 6500.0;
        outObject.setDisperser(dispValue);
        outObject.setDisperserLambda(lambdaValue);

        Filter filterValue = Filter.SI_1;
        outObject.setFilter(filterValue);

        double expTime = 100.0;
        outObject.setExposureTime(expTime);
        double posAngle = 179.23;
        outObject.setPosAngleDegrees(posAngle);

        final InstTReCS inObject = ser(outObject);

        assertTrue(inObject.getDisperser() == dispValue);
        assertEquals(lambdaValue, inObject.getDisperserLambda(), _ERROR);

        assertEquals("Filter", filterValue, inObject.getFilter());

        assertEquals("Exp time", expTime, inObject.getExposureTime(), _ERROR);
        assertEquals("PosAngle", posAngle, inObject.getPosAngleDegrees(), _ERROR);
    }
}
