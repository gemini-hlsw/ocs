package edu.gemini.itc.shared;

import edu.gemini.itc.shared.*;

import java.io.Serializable;

/**
 * Container for observation detail parameters.
 */
public final class ObservationDetails implements Serializable {

    private final CalculationMethod calculationMethod;
    private final AnalysisMethod analysisMethod;

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
        if      (calculationMethod instanceof ImagingSN)      return ((ImagingSN)      calculationMethod).time();
        else if (calculationMethod instanceof SpectroscopySN) return ((SpectroscopySN) calculationMethod).time();
        else if (calculationMethod instanceof ImagingInt)     return ((ImagingInt)     calculationMethod).expTime();
        else    throw new IllegalArgumentException();
    }

    public double getSourceFraction() {
        return calculationMethod.fraction();
    }

    // TODO: make sure this is only called where applicable!
    public double getSNRatio() {
        if      (calculationMethod instanceof ImagingInt)     return ((ImagingInt) calculationMethod).sigma();
        else    return 0.0;
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

    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Calculation Method:\t" + getMethod() + "\n");
        sb.append("Num Exposures:\t\t" + getNumExposures() + "\n");
        sb.append("Exposure Time:\t\t" + getExposureTime() + "\n");
        sb.append("Fraction on Source:\t" + getSourceFraction() + "\n");
        sb.append("SN Ratio:\t\t" + getSNRatio() + "\n");
        sb.append("Aperture Type:\t\t" + (isAutoAperture() ? "autoAper" : "userAper") + "\n");
        sb.append("Aperture Diameter:\t" + getApertureDiameter() + "\n");
        sb.append("\n");
        return sb.toString();
    }

}
