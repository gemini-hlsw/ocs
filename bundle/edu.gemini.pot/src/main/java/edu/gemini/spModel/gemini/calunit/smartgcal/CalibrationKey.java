package edu.gemini.spModel.gemini.calunit.smartgcal;

import java.io.Serializable;

public interface CalibrationKey extends Serializable {

    ConfigurationKey getConfig();

    public interface WithWavelength extends Serializable {
        Double getWavelength();
    }

}
