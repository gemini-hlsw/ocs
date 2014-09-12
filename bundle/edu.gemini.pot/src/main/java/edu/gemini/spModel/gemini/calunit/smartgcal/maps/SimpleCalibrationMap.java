package edu.gemini.spModel.gemini.calunit.smartgcal.maps;


import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;

import java.util.*;

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

}
