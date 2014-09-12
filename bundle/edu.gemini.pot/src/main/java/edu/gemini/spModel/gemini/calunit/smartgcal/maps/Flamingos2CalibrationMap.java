package edu.gemini.spModel.gemini.calunit.smartgcal.maps;

import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyFlamingos2;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;

import java.util.*;

/**
 * Map for Flamingos2 smart calibrations lookup.
 */
public class Flamingos2CalibrationMap extends SimpleCalibrationMap {

    public Flamingos2CalibrationMap(Version version) {
        super(version);
    }

    /** {@inheritDoc} */
    @Override public ConfigurationKey.Values[] getKeyValueNames() {
        return ConfigKeyFlamingos2.Values.values();
    }

    /** {@inheritDoc} */
    @Override public Set<ConfigurationKey> createConfig(Properties properties) {

        // lookup values
        Set<Flamingos2.Disperser> dispersers = getValues(Flamingos2.Disperser.class, properties, ConfigKeyFlamingos2.Values.DISPERSER);
        Set<Flamingos2.Filter> filters = getValues(Flamingos2.Filter.class, properties, ConfigKeyFlamingos2.Values.FILTER);
        Set<Flamingos2.FPUnit> fpUnits = getValues(Flamingos2.FPUnit.class, properties, ConfigKeyFlamingos2.Values.FPU);

        // create all possible combinations and produce a key for each one
        Set<ConfigurationKey> keys = new HashSet<ConfigurationKey>();
        for (Flamingos2.Disperser disperser : dispersers) {
            for (Flamingos2.Filter filter : filters) {
                for (Flamingos2.FPUnit fpUnit : fpUnits) {
                    ConfigKeyFlamingos2 key =
                            new ConfigKeyFlamingos2(
                                    disperser, filter, fpUnit
                            );

                    keys.add(key);
                }
            }
        }

        // return the set of keys we just came up with
        return keys;
    }
}
