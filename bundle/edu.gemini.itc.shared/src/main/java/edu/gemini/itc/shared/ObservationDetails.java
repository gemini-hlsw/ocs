package edu.gemini.itc.shared;

import java.io.Serializable;

/**
 * Container for observation detail parameters.
 */
public final class ObservationDetails implements Serializable {

    public final CalculationMethod calculationMethod;
    public final AnalysisMethod analysisMethod;

    public ObservationDetails(final CalculationMethod calculationMethod, final AnalysisMethod analysisMethod) {
        this.calculationMethod = calculationMethod;
        this.analysisMethod = analysisMethod;
    }

    public CalculationMethod getMethod() {
        return calculationMethod;
    }

    public AnalysisMethod getAnalysis() {
        return analysisMethod;
    }

    // TODO: make sure this is only called where applicable!
    public int getNumExposures() {
        if      (calculationMethod instanceof ImagingSN)      return ((ImagingSN)      calculationMethod).exposures();
        else if (calculationMethod instanceof SpectroscopySN) return ((SpectroscopySN) calculationMethod).exposures();
        else    return 0;
    }

    // TODO: make sure this is only called where applicable!
    public double getExposureTime() {
        return calculationMethod.exposureTime();
    }

    public double getSourceFraction() {
        return calculationMethod.sourceFraction();
    }

    public boolean isAutoAperture() {
        return analysisMethod instanceof AutoAperture;
    }

    // TODO: make sure this is only called where applicable!
    public double getApertureDiameter() {
        if     (analysisMethod instanceof UserAperture)        return (((UserAperture) analysisMethod).diameter());
        else   return 0.0;
    }

    public double getSkyApertureDiameter() {
        return analysisMethod.skyAperture();
    }

}
