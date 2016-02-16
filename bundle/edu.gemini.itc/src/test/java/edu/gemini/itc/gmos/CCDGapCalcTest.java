package edu.gemini.itc.gmos;

import org.junit.Assert;
import org.junit.Test;

/**
 * Testing out calcIfu2Shift code in CCDGapCalc.
 * CCDGapCalc.java is based on the IRAF script gfoneshift.cl, which takes the central wavelength
 * and the lpmm and outputs the shift. This is used in the GMOS IFU-2 calculations.
 * This test was created to compare the output of CCDGapCalc.java to gfoneshift.cl.
 * ...
 */
public class CCDGapCalcTest {

    @Test
    public void thisIsATest() {
        final double actualResult0  = CCDGapCalc.calcIfu2Shift(750.0, 831.0);
        final double expectedResult0 = 2736.24773782;           // Taken from gfoneshift in PYRAF
        final double actualResult1  = CCDGapCalc.calcIfu2Shift(750.0, 150.0);
        final double expectedResult1 = 3645.34948931;           // Taken from gfoneshift in PYRAF

        Assert.assertTrue("this is true", true);
        Assert.assertEquals("well this should work", expectedResult0, actualResult0, 0.1);   // 20150814: 0.0172 difference
        Assert.assertEquals("well this should work", expectedResult1, actualResult1, 0.1);     // 20150814: 0.0288 difference-
    }
}
