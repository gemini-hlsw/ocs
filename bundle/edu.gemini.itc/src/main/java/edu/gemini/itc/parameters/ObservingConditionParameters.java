package edu.gemini.itc.parameters;

import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;

import javax.servlet.http.HttpServletRequest;

/**
 * This class holds the information from the Observing Condition section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class ObservingConditionParameters extends ITCParameters {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.

    public static final String IMAGE_QUALITY = "imageQuality";
    public static final String SKY_TRANS_CLOUD = "skyTransCloud";
    public static final String SKY_TRANS_WATER = "skyTransWater";
    public static final String SKY_BACKGROUND = "skyBackground";
    public static final String AIRMASS = "airMass";

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.

    // Data members
    private int _imageQuality;
    private int _skyTransCloud;
    private int _skyTransWater;
    private int _skyBackground;
    private double _airmass;

    /**
     * Constructs a ObservingConditionParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public ObservingConditionParameters(HttpServletRequest r) {
        parseServletRequest(r);
    }

    /**
     * Constructs a ObservingConditionParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public ObservingConditionParameters(ITCMultiPartParser p) {
        parseMultipartParameters(p);
    }

    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) {
        // Parse the observing condition section of the form.

        String s;

        // Get image quality
        s = r.getParameter(IMAGE_QUALITY);
        if (s == null) {
            ITCParameters.notFoundException(IMAGE_QUALITY);
        }
        _imageQuality = ITCParameters.parseInt(s, "Image quality");

        // Get sky transparency (cloud)
        s = r.getParameter(SKY_TRANS_CLOUD);
        if (s == null) {
            ITCParameters.notFoundException(SKY_TRANS_CLOUD);
        }
        _skyTransCloud =
                ITCParameters.parseInt(s, "Sky transparency (cloud cover)");

        // Get sky transparency (water)
        s = r.getParameter(SKY_TRANS_WATER);
        if (s == null) {
            ITCParameters.notFoundException(SKY_TRANS_WATER);
        }
        _skyTransWater =
                ITCParameters.parseInt(s, "Sky transparency (water vapour)");

        // Get sky background
        s = r.getParameter(SKY_BACKGROUND);
        if (s == null) {
            ITCParameters.notFoundException(SKY_BACKGROUND);
        }
        _skyBackground = ITCParameters.parseInt(s, "Sky background");

        // Get airmass
        s = r.getParameter(AIRMASS);
        if (s == null) {
            ITCParameters.notFoundException(AIRMASS);
        }
        _airmass = ITCParameters.parseDouble(s, "Airmass");
    }

    public void parseMultipartParameters(ITCMultiPartParser p) {
        _imageQuality = ITCParameters.parseInt(p.getParameter(IMAGE_QUALITY), "Image Quality");
        _skyTransCloud = ITCParameters.parseInt(p.getParameter(SKY_TRANS_CLOUD), "Sky transparency (cloud cover)");
        _skyTransWater = ITCParameters.parseInt(p.getParameter(SKY_TRANS_WATER), "Sky transparency (water vapour)");
        _skyBackground = ITCParameters.parseInt(p.getParameter(SKY_BACKGROUND), "Sky background");
        _airmass = ITCParameters.parseDouble(p.getParameter(AIRMASS), "Airmass");
    }

    /**
     * Constructs a ObservingConditionParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public ObservingConditionParameters(int imageQuality,
                                        int skyTransCloud,
                                        int skyTransWater,
                                        int skyBackground,
                                        double airmass) {
        _imageQuality = imageQuality;
        _skyTransCloud = skyTransCloud;
        _skyTransWater = skyTransWater;
        _skyBackground = skyBackground;
        _airmass = airmass;
    }

    public int getImageQuality() {
        return _imageQuality;
    }

    public double getImageQualityPercentile() {
        if (getImageQuality() == 1)
            return .2;
            // else if (getImageQuality() == 2) return .5; //old bin changed to .7 BDW 9-4-02
            // else if (getImageQuality() == 3) return .8; //old bin changed to .85 BDW 9-4-02
        else if (getImageQuality() == 2)
            return .7;
        else if (getImageQuality() == 3)
            return .85;
        else
            return 1.0;
    }

    public int getSkyTransparencyCloud() {
        return _skyTransCloud;
    }

    public double getSkyTransparencyCloudPercentile() {
        if (getSkyTransparencyCloud() == 1)
            return .2;
        else if (getSkyTransparencyCloud() == 2)
            return .5;
        else if (getSkyTransparencyCloud() == 3)
            return .7;
        else if (getSkyTransparencyCloud() == 4)
            return .8;
        else
            return 1.0;
    }

    public int getSkyTransparencyWater() {
        return _skyTransWater;
    }

    public double getSkyTransparencyWaterPercentile() {
        if (getSkyTransparencyWater() == 1)
            return .2;
        else if (getSkyTransparencyWater() == 2)
            return .5;
        else if (getSkyTransparencyWater() == 3)
            return .8;
        else
            return 1.0;
    }

    public String getSkyTransparencyWaterCategory() {
        if (getSkyTransparencyWater() == 1)
            return "20";
        else if (getSkyTransparencyWater() == 2)
            return "50";
        else if (getSkyTransparencyWater() == 3)
            return "80";
        else
            return "100";
    }


    public int getSkyBackground() {
        return _skyBackground;
    }

    public double getSkyBackgroundPercentile() {
        if (getSkyBackground() == 1)
            return .2;
        else if (getSkyBackground() == 2)
            return .5;
        else if (getSkyBackground() == 3)
            return .8;
        else
            return 1.0;
    }

    public String getSkyBackgroundCategory() {
        if (_skyBackground == 1)
            return "20";
        else if (_skyBackground == 2)
            return "50";
        else if (_skyBackground == 3)
            return "80";
        else
            return "100";
    }

    public double getAirmass() {
        return _airmass;
    }

    public String getAirmassCategory() {
        if (_airmass <= 1.25)
            return "10";
        else if (_airmass > 1.25 && _airmass <= 1.75)
            return "15";
        else
            return "20";
    }


    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Image Quality:\t" + getImageQuality() + "\n");
        sb.append("Cloud Trans:\t" + getSkyTransparencyCloud() + "\n");
        sb.append("Water Trans:\t" + getSkyTransparencyWater() + "\n");
        sb.append("Sky Background:\t" + getSkyBackground() + "\n");
        sb.append("Airmass:\t" + getAirmass() + "\n");
        sb.append("\n");
        return sb.toString();
    }

    public String printParameterSummary() {
        StringBuffer sb = new StringBuffer();

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();


        sb.append("Observing Conditions:");
        sb.append("<LI> Image Quality: " + device.toString(getImageQualityPercentile() * 100) + "%");
        sb.append("<LI> Sky Transparency (cloud cover): " + device.toString(getSkyTransparencyCloudPercentile() * 100) + "%");
        sb.append("<LI> Sky transparency (water vapour): " + device.toString(getSkyTransparencyWaterPercentile() * 100) + "%");
        sb.append("<LI> Sky background: " + device.toString(getSkyBackgroundPercentile() * 100) + "%");
        sb.append("<LI> Airmass: " + device.toString(getAirmass()));
        sb.append("<BR>");

        sb.append("Frequency of occurrence of these conditions: " +
                        device.toString(getImageQualityPercentile() *
                                getSkyTransparencyCloudPercentile() *
                                getSkyTransparencyWaterPercentile() *
                                getSkyBackgroundPercentile() * 100)
                        + "%<BR>"
        );

        return sb.toString();
    }


}
