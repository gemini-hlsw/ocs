package edu.gemini.spModel.smartgcal;

import edu.gemini.spModel.gemini.calunit.smartgcal.*;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGmosNorth;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGmosSouth;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGnirs;
import edu.gemini.spModel.gemini.calunit.smartgcal.maps.GMOSNCalibrationMap;
import edu.gemini.spModel.gemini.calunit.smartgcal.maps.GMOSSCalibrationMap;
import edu.gemini.spModel.gemini.calunit.smartgcal.maps.GNIRSCalibrationMap;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.Properties;
import java.util.Set;

/**
 */
public class CalibrationMapTest {

    @Test
    public void doesSupportGNIRSWildcards() {

        CalibrationMap map = new GNIRSCalibrationMap(new Version(1, new Date()));
        Set<ConfigurationKey> keys;

        Properties properties = new Properties();
        properties.setProperty(ConfigKeyGnirs.Values.MODE.toString(), CalibrationProvider.GNIRSMode.SPECTROSCOPY.toString());
        properties.setProperty(ConfigKeyGnirs.Values.PIXEL_SCALE.toString(), GNIRSParams.PixelScale.PS_005.displayValue());
        properties.setProperty(ConfigKeyGnirs.Values.CROSS_DISPERSED.toString(), GNIRSParams.CrossDispersed.LXD.displayValue());
        properties.setProperty(ConfigKeyGnirs.Values.DISPERSER.toString(), GNIRSParams.Disperser.D_10.displayValue());
        properties.setProperty(ConfigKeyGnirs.Values.SLIT_WIDTH.toString(), GNIRSParams.SlitWidth.SW_1.displayValue());
        properties.setProperty(ConfigKeyGnirs.Values.WELL_DEPTH.toString(), GNIRSParams.WellDepth.DEEP.displayValue());

        // no wildcards, we expect precisely one key
        keys= map.createConfig(properties);
        Assert.assertEquals(1, keys.size());

        // wildcard that matches a subset of available values
        properties.setProperty(ConfigKeyGnirs.Values.SLIT_WIDTH.toString(), "0.*");
        keys = map.createConfig(properties);
        Assert.assertEquals(6, keys.size());

        // wildcard that matches all available values
        // Note that obsolete values are allowed!
        properties.setProperty(ConfigKeyGnirs.Values.SLIT_WIDTH.toString(), "*");
        keys = map.createConfig(properties);
        Assert.assertEquals(15, keys.size());

        // combination of wildcards: 15 slit widths (2 obsolete), 2 pixel scales, 2 well depths = 60 keys...
        properties.setProperty(ConfigKeyGnirs.Values.SLIT_WIDTH.toString(), "*");
        properties.setProperty(ConfigKeyGnirs.Values.PIXEL_SCALE.toString(), "*");
        properties.setProperty(ConfigKeyGnirs.Values.WELL_DEPTH.toString(), "*");
        keys = map.createConfig(properties);
        Assert.assertEquals(60, keys.size());
    }

    @Test
    public void doesSupportGMOSNWildcards() {

        CalibrationMap map = new GMOSNCalibrationMap(new Version(1, new Date()));
        Set<ConfigurationKey> keys;

        Properties properties = new Properties();
        properties.setProperty(ConfigKeyGmosNorth.Values.XBIN.toString(), GmosCommonType.Binning.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosNorth.Values.YBIN.toString(), GmosCommonType.Binning.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosNorth.Values.ORDER.toString(), GmosCommonType.Order.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosNorth.Values.GAIN.toString(), GmosCommonType.AmpGain.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosNorth.Values.DISPERSER.toString(), GmosNorthType.DisperserNorth.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosNorth.Values.FILTER.toString(), GmosNorthType.FilterNorth.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosNorth.Values.FPU.toString(), GmosNorthType.FPUnitNorth.DEFAULT.displayValue());

        // no wildcards, we expect precisely one key
        keys = map.createConfig(properties);
        Assert.assertEquals(1, keys.size());

        // wildcard that matches a subset of available values
        properties.setProperty(ConfigKeyGmosNorth.Values.FPU.toString(), "Longslit *");
        keys = map.createConfig(properties);
        Assert.assertEquals(7, keys.size());

        // wildcard that matches all available values
        properties.setProperty(ConfigKeyGmosNorth.Values.FPU.toString(), "*");
        keys = map.createConfig(properties);
        Assert.assertEquals(GmosNorthType.FPUnitNorth.values().length, keys.size());

        // combination of wildcards: 7 fpus, 3 xbins, 2 gains = *42* keys...
        properties.setProperty(ConfigKeyGmosNorth.Values.FPU.toString(), "Longslit *");
        properties.setProperty(ConfigKeyGmosNorth.Values.XBIN.toString(), "*");
        properties.setProperty(ConfigKeyGmosNorth.Values.GAIN.toString(), "*");
        keys = map.createConfig(properties);
        Assert.assertEquals(42, keys.size());
    }

