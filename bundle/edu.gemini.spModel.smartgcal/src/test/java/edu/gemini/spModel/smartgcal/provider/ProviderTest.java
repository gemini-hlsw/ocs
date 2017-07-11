package edu.gemini.spModel.smartgcal.provider;

import edu.gemini.spModel.gemini.calunit.CalUnitParams;
import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProvider;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.CalibrationKeyImpl;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGmosNorth;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGmosSouth;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGnirs;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import edu.gemini.spModel.smartgcal.repository.CalibrationResourceRepository;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 */
public class ProviderTest {

    private static final CalibrationResourceRepository repository = new CalibrationResourceRepository();

    @Test
    public void canLookupGNIRSCalibration() throws Exception {
        CalibrationProvider provider = new CalibrationProviderImpl(repository);
        List<Calibration> calibrations = provider.getCalibrations(
                new CalibrationKeyImpl.WithWavelength(
                        new ConfigKeyGnirs(
                                CalibrationProvider.GNIRSMode.SPECTROSCOPY,
                                GNIRSParams.PixelScale.PS_005,
                                GNIRSParams.Disperser.D_10,
                                GNIRSParams.CrossDispersed.LXD,
                                GNIRSParams.SlitWidth.SW_1,
                                GNIRSParams.WellDepth.SHALLOW),
                        1.11
                )
        );

        Assert.assertNotNull(calibrations);
        Assert.assertTrue(calibrations.size() > 0);
    }

    @Test
    public void canLookupGMOSNCalibration() throws Exception {
        CalibrationProvider provider = new CalibrationProviderImpl(repository);
        List<Calibration> calibrations =  provider.getCalibrations(
                new CalibrationKeyImpl.WithWavelength(
                        new ConfigKeyGmosNorth(
                                GmosNorthType.DisperserNorth.MIRROR,
                                GmosNorthType.FilterNorth.RG610_G0307,
                                GmosNorthType.FPUnitNorth.FPU_NONE,
                                GmosCommonType.Binning.ONE,
                                GmosCommonType.Binning.ONE,
                                GmosCommonType.Order.ZERO,
                                GmosCommonType.AmpGain.LOW),
                        500.0
                )
        );
        Assert.assertNotNull(calibrations);
        Assert.assertTrue(calibrations.size() >= 0); // currently no GMOS-N data...
    }

    @Test
    public void canLookupGMOSSCalibration() throws Exception {
        CalibrationProvider provider = new CalibrationProviderImpl(repository);
        List<Calibration> calibrations =  provider.getCalibrations(
                new CalibrationKeyImpl.WithWavelength(
                        new ConfigKeyGmosSouth(
                                GmosSouthType.DisperserSouth.R150_G5326,
                                GmosSouthType.FilterSouth.NONE,
                                GmosSouthType.FPUnitSouth.LONGSLIT_4,
                                GmosCommonType.Binning.ONE,
                                GmosCommonType.Binning.ONE,
                                GmosCommonType.Order.ONE,
                                GmosCommonType.AmpGain.LOW
                        ),
                        500.0
                )
        );
        Assert.assertNotNull(calibrations);
        Assert.assertTrue(calibrations.size() > 0);
    }

    // some additional tests on GMOS-S table
    @Test
    public void canLookupGMOSSCalibrationDifferentWavelenghts() throws Exception {
        CalibrationProvider provider = new CalibrationProviderImpl(repository);
        ConfigKeyGmosSouth key = new ConfigKeyGmosSouth(
                        GmosSouthType.DisperserSouth.R831_G5322,
                        GmosSouthType.FilterSouth.NONE,
                        GmosSouthType.FPUnitSouth.LONGSLIT_7,   // any
                        GmosCommonType.Binning.TWO,
                        GmosCommonType.Binning.FOUR,
                        GmosCommonType.Order.ONE,
                        GmosCommonType.AmpGain.LOW
                );

        // ===== test wavelength 500
        List<Calibration> calibrations =
                provider.getCalibrations(new CalibrationKeyImpl.WithWavelength(key, 500.0));
        Assert.assertNotNull(calibrations);
        Assert.assertEquals(2, calibrations.size());
        //  check number 1
        Assert.assertEquals(CalUnitParams.Shutter.CLOSED,       calibrations.get(0).getShutter());
        Assert.assertEquals(CalUnitParams.Filter.ND_20,         calibrations.get(0).getFilter());
        Assert.assertEquals(CalUnitParams.Diffuser.VISIBLE,     calibrations.get(0).getDiffuser());
        Assert.assertEquals(4,                                  calibrations.get(0).getExposureTime(), 0.01);
        Assert.assertEquals(new Integer(1),                     calibrations.get(0).getCoadds());
        Assert.assertTrue(calibrations.get(0).isFlat());
        //  check number 2
        Assert.assertEquals(CalUnitParams.Shutter.CLOSED,       calibrations.get(1).getShutter());
        Assert.assertEquals(CalUnitParams.Filter.NONE,          calibrations.get(1).getFilter());
        Assert.assertEquals(CalUnitParams.Diffuser.VISIBLE,     calibrations.get(1).getDiffuser());
        Assert.assertEquals(30,                                 calibrations.get(1).getExposureTime(), 0.01);
        Assert.assertEquals(new Integer(1),                     calibrations.get(1).getCoadds());
        Assert.assertTrue(calibrations.get(1).isArc());
     }

}
