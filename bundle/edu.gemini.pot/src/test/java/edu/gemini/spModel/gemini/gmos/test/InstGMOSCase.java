package edu.gemini.spModel.gemini.gmos.test;

import edu.gemini.spModel.gemini.gmos.*;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Class InstGMOSTest tests the InstGMOS class.
 */
public final class InstGMOSCase {
    private InstGmosNorth _t1;
    private InstGmosSouth _t2;

    static final private double _ERROR = .00001;

    @Before
    public void setUp() throws Exception {
        _t1 = new InstGmosNorth();
        _t2 = new InstGmosSouth();
    }

    @Test
    public void testInitial() {
        assertTrue(_t1.getDisperser() == GmosNorthType.DisperserNorth.DEFAULT);
        assertTrue(_t1.getDisperserOrder() == GmosCommonType.Order.DEFAULT);
        assertEquals(InstGmosCommon.DEFAULT_DISPERSER_LAMBDA, _t1.getDisperserLambda(), _ERROR);
        assertEquals(GmosCommonType.FPUnitMode.DEFAULT, _t1.getFPUnitMode());
        assertEquals(GmosNorthType.FPUnitNorth.DEFAULT, _t1.getFPUnit());
        assertEquals(GmosNorthType.FilterNorth.DEFAULT, _t1.getFilter());
        assertEquals("", _t1.getFPUnitCustomMask());
        assertEquals(GmosCommonType.AmpReadMode.DEFAULT, _t1.getAmpReadMode());
        assertEquals(GmosCommonType.AmpCount.DEFAULT, _t1.getAmpCount());
        assertEquals(GmosCommonType.AmpGain.DEFAULT, _t1.getGainChoice());
        assertEquals(GmosCommonType.Binning.DEFAULT, _t1.getCcdXBinning());
        assertEquals(GmosCommonType.Binning.DEFAULT, _t1.getCcdYBinning());
        assertEquals(GmosNorthType.StageModeNorth.DEFAULT, _t1.getStageMode());
        assertEquals(GmosCommonType.BuiltinROI.DEFAULT, _t1.getBuiltinROI());
    }

    // Test disperser convenience methods
    @Test
    public void testDisperser() {
        assertTrue(_t1.getDisperser() == GmosNorthType.DisperserNorth.DEFAULT);
        assertTrue(_t1.getDisperserOrder() == GmosCommonType.Order.DEFAULT);
        assertEquals(InstGmosCommon.DEFAULT_DISPERSER_LAMBDA, _t1.getDisperserLambda(), _ERROR);

        GmosNorthType.DisperserNorth dispValue = GmosNorthType.DisperserNorth.R831_G5302;
        //noinspection unchecked
        _t1.setDisperser(dispValue);

        assertEquals(dispValue, _t1.getDisperser());
        assertTrue(_t1.getDisperserOrder() == GmosCommonType.Order.DEFAULT);
        assertEquals(InstGmosCommon.DEFAULT_DISPERSER_LAMBDA, _t1.getDisperserLambda(), _ERROR);
    }

    /**
     * Test Mean Gain values
     */
    @Test
    public void testMeanGainValues() {
        //Gemini North Specs
        _t1.setAmpReadMode(GmosCommonType.AmpReadMode.SLOW);
        _t1.setGainChoice(GmosCommonType.AmpGain.LOW);
        assertEquals(1.63, _t1.getMeanGain(), _ERROR);

        _t1.setAmpReadMode(GmosCommonType.AmpReadMode.FAST);
        _t1.setGainChoice(GmosCommonType.AmpGain.LOW);
        assertEquals(1.96, _t1.getMeanGain(), _ERROR);

        _t1.setAmpReadMode(GmosCommonType.AmpReadMode.FAST);
        _t1.setGainChoice(GmosCommonType.AmpGain.HIGH);
        assertEquals(5.11, _t1.getMeanGain(), _ERROR);


        //Gemini South Specs
        _t2.setAmpReadMode(GmosCommonType.AmpReadMode.SLOW);
        _t2.setGainChoice(GmosCommonType.AmpGain.LOW);
        assertEquals(1.8, _t2.getMeanGain(), _ERROR);

        _t2.setAmpReadMode(GmosCommonType.AmpReadMode.FAST);
        _t2.setGainChoice(GmosCommonType.AmpGain.LOW);
        assertEquals(1.6, _t2.getMeanGain(), _ERROR);

        _t2.setAmpReadMode(GmosCommonType.AmpReadMode.FAST);
        _t2.setGainChoice(GmosCommonType.AmpGain.HIGH);
        assertEquals(5.2, _t2.getMeanGain(), _ERROR);
    }


