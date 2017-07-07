package edu.gemini.spModel.gemini.calunit.smartgcal.maps;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;

import java.util.*;
import java.util.stream.Stream;

/**
 */
public abstract class SimpleCalibrationMap extends BaseCalibrationMap {

    private Map<ConfigurationKey, List<Calibration>> map;

    public SimpleCalibrationMap(Version version) {
        super(version);
        map = new HashMap<ConfigurationKey, List<Calibration>>();
    }

    /** {@inheritDoc} */
    @Override public List<Calibration> get(ConfigurationKey key, Double centralWavelength) {
        throw new RuntimeException();
    }

    public Calibration put(ConfigurationKey key, Properties properties, Calibration calibration) {
        List<Calibration> calibrations = map.get(key);
        if (calibrations == null) {
            calibrations = new ArrayList<Calibration>();
            map.put(key, calibrations);
        }
        calibrations.add(calibration);
        return calibration;
    }

    public List<Calibration> get(ConfigurationKey key) {
        List<Calibration> calibrations = map.get(key);
        if (calibrations == null) {
            return Collections.emptyList();
        }
        return calibrations;
    }

    /**
     * Export the calibration map to a list of String suitable for writing to a
     * configuration file.
     */
    @Override
    public Stream<ImList<String>> export() {
        return map.entrySet().stream().flatMap(me -> {
            final ImList<String> key = me.getKey().export();
            return me.getValue().stream().map(cal -> key.append(cal.export()));
        });
    }
}
