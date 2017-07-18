package edu.gemini.spModel.gemini.calunit.smartgcal.maps;

import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyNifs;
import edu.gemini.spModel.gemini.nifs.NIFSParams;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 */
public final class NIFSCalibrationMap extends CentralWavelengthMap {

    public NIFSCalibrationMap(Version version) {
        super(version);
    }

    @Override
    public ConfigurationKey.Values[] getKeyValueNames() {
        return ConfigKeyNifs.Values.values();
    }

    @Override
    public Set<ConfigurationKey> createConfig(Properties properties) {

        // lookup values
        Set<NIFSParams.Disperser> dispersers = getValues(NIFSParams.Disperser.class, properties, ConfigKeyNifs.Values.DISPERSER);
        Set<NIFSParams.Filter> filters = getValues(NIFSParams.Filter.class, properties, ConfigKeyNifs.Values.FILTER);
        Set<NIFSParams.Mask> masks = getValues(NIFSParams.Mask.class, properties, ConfigKeyNifs.Values.FOCAL_PLANE_MASK);

        // create all possible combinations and produce a key for each one
        Set<ConfigurationKey> keys = new HashSet<ConfigurationKey>();
        for (NIFSParams.Disperser disperser : dispersers) {
            for (NIFSParams.Filter filter : filters) {
                for (NIFSParams.Mask mask : masks) {
                    ConfigKeyNifs key =
                            new ConfigKeyNifs(
                                    disperser, filter, mask
                            );

                    keys.add(key);
                }
            }
        }

        // return the set of keys we just came up with
        return keys;
    }

    // NIFS central wavelength is stored as um.
    protected int toAngstroms(double wl) {
        return (int) Math.round(wl * 10000);
    }
}