    /**
     * Test Read Noise values
     */
    @Test
    public void testMeanReadNoiseValues() {
        //Gemini North Specs
        _t1.setAmpReadMode(GmosCommonType.AmpReadMode.SLOW);
        _t1.setGainChoice(GmosCommonType.AmpGain.LOW);
        assertEquals(4.14, _t1.getMeanReadNoise(), _ERROR);

        _t1.setAmpReadMode(GmosCommonType.AmpReadMode.FAST);
        _t1.setGainChoice(GmosCommonType.AmpGain.LOW);
        assertEquals(6.27, _t1.getMeanReadNoise(), _ERROR);

        _t1.setAmpReadMode(GmosCommonType.AmpReadMode.FAST);
        _t1.setGainChoice(GmosCommonType.AmpGain.HIGH);
        assertEquals(8.69, _t1.getMeanReadNoise(), _ERROR);

        //Gemini South Specs
        _t2.setAmpReadMode(GmosCommonType.AmpReadMode.SLOW);
        _t2.setGainChoice(GmosCommonType.AmpGain.LOW);
        assertEquals(4.0, _t2.getMeanReadNoise(), _ERROR);

        _t2.setAmpReadMode(GmosCommonType.AmpReadMode.FAST);
        _t2.setGainChoice(GmosCommonType.AmpGain.LOW);
        assertEquals(6.6, _t2.getMeanReadNoise(), _ERROR);

        _t2.setAmpReadMode(GmosCommonType.AmpReadMode.FAST);
        _t2.setGainChoice(GmosCommonType.AmpGain.HIGH);
        assertEquals(7.9, _t2.getMeanReadNoise(), _ERROR);
    }



