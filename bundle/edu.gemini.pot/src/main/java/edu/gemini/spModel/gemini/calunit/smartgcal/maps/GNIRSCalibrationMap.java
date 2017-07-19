package edu.gemini.spModel.gemini.calunit.smartgcal.maps;

import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProvider;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGnirs;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 */
public final class GNIRSCalibrationMap extends CentralWavelengthMap {

    public GNIRSCalibrationMap(Version version) {
        super(version);
    }

    @Override
    public ConfigurationKey.Values[] getKeyValueNames() {
        return ConfigKeyGnirs.Values.values();
    }

    @Override
    public Set<ConfigurationKey> createConfig(Properties properties) {

        CalibrationProvider.GNIRSMode mode = Enum.valueOf(CalibrationProvider.GNIRSMode.class, properties.getProperty(ConfigKeyGnirs.Values.MODE.toString()).toUpperCase());

        // lookup values
        Set<GNIRSParams.PixelScale> pixelScales = getValues(GNIRSParams.PixelScale.class, properties, ConfigKeyGnirs.Values.PIXEL_SCALE);
        Set<GNIRSParams.WellDepth> wellDepths = getValues(GNIRSParams.WellDepth.class, properties, ConfigKeyGnirs.Values.WELL_DEPTH);
        Set<GNIRSParams.SlitWidth> slitWidths = getValues(GNIRSParams.SlitWidth.class, properties, ConfigKeyGnirs.Values.SLIT_WIDTH);
        Set<GNIRSParams.Disperser> dispersers = getValues(GNIRSParams.Disperser.class, properties, ConfigKeyGnirs.Values.DISPERSER);
        Set<GNIRSParams.CrossDispersed> crossDisperseds = getValues(GNIRSParams.CrossDispersed.class, properties, ConfigKeyGnirs.Values.CROSS_DISPERSED);

        // create all possible combinations and produce a key for each one
        Set<ConfigurationKey> keys = new HashSet<ConfigurationKey>();
        for (GNIRSParams.PixelScale pixelScale : pixelScales) {
            for (GNIRSParams.WellDepth wellDepth : wellDepths) {
                for (GNIRSParams.SlitWidth slitWidth : slitWidths) {
                    for (GNIRSParams.Disperser disperser : dispersers) {
                        for (GNIRSParams.CrossDispersed crossDispersed : crossDisperseds) {

                            ConfigKeyGnirs key =
                                    new ConfigKeyGnirs(
                                            mode, pixelScale, disperser, crossDispersed,  slitWidth, wellDepth
                                    );

                            keys.add(key);

                        }
                    }
                }
            }
        }

        // return the set of keys we just came up with
        return keys;
    }

    // GNIRS central wavelength is stored as um.
    protected int toAngstroms(double wl) {
        return (int) Math.round(wl * 10000);
    }

}
