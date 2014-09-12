package edu.gemini.spModel.gemini.calunit.calibration;

import edu.gemini.spModel.gemini.calunit.CalUnitParams.Lamp;

/**
 * An abstract implementation of IndexedCalibrationStep which defines the
 * arc/flat methods.
 */
public abstract class AbstractIndexedCalibrationStep implements IndexedCalibrationStep {
    @Override public Boolean isArc() {
        for (Lamp lamp : getLamps()) if (lamp.isArc()) return true;
        return false;
    }

    @Override public Boolean isFlat() { return !isArc(); }
}