    /**
     * Test cloneable
     */
    @Test
    public void testCloneable() {
        String title1 = "Initial GMOS";
        // Give the data object a title
        InstGmosNorth t1 = _t1;
        t1.setTitle(title1);

        // Create change
        GmosNorthType.DisperserNorth dispValue = GmosNorthType.DisperserNorth.B1200_G5301;
        t1.setDisperser(dispValue);
        GmosCommonType.Order orderValue = GmosCommonType.Order.TWO;
        t1.setDisperserOrder(orderValue);
        double lambdaValue = 6500.0;
        t1.setDisperserLambda(lambdaValue);

        GmosCommonType.ADC adcValue = GmosCommonType.ADC.BEST_STATIC;
        t1.setAdc(adcValue);

        GmosNorthType.FPUnitNorth fpuValue = GmosNorthType.FPUnitNorth.IFU_1;
        t1.setFPUnit(fpuValue);

        GmosNorthType.FilterNorth filterValue = GmosNorthType.FilterNorth.r_G0303;
        t1.setFilter(filterValue);

        GmosCommonType.AmpGain ampGainValue = GmosCommonType.AmpGain.HIGH;
        t1.setGainChoice(ampGainValue);

        GmosCommonType.AmpReadMode ampSpeedValue = GmosCommonType.AmpReadMode.FAST;
        t1.setAmpReadMode(ampSpeedValue);

        GmosCommonType.AmpCount ampCountValue = GmosCommonType.AmpCount.SIX;
        t1.setAmpCount(ampCountValue);

        GmosCommonType.Binning xBinValue = GmosCommonType.Binning.FOUR;
        t1.setCcdXBinning(xBinValue);

        GmosCommonType.Binning yBinValue = GmosCommonType.Binning.TWO;
        t1.setCcdYBinning(yBinValue);

        String maskValue = "My Test Mask";
        t1.setFPUnitCustomMask(maskValue);

        GmosNorthType.StageModeNorth stageModeValue = GmosNorthType.StageModeNorth.FOLLOW_XY;
        t1.setStageMode(stageModeValue);

        GmosCommonType.BuiltinROI builtinROIValue = GmosCommonType.BuiltinROI.TOP_SPECTRUM;
        t1.setBuiltinROI(builtinROIValue);

        // Check values
        assertTrue(t1.getDisperser() == dispValue);
        assertTrue(t1.getDisperserOrder() == orderValue);
        assertEquals(lambdaValue, t1.getDisperserLambda(), _ERROR);

        assertTrue(t1.getAdc() == adcValue);
        assertTrue(t1.getAmpCount() == ampCountValue);
        assertTrue(t1.getGainChoice() == ampGainValue);
        assertTrue(t1.getAmpReadMode() == ampSpeedValue);
        assertTrue(t1.getFPUnit() == fpuValue);
        assertTrue(t1.getFilter() == filterValue);
        assertEquals(t1.getFPUnitCustomMask(), maskValue);
        assertTrue(t1.getCcdXBinning() == xBinValue);
        assertTrue(t1.getCcdYBinning() == yBinValue);
        assertTrue(t1.getStageMode() == stageModeValue);
        assertTrue(t1.getBuiltinROI() == builtinROIValue);

        // Now clone and test that changing values don't influence one another
        InstGmosNorth t2 = (InstGmosNorth) t1.clone();

        assertTrue(t2.getDisperser() == dispValue);
        assertTrue(t2.getDisperserOrder() == orderValue);
        assertEquals(lambdaValue, t2.getDisperserLambda(), _ERROR);
        assertTrue(t2.getFilter() == filterValue);
        assertTrue(t2.getFPUnit() == fpuValue);
        assertEquals(t2.getFPUnitCustomMask(), maskValue);
        assertTrue(t2.getAdc() == adcValue);
        assertTrue(t2.getGainChoice() == ampGainValue);
        assertTrue(t2.getAmpReadMode() == ampSpeedValue);
        assertTrue(t2.getAmpCount() == ampCountValue);
        assertEquals(t2.getFPUnitCustomMask(), maskValue);
        assertTrue(t2.getCcdXBinning() == xBinValue);
        assertTrue(t2.getCcdYBinning() == yBinValue);
        assertTrue(t2.getStageMode() == stageModeValue);
        assertTrue(t2.getBuiltinROI() == builtinROIValue);

        GmosCommonType.ADC adcValue2 = GmosCommonType.ADC.FOLLOW;
        t2.setAdc(adcValue2);
        assertTrue(t2.getAdc() == adcValue2);
        assertTrue(t1.getAdc() == adcValue);

        GmosNorthType.FilterNorth filterValue2 = GmosNorthType.FilterNorth.NONE;
        t2.setFilter(filterValue2);
        assertTrue(t2.getFilter() == filterValue2);
        assertTrue(t1.getFilter() == filterValue);

        GmosNorthType.FPUnitNorth fpuValue2 = GmosNorthType.FPUnitNorth.LONGSLIT_5;
        t2.setFPUnit(fpuValue2);
        assertTrue(t2.getFPUnit() == fpuValue2);
        assertTrue(t1.getFPUnit() == fpuValue);

        String maskValue2 = "Another Mask";
        t2.setFPUnitCustomMask(maskValue2);
        assertEquals(t2.getFPUnitCustomMask(), maskValue2);
        assertEquals(t1.getFPUnitCustomMask(), maskValue);

        GmosCommonType.AmpGain ampGainValue2 = GmosCommonType.AmpGain.LOW;
        t2.setGainChoice(ampGainValue2);
        assertTrue(t2.getGainChoice() == ampGainValue2);
        assertTrue(t1.getGainChoice() == ampGainValue);

        GmosCommonType.AmpReadMode ampSpeedValue2 = GmosCommonType.AmpReadMode.SLOW;
        t2.setAmpReadMode(ampSpeedValue2);
        assertTrue(t2.getAmpReadMode() == ampSpeedValue2);
        assertTrue(t1.getAmpReadMode() == ampSpeedValue);

        GmosCommonType.AmpCount ampCountValue2 = GmosCommonType.AmpCount.THREE;
        t2.setAmpCount(ampCountValue2);
        assertTrue(t2.getAmpCount() == ampCountValue2);
        assertTrue(t1.getAmpCount() == ampCountValue);

        GmosCommonType.Binning binValue2 = GmosCommonType.Binning.ONE;
        t2.setCcdXBinning(binValue2);
        t2.setCcdYBinning(binValue2);
        assertTrue(t2.getCcdXBinning() == binValue2);
        assertTrue(t1.getCcdXBinning() == xBinValue);
        assertTrue(t2.getCcdYBinning() == binValue2);
        assertTrue(t1.getCcdYBinning() == yBinValue);

        GmosNorthType.StageModeNorth stageModeValue2 = GmosNorthType.StageModeNorth.FOLLOW_XYZ;
        t2.setStageMode(stageModeValue2);
        assertTrue(t2.getStageMode() == stageModeValue2);
        assertTrue(t1.getStageMode() == stageModeValue);

        GmosCommonType.BuiltinROI builtinROIValue2 = GmosCommonType.BuiltinROI.BOTTOM_SPECTRUM;
        t2.setBuiltinROI(builtinROIValue2);
        assertTrue(t2.getBuiltinROI() == builtinROIValue2);
        assertTrue(t1.getBuiltinROI() == builtinROIValue);

        // Now change disperser
        GmosNorthType.DisperserNorth dispValue2 = GmosNorthType.DisperserNorth.MIRROR;
        GmosCommonType.Order orderValue2 = GmosCommonType.Order.ONE;
        double lambdaValue2 = 2500.0;
        t2.setDisperser(dispValue2);
        t2.setDisperserOrder(orderValue2);
        t2.setDisperserLambda(lambdaValue2);

        assertTrue(t1.getDisperser() == dispValue);
        assertTrue(t1.getDisperserOrder() == orderValue);
        assertEquals(lambdaValue, t1.getDisperserLambda(), _ERROR);
        assertTrue(t2.getDisperser() == dispValue2);
        assertTrue(t2.getDisperserOrder() == orderValue2);
        assertEquals(lambdaValue2, t2.getDisperserLambda(), _ERROR);
    }

