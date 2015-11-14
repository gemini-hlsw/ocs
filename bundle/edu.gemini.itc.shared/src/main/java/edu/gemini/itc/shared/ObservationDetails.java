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
        if      (calculationMethod instanceof S2NMethod) return ((S2NMethod)      calculationMethod).exposures();
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

    public double getSkyApertureDiameter() {
        return analysisMethod.skyAperture();
    }

}
