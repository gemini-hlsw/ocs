package edu.gemini.spModel.gemini.calunit.smartgcal.maps;

import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGmos;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGmosSouth;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * GMOS South calibration map.
 * This map stores calibrations that can be retrived with GMOS-S configuration keys which represent
 * instrument configurations. When reading a file, createConfig() is called with the a Properties object
 * containing the named value pairs describing the different instrument parameters. These parameters can
 * be wildcards or regular expressions which represent a set of values. The calls to getValues() and the
 * nested for-loops will expand lines in the config files with wildcards and regular expressions to all
 * possible configuration keys that are represented by this line. This leads to big hash maps but makes
 * lookups fast and simple. As of April 2012 the GMOS-N and GMOS-S tables have each between 20000 and 40000
 * entries resulting in about 50MB of memory consumption (see UX-1426 for more information).
 */
public final class GMOSSCalibrationMap extends CentralWavelengthMap {

    public GMOSSCalibrationMap(Version version) {
        // make gmos-s maps big enough for all entries that it will have to store
        super(version, 40000);
    }

    @Override
    public ConfigurationKey.Values[] getKeyValueNames() {
        return ConfigKeyGmosSouth.Values.values();
    }

    @Override
    public Set<ConfigurationKey> createConfig(Properties properties) {
        // lookup values
        Set<GmosCommonType.Binning> xBins = getValues(GmosCommonType.Binning.class, properties, ConfigKeyGmos.Values.XBIN);
        Set<GmosCommonType.Binning> yBins = getValues(GmosCommonType.Binning.class, properties, ConfigKeyGmos.Values.YBIN);
        Set<GmosCommonType.Order> orders = getValues(GmosCommonType.Order.class, properties, ConfigKeyGmos.Values.ORDER);
        Set<GmosCommonType.AmpGain> gains = getValues(GmosCommonType.AmpGain.class, properties, ConfigKeyGmos.Values.GAIN);
        Set<GmosSouthType.DisperserSouth> dispersers = getValues(GmosSouthType.DisperserSouth.class, properties, ConfigKeyGmos.Values.DISPERSER);
        Set<GmosSouthType.FilterSouth> filters = getValues(GmosSouthType.FilterSouth.class, properties, ConfigKeyGmos.Values.FILTER);
        Set<GmosSouthType.FPUnitSouth> fpus = getValues(GmosSouthType.FPUnitSouth.class, properties, ConfigKeyGmos.Values.FPU);

        // create all possible combinations and produce a key for each one
        Set<ConfigurationKey> keys = new HashSet<ConfigurationKey>();
        for (GmosCommonType.Binning xBin : xBins) {
            for (GmosCommonType.Binning yBin : yBins) {
                for (GmosCommonType.Order order : orders) {
                    for (GmosCommonType.AmpGain gain : gains) {
                        for (GmosSouthType.DisperserSouth disperser : dispersers) {
                            for (GmosSouthType.FilterSouth filter : filters) {
                                for (GmosSouthType.FPUnitSouth fpu : fpus) {

                                    ConfigKeyGmosSouth key =
                                            new ConfigKeyGmosSouth(
                                                    disperser, filter, fpu, xBin, yBin, order, gain
                                            );

                                    keys.add(key);

                                }
                            }

                        }
                    }
                }
            }
        }

        // return the set of keys we just came up with
        return keys;
    }

    // GMOS-S central wavelength is stored as nm.
    protected int toAngstroms(double wl) {
        return (int) Math.round(wl * 10);
    }

}