    // Test serialization
    @Test
    public void testSerialization() throws Exception {
        final InstGmosNorth outObject = new InstGmosNorth();

        GmosNorthType.DisperserNorth dispValue = GmosNorthType.DisperserNorth.B1200_G5301;
        outObject.setDisperser(dispValue);
        GmosCommonType.Order orderValue = GmosCommonType.Order.TWO;
        outObject.setDisperserOrder(orderValue);
        double lambdaValue = 6500.0;
        outObject.setDisperserLambda(lambdaValue);

        GmosCommonType.ADC adcValue = GmosCommonType.ADC.BEST_STATIC;
        outObject.setAdc(adcValue);

        GmosNorthType.FPUnitNorth fpuValue = GmosNorthType.FPUnitNorth.IFU_1;
        outObject.setFPUnit(fpuValue);

        GmosNorthType.FilterNorth filterValue = GmosNorthType.FilterNorth.r_G0303;
        outObject.setFilter(filterValue);

        GmosCommonType.AmpGain gainValue = GmosCommonType.AmpGain.HIGH;
        outObject.setGainChoice(gainValue);
        GmosCommonType.AmpReadMode speedValue = GmosCommonType.AmpReadMode.FAST;
        outObject.setAmpReadMode(speedValue);
        GmosCommonType.AmpCount countValue = GmosCommonType.AmpCount.SIX;
        outObject.setAmpCount(countValue);

        double expTime = 100.0;
        outObject.setExposureTime(expTime);
        double posAngle = 179.23;
        outObject.setPosAngleDegrees(posAngle);

        GmosCommonType.Binning binningValue = GmosCommonType.Binning.FOUR;
        outObject.setCcdXBinning(binningValue);
        outObject.setCcdYBinning(binningValue);

        String maskValue = "My Test Mask";
        outObject.setFPUnitCustomMask(maskValue);

        GmosNorthType.StageModeNorth stageModeValue = GmosNorthType.StageModeNorth.FOLLOW_XYZ;
        outObject.setStageMode(stageModeValue);

        GmosCommonType.BuiltinROI builtinROIValue = GmosCommonType.BuiltinROI.TOP_SPECTRUM;
        outObject.setBuiltinROI(builtinROIValue);

        final InstGmosNorth inObject = ser(outObject);

        assertTrue(inObject.getDisperser() == dispValue);
        assertTrue(inObject.getDisperserOrder() == orderValue);
        assertEquals(lambdaValue, inObject.getDisperserLambda(), _ERROR);

        assertEquals("ADC", adcValue, inObject.getAdc());
        assertEquals("FPU", fpuValue, inObject.getFPUnit());
        assertEquals("Filter", filterValue, inObject.getFilter());

        assertEquals("ampGain", gainValue, inObject.getGainChoice());
        assertEquals("ampSpeed", speedValue, inObject.getAmpReadMode());
        assertEquals("ampCount", countValue, inObject.getAmpCount());

        assertEquals("Exp time", expTime, inObject.getExposureTime(), _ERROR);
        assertEquals("PosAngle", posAngle, inObject.getPosAngleDegrees(), _ERROR);
        assertEquals("XBin", binningValue, inObject.getCcdXBinning());
        assertEquals("YBin", binningValue, inObject.getCcdYBinning());

        assertEquals("Mask", maskValue, inObject.getFPUnitCustomMask());
        assertEquals("StageMode", stageModeValue, inObject.getStageMode());
        assertEquals("BuiltinROI", builtinROIValue, inObject.getBuiltinROI());
    }
}
