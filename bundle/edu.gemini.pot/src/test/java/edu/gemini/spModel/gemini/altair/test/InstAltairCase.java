/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file LICENSE for complete details.
 *
 */
package edu.gemini.spModel.gemini.altair.test;

import edu.gemini.spModel.gemini.altair.AltairParams.ADC;
import edu.gemini.spModel.gemini.altair.AltairParams.Wavelength;
import edu.gemini.spModel.gemini.altair.InstAltair;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Class InstAltairTest tests the InstAltair class.
 */
public final class InstAltairCase {
    private InstAltair _alt1;

    @Before
    public void setUp() throws Exception {
        _alt1 = new InstAltair();
    }

    // Setup some test objects.
    @Test
    public void testInitial() {
        assertTrue(_alt1.getWavelength() == Wavelength.DEFAULT);
        assertTrue(_alt1.getAdc() == ADC.DEFAULT);
    }

    /**
     * Test getters and setters
     */
    @Test
    public void testGettersAndSetters() {
        InstAltair altOrig = _alt1;
        assertNotNull(altOrig);

        // setTitle is defined in a superclasses of InstPhoenix
        String title = "An Instance of Altair";
        altOrig.setTitle(title);
        assertTrue(altOrig.getTitle().equals(title));

        // defined in InstAltair
        altOrig.setWavelength(Wavelength.WAVELENGTH_1UM);
        assertTrue(altOrig.getWavelength() == Wavelength.WAVELENGTH_1UM);
        altOrig.setAdc(ADC.ON);
        assertTrue(altOrig.getAdc() == ADC.ON);
        altOrig.setAdc(ADC.OFF);
        assertTrue(altOrig.getAdc() == ADC.OFF);

    }

    /**
     * Test cloneable
     */
    @Test
    public void testCloneable() {
        // Give the data object a title
        String title1 = "Initial Altair";
        InstAltair altOrig = _alt1;
        assertNotNull(altOrig);
        altOrig.setTitle(title1);
    }

    @Test
    public void testSerialization() throws Exception {
        final InstAltair outObject = new InstAltair();

        // Create change
        outObject.setAdc(ADC.ON);
        outObject.setWavelength(Wavelength.WAVELENGTH_1UM);

        final InstAltair inObject = ser(outObject);
        assertSame("ADC", ADC.ON, inObject.getAdc());
        assertSame("Wavelength", Wavelength.WAVELENGTH_1UM, inObject.getWavelength());
    }
}
