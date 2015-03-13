package edu.gemini.itc.parameters;

import edu.gemini.itc.shared.*;

/**
 * This class holds the information from the Observation Details section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class ObservationDetailsParameters extends ITCParameters {

    private final CalculationMethod calculationMethod;
    private final AnalysisMethod analysisMethod;

    public ObservationDetailsParameters(final CalculationMethod calculationMethod, final AnalysisMethod analysisMethod) {
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

    public String printParameterSummary() {
        StringBuffer sb = new StringBuffer();

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();

        sb.append("Calculation and analysis methods:\n");
        sb.append("<LI>mode: " + (calculationMethod.isImaging() ? "imaging" : "spectroscopy") + "\n");
        sb.append("<LI>Calculation of ");
        if (getMethod().isS2N()) {
            sb.append("S/N ratio with " + getNumExposures() + " exposures of " + device.toString(getExposureTime()) + " secs,");
            sb.append(" and " + device.toString(getSourceFraction() * 100) + " % of them were on source.\n");
        } else {
            sb.append("integration time from a S/N ratio of " + device.toString(getSNRatio()) + " for exposures of");
            sb.append(" " + device.toString(getExposureTime()) + " with " + device.toString(getSourceFraction() * 100) + " % of them were on source.\n");
        }
        sb.append("<LI>Analysis performed for aperture ");
        if (isAutoAperture()) {
            sb.append("that gives 'optimum' S/N ");
        } else {
            sb.append("of diameter " + device.toString(getApertureDiameter()) + " ");
        }
        sb.append("and a sky aperture that is " + device.toString(getSkyApertureDiameter()) + " times the target aperture.\n");

        return sb.toString();
    }


}
