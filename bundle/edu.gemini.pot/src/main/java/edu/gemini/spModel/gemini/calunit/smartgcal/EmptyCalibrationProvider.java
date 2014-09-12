package edu.gemini.spModel.gemini.calunit.smartgcal;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A default/empty calibration provider that always returns no calibration
 * information.
 */
public enum EmptyCalibrationProvider implements CalibrationProvider {
    instance;

    private static final Date timestamp = new Date();

    @Override
    public List<Calibration> getCalibrations(CalibrationKey key) {
        return Collections.emptyList();
    }

    @Override
    public Version getVersion(Calibration.Type type, String instrument) {
        return new Version(0, timestamp);
    }

    @Override
    public List<VersionInfo> getVersionInfo() {
        return Collections.emptyList();
    }

}
