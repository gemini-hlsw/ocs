package edu.gemini.spModel.gemini.calunit.smartgcal.maps;

import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyIgrins2;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;


public class Igrins2CalibrationMap extends SimpleCalibrationMap {

    public Igrins2CalibrationMap(Version version) {
        super(version);
    }

    @Override public ConfigurationKey.Values[] getKeyValueNames() {
        return new ConfigurationKey.Values[0];
    }

    @Override public Set<ConfigurationKey> createConfig(Properties properties) {
        return Collections.singleton(ConfigKeyIgrins2.INSTANCE);
    }
}
