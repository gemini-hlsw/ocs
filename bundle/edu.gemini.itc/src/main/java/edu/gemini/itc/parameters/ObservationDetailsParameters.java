package edu.gemini.itc.parameters;

import edu.gemini.itc.shared.*;


/**
 * This class holds the information from the Observation Details section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class ObservationDetailsParameters extends ITCParameters {

    public static enum CalcMethod {
        IMAGING_SN,
        IMAGING_SN_TOTAL,
        IMAGING_INT,
        SPECTROSCOPY
    }
    public static enum AnMethod {
        AUTO_APERTURE,
        USER_APERTURE
    }

    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String CALC_METHOD = "calcMethod";
    public static final String CALC_MODE = "calcMode";
    //define the parameters for imaging method A and C.
    public static final String NUM_EXPOSURES = "numExpA";
    public static final String EXP_TIME = "expTimeA";
    public static final String EXP_TIME_2 = "expTimeC";
    public static final String TOTAL_OBSERVATION_TIME = "totTimeA";
    public static final String SRC_FRACTION = "fracOnSourceA";
    public static final String SRC_FRACTION_2 = "fracOnSourceC";
    public static final String SIGMA = "sigmaC";
    //define the parameters for spectroscopy method A


    public static final String APER_TYPE = "aperType";
    public static final String APER_DIAM = "userAperDiam";
    public static final String AUTO_SKY_APER = "autoSkyAper";
    public static final String USER_SKY_APER = "userSkyAper";

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String S2N = "s2n";
    public static final String INTTIME = "intTime";
    public static final String IMAGING = "imaging";
    public static final String SPECTROSCOPY = "spectroscopy";
    public static final String AUTO_APER = "autoAper";
    public static final String USER_APER = "userAper";

    // Data members
    private final String _calcMode; //imgaging or spectroscopy
    private final String _calcMethod;  // S/N given time or time given S/N
    private int _numExposures;
    private double _exposureTime;   // in seconds
    private double _totalObservationTime;  // Total observation Time, some instruments use this.
    private double _sourceFraction;  // fraction of exposures containing source
    private double _snRatio;  // ratio desired

    private final String _apertureType; // auto or user
    private double _apertureDiameter; // in arcsec
    private double _skyApertureDiameter;

    /**
     * Constructs a ObservationDetailsParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public ObservationDetailsParameters(ITCMultiPartParser p) {
        _calcMode = p.getParameter(CALC_MODE);
        _calcMethod = p.getParameter(CALC_METHOD);

        if (_calcMethod.equals(S2N)) {
            if (p.parameterExists(NUM_EXPOSURES) && p.parameterExists(EXP_TIME)) {
                _numExposures = ITCParameters.parseInt(p.getParameter(NUM_EXPOSURES), "Number of Exposures");
                if (_numExposures < 0) _numExposures *= -1;
                _exposureTime = ITCParameters.parseDouble(p.getParameter(EXP_TIME), "Exposure Time");
                if (_exposureTime < 0) _exposureTime *= -1;
            } else {
                if (p.parameterExists(TOTAL_OBSERVATION_TIME)) {
                    _totalObservationTime = ITCParameters.parseInt(p.getParameter(TOTAL_OBSERVATION_TIME), "Total Observation Time");
                } else {
                    p.getParameter(TOTAL_OBSERVATION_TIME);  // DUMMY Parameter to throw an exception.
                }
            }
            _sourceFraction = ITCParameters.parseDouble(p.getParameter(SRC_FRACTION), "Exposures Containing Source");
            if (_sourceFraction < 0) _sourceFraction *= -1;
        } else if (_calcMethod.equals(INTTIME)) {
            if (_calcMode.equals(SPECTROSCOPY)) {
                throw new IllegalArgumentException("Total integration time to achieve a specific \nS/N ratio is not supported in spectroscopy mode.  \nPlease select the Total S/N method. ");
            }
            if (p.parameterExists(EXP_TIME_2)) {
                _exposureTime = ITCParameters.parseDouble(p.getParameter(EXP_TIME_2), "Exposure Time");
                if (_exposureTime < 0) _exposureTime *= -1;
            } else
                _totalObservationTime = 1;  //_totalObservationTime not used for int time just set it to 1.
            _snRatio = ITCParameters.parseDouble(p.getParameter(SIGMA), "Sigma");
            if (_snRatio < 0) _snRatio *= -1;
            _sourceFraction = ITCParameters.parseDouble(p.getParameter(SRC_FRACTION_2), "Exposures Containing Source");
            if (_sourceFraction < 0) _sourceFraction *= -1;

        } else {
            throw new IllegalArgumentException("Unrecognized calculation mode: " + getCalculationMode());
        }

        // Aperture Section
        String skyAper;
        _apertureType = p.getParameter(APER_TYPE);
        if (_apertureType.equals(AUTO_APER)) {
            skyAper = p.getParameter(AUTO_SKY_APER);
        } else if (_apertureType.equals(USER_APER)) {
            skyAper = p.getParameter(USER_SKY_APER);
            _apertureDiameter = ITCParameters.parseDouble(p.getParameter(APER_DIAM), "Aperture Diameter");
            if (_apertureDiameter < 0) _apertureDiameter *= -1;
        } else {
            throw new IllegalArgumentException("Unrecognized Aperture type: " + _apertureType + ". Contact Helpdesk.");
        }
        _skyApertureDiameter = ITCParameters.parseDouble(skyAper, "Sky Aperture Diameter");
        if (_skyApertureDiameter < 0) _skyApertureDiameter *= -1;
        if (_skyApertureDiameter < 1)
            throw new IllegalArgumentException("The Sky aperture: " + _skyApertureDiameter + " must be 1 or greater.  Please retype the value and resubmit.");

    }


    public ObservationDetailsParameters(String calcMode,
                                        String calcMethod,
                                        int numExposures,
                                        double exposureTime,
                                        double sourceFraction,
                                        double snRatio,
                                        String apertureType,
                                        double apertureDiameter,
                                        double skyApertureDiameter) {
        _calcMode = calcMode;
        _calcMethod = calcMethod;
        _numExposures = numExposures;
        _exposureTime = exposureTime;
        _sourceFraction = sourceFraction;
        _snRatio = snRatio;
        _apertureType = apertureType;
        _apertureDiameter = apertureDiameter;
        _skyApertureDiameter = skyApertureDiameter;

    }

    public String getCalculationMethod() {
        return _calcMethod;
    }

    public String getCalculationMode() {
        return _calcMode;
    }

    public int getNumExposures() {
        return _numExposures;
    }

    public void setNumExposures(int numExposures) {
        _numExposures = numExposures;
    }

    public double getExposureTime() {
        return _exposureTime;
    }

    public void setExposureTime(double exposureTime) {
        _exposureTime = exposureTime;
    }

    public double getTotalObservationTime() {
        return _totalObservationTime;
    }

    public void setTotalObservationTime(double totalObservationTime) {
        _totalObservationTime = totalObservationTime;
    }

    public double getSourceFraction() {
        return _sourceFraction;
    }

    public double getSNRatio() {
        return _snRatio;
    }

    public String getApertureType() {
        return _apertureType;
    }

    public double getApertureDiameter() {
        return _apertureDiameter;
    }

    public double getSkyApertureDiameter() {
        return _skyApertureDiameter;
    }

    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Calculation Method:\t" + getCalculationMethod() + "\n");
        sb.append("Num Exposures:\t\t" + getNumExposures() + "\n");
        sb.append("Exposure Time:\t\t" + getExposureTime() + "\n");
        sb.append("Fraction on Source:\t" + getSourceFraction() + "\n");
        sb.append("SN Ratio:\t\t" + getSNRatio() + "\n");
        sb.append("Aperture Type:\t\t" + getApertureType() + "\n");
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
        sb.append("<LI>mode: " + getCalculationMode() + "\n");
        sb.append("<LI>Calculation of ");
        if (getCalculationMethod().equals(S2N)) {
            sb.append("S/N ratio with " + getNumExposures() + " exposures of " + device.toString(getExposureTime()) + " secs,");
            sb.append(" and " + device.toString(getSourceFraction() * 100) + " % of them were on source.\n");
        } else {
            sb.append("integration time from a S/N ratio of " + device.toString(getSNRatio()) + " for exposures of");
            sb.append(" " + device.toString(getExposureTime()) + " with " + device.toString(getSourceFraction() * 100) + " % of them were on source.\n");
        }
        sb.append("<LI>Analysis performed for aperture ");
        if (getApertureType().equals(AUTO_APER)) {
            sb.append("that gives 'optimum' S/N ");
        } else {
            sb.append("of diameter " + device.toString(getApertureDiameter()) + " ");
        }
        sb.append("and a sky aperture that is " + device.toString(getSkyApertureDiameter()) + " times the target aperture.\n");

        return sb.toString();
    }


}
