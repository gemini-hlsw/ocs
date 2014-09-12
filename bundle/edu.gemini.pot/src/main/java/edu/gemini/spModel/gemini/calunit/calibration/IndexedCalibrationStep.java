package edu.gemini.spModel.gemini.calunit.calibration;

/**
 * A particular calibration step in a sequence.  Adds an index number to a
 * CalibrationStep.
 */
public interface IndexedCalibrationStep extends CalibrationStep {

    /**
     * Gets the index of the calibration step within the sequence.
     */
    Integer getIndex();
    String getObsClass();
}
