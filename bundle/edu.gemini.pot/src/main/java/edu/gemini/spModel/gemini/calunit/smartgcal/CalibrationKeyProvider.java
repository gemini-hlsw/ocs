package edu.gemini.spModel.gemini.calunit.smartgcal;

import edu.gemini.spModel.data.config.ISysConfig;

/**
 * Interface that has to be implemented by instruments that support smart calibrations.
 */
public interface CalibrationKeyProvider {

    /**
     * Extracts a key that describes the instrument configuration (as far as it is relevant for calibration purposes)
     * and which can be used to lookup the smart calibration values in the calibration tables.
     * @param instrumentConfig
     * @return
     */
    CalibrationKey extractKey(ISysConfig instrumentConfig);

}