    @Test
    public void doesSupportGMOSSWildcards() {

        CalibrationMap map = new GMOSSCalibrationMap(new Version(1, new Date()));
        Set<ConfigurationKey> keys;

        Properties properties = new Properties();
        properties.setProperty(ConfigKeyGmosSouth.Values.XBIN.toString(), GmosCommonType.Binning.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosSouth.Values.YBIN.toString(), GmosCommonType.Binning.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosSouth.Values.ORDER.toString(), GmosCommonType.Order.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosSouth.Values.GAIN.toString(), GmosCommonType.AmpGain.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosSouth.Values.DISPERSER.toString(), GmosSouthType.DisperserSouth.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosSouth.Values.FILTER.toString(), GmosSouthType.FilterSouth.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosSouth.Values.FPU.toString(), GmosSouthType.FPUnitSouth.DEFAULT.displayValue());

        // no wildcards, we expect precisely one key
        keys= map.createConfig(properties);
        Assert.assertEquals(1, keys.size());

        // wildcard that matches a subset of available values
        properties.setProperty(ConfigKeyGmosSouth.Values.FPU.toString(), "N and S *");
        keys = map.createConfig(properties);
        Assert.assertEquals(5, keys.size());

        // wildcard that matches all available values
        properties.setProperty(ConfigKeyGmosSouth.Values.FPU.toString(), "*");
        keys = map.createConfig(properties);
        Assert.assertEquals(GmosSouthType.FPUnitSouth.values().length, keys.size());

        // combination of wildcards: 5 fpus, 3 xbins, 2 gains = 30 keys...
        properties.setProperty(ConfigKeyGmosSouth.Values.FPU.toString(), "N and S *");
        properties.setProperty(ConfigKeyGmosSouth.Values.XBIN.toString(), "*");
        properties.setProperty(ConfigKeyGmosSouth.Values.GAIN.toString(), "*");
        keys = map.createConfig(properties);
        Assert.assertEquals(30, keys.size());
    }

    @Test
    public void doesSupportGMOSSRegex() {
        CalibrationMap map = new GMOSSCalibrationMap(new Version(1, new Date()));
        Set<ConfigurationKey> keys;

        // validate regex (just to be sure)
        String regex = "Longslit.*|N and S.*";
        Assert.assertTrue("Longslit 1.00 arcsec".matches(regex));
        Assert.assertTrue("N and S 1.00 arcsec".matches(regex));

        // put together the properties
        Properties properties = new Properties();
        properties.setProperty(ConfigKeyGmosSouth.Values.XBIN.toString(), GmosCommonType.Binning.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosSouth.Values.YBIN.toString(), GmosCommonType.Binning.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosSouth.Values.ORDER.toString(), GmosCommonType.Order.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosSouth.Values.GAIN.toString(), GmosCommonType.AmpGain.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosSouth.Values.DISPERSER.toString(), GmosSouthType.DisperserSouth.DEFAULT.displayValue());
        properties.setProperty(ConfigKeyGmosSouth.Values.FILTER.toString(), GmosSouthType.FilterSouth.DEFAULT.displayValue());
        // regex that matches either "Longslit*" or "N and S*"
        properties.setProperty(ConfigKeyGmosSouth.Values.FPU.toString(), "$"+regex);

        // we expect 7 "Longslit" and 5 "N and S" values = 12 keys to match this
        keys= map.createConfig(properties);
        Assert.assertEquals(12, keys.size());
    }
}
