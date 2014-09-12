package edu.gemini.spModel.smartgcal;

import edu.gemini.spModel.gemini.calunit.CalUnitParams;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class CalibrationTest {

    @Test
    public void canCreateFlatCalibration() {

        // create a calibration
        CalibrationImpl calibration = new CalibrationImpl(
                new CalUnitParams.Lamp[] { CalUnitParams.Lamp.IR_GREY_BODY_HIGH },
                CalUnitParams.Shutter.CLOSED,
                CalUnitParams.Filter.ND_10,
                CalUnitParams.Diffuser.VISIBLE,
                1,
                12.0f,
                1,
                new CalibrationImpl.Basecal[] {}
                );

        // check that calibration was created properly
        Assert.assertTrue(calibration.isFlat());
        Assert.assertFalse(calibration.isArc());
    }

    @Test
    public void canCreateArcCalibration() {

        // create a calibration
        CalibrationImpl calibration = new CalibrationImpl(
                new CalUnitParams.Lamp[] { CalUnitParams.Lamp.AR_ARC, CalUnitParams.Lamp.THAR_ARC },
                CalUnitParams.Shutter.CLOSED,
                CalUnitParams.Filter.ND_10,
                CalUnitParams.Diffuser.VISIBLE,
                1,
                12.0f,
                1,
                new CalibrationImpl.Basecal[] {}
                );

        // check that calibration was created properly
        Assert.assertTrue(calibration.isArc());
        Assert.assertFalse(calibration.isFlat());
    }

    @Test
    public void canCreateArcCalibrationFromProperties() {

        Properties properties = new Properties();
        properties.setProperty(CalibrationImpl.Values.LAMPS.toString(), "Ar arc+ThAr arc");
        properties.setProperty(CalibrationImpl.Values.SHUTTER.toString(), "closed");
        properties.setProperty(CalibrationImpl.Values.FILTER.toString(), "none");
        properties.setProperty(CalibrationImpl.Values.DIFFUSER.toString(), "IR");
        properties.setProperty(CalibrationImpl.Values.OBSERVE.toString(), "4");
        properties.setProperty(CalibrationImpl.Values.EXPOSURE_TIME.toString(), "15.23");
        properties.setProperty(CalibrationImpl.Values.COADDS.toString(), "2");
        properties.setProperty(CalibrationImpl.Values.BASECAL.toString(), "night+day");

        // create a calibration
        CalibrationImpl calibration = (CalibrationImpl)CalibrationImpl.parse(properties);

        // check that calibration was created properly
        Set<CalUnitParams.Lamp> expectedLamps = new HashSet<CalUnitParams.Lamp>();
        expectedLamps.add(CalUnitParams.Lamp.THAR_ARC);
        expectedLamps.add(CalUnitParams.Lamp.AR_ARC);

        Assert.assertTrue(calibration.isArc());
        Assert.assertFalse(calibration.isFlat());
        Assert.assertEquals(expectedLamps, calibration.getLamps());
        Assert.assertEquals(CalUnitParams.Shutter.CLOSED, calibration.getShutter());
        Assert.assertEquals(CalUnitParams.Filter.NONE, calibration.getFilter());
        Assert.assertEquals(CalUnitParams.Diffuser.IR, calibration.getDiffuser());
        Assert.assertEquals(new Integer(4), calibration.getObserve());
        Assert.assertEquals(15.23d, calibration.getExposureTime(), 0.001);
        Assert.assertEquals(new Integer(2), calibration.getCoadds());
        Assert.assertTrue(calibration.isBasecalDay());
        Assert.assertTrue(calibration.isBasecalNight());
    }

    @Test(expected = RuntimeException.class)
    public void canNotCreateMixedCalibration() {
        // try to create a calibration with mixed lamps (pfui!)
        CalibrationImpl calibration = new CalibrationImpl(
                new CalUnitParams.Lamp[] { CalUnitParams.Lamp.IR_GREY_BODY_HIGH, CalUnitParams.Lamp.THAR_ARC },
                CalUnitParams.Shutter.CLOSED,
                CalUnitParams.Filter.ND_10,
                CalUnitParams.Diffuser.VISIBLE,
                1,
                12.0f,
                1,
                new CalibrationImpl.Basecal[] {}
                );
    }

    @Test(expected = RuntimeException.class)
    public void canNotCreateFlatCalibrationWithMultipleLamps() {
        // try to create a flat with more than one lamp (pfui!)
        CalibrationImpl calibration = new CalibrationImpl(
                new CalUnitParams.Lamp[] { CalUnitParams.Lamp.IR_GREY_BODY_HIGH, CalUnitParams.Lamp.IR_GREY_BODY_LOW },
                CalUnitParams.Shutter.CLOSED,
                CalUnitParams.Filter.ND_10,
                CalUnitParams.Diffuser.VISIBLE,
                1,
                12.0f,
                1,
                new CalibrationImpl.Basecal[] {}
                );
    }
}
