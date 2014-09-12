package edu.gemini.spModel.gemini.calunit.smartgcal.maps;

import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGpi;
import edu.gemini.spModel.gemini.gpi.Gpi;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 */
public class GpiCalibrationMap extends SimpleCalibrationMap {

    public GpiCalibrationMap(Version version) {
        super(version);
    }

    @Override public ConfigurationKey.Values[] getKeyValueNames() {
        return ConfigKeyGpi.Values.values();
    }

    @Override public Set<ConfigurationKey> createConfig(Properties properties) {

        // lookup values
        Set<Gpi.ObservingMode> modes = getValues(Gpi.ObservingMode.class, properties, ConfigKeyGpi.Values.MODE);
        Set<Gpi.Disperser> dispersers = getValues(Gpi.Disperser.class, properties, ConfigKeyGpi.Values.DISPERSER);

        // create all possible combinations and produce a key for each one
        Set<ConfigurationKey> keys = new HashSet<ConfigurationKey>();
        for (Gpi.ObservingMode mode : modes) {
            for (Gpi.Disperser disperser : dispersers) {
                ConfigKeyGpi key = new ConfigKeyGpi(mode, disperser);
                keys.add(key);
            }
        }

        // return the set of keys we just came up with
        return keys;
    }
}
