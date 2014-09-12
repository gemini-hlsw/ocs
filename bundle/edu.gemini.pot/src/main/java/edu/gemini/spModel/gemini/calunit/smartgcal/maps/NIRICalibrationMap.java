package edu.gemini.spModel.gemini.calunit.smartgcal.maps;

import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyNiri;
import edu.gemini.spModel.gemini.niri.Niri;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 */
public class NIRICalibrationMap extends SimpleCalibrationMap {

    public NIRICalibrationMap(Version version) {
        super(version);
    }

    /** {@inheritDoc} */
    @Override public ConfigurationKey.Values[] getKeyValueNames() {
        return ConfigKeyNiri.Values.values();
    }

    /** {@inheritDoc} */
    @Override public Set<ConfigurationKey> createConfig(Properties properties) {

        // lookup values
        Set<Niri.Disperser> dispersers = getValues(Niri.Disperser.class, properties, ConfigKeyNiri.Values.DISPERSER);
        Set<Niri.Filter> filters = getValues(Niri.Filter.class, properties, ConfigKeyNiri.Values.FILTER);
        Set<Niri.Mask> masks = getValues(Niri.Mask.class, properties, ConfigKeyNiri.Values.MASK);
        Set<Niri.Camera> cameras = getValues(Niri.Camera.class, properties, ConfigKeyNiri.Values.CAMERA);
        Set<Niri.BeamSplitter> beamSplitters = getValues(Niri.BeamSplitter.class, properties, ConfigKeyNiri.Values.BEAM_SPLITTER);

        // create all possible combinations and produce a key for each one
        Set<ConfigurationKey> keys = new HashSet<ConfigurationKey>();
        for (Niri.Disperser disperser : dispersers) {
            for (Niri.Filter filter : filters) {
                for (Niri.Mask mask : masks) {
                    for (Niri.Camera camera : cameras) {
                        for (Niri.BeamSplitter beamSplitter : beamSplitters) {
                            ConfigKeyNiri key =
                                    new ConfigKeyNiri(
                                            disperser, filter, mask, camera,beamSplitter
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
}
