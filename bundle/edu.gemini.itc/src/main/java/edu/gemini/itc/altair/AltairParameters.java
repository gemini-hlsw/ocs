package edu.gemini.itc.altair;

import edu.gemini.itc.parameters.TeleParameters;
import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;

import javax.servlet.http.HttpServletRequest;


/**
 * This class holds the information from the Altair section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class AltairParameters extends ITCParameters {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.

    public static final String GUIDE_SEPERATION = "guideSep";
    public static final String GUIDE_MAG = "guideMag";
    public static final String FIELD_LENS = "fieldLens";
    public static final String WFS_MODE = "wfsMode";
    public static final String LGS = "laserGuideStar";
    public static final String NGS = "naturalGuideStar";

    private static final String FIELD_LENS_IN = "IN";
    private static final String FIELD_LENS_OUT = "OUT";


    // Data members
    private boolean _altairUsed = true; // true or false
    private String _wfsMode;
    private double _guideStarSeperation;
    private double _guideStarMagnitude;
    private String _fieldLens;
    private TeleParameters.Wfs _wfs;

    /**
     * Constructs a PlottingDetailsParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public AltairParameters(HttpServletRequest r) {
        parseServletRequest(r);
    }

    /**
     * Constructs a AltairParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public AltairParameters(ITCMultiPartParser p) {
        parseMultipartParameters(p);
    }

    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) {
        // Parse the Plotting details section of the form.


        String guideStarSeperation = r.getParameter(GUIDE_SEPERATION);
        if (guideStarSeperation == null) {
            _altairUsed = false;
        }
        _guideStarSeperation = ITCParameters.parseDouble(guideStarSeperation, "Seperation of Guide Star");
        if (_guideStarSeperation < 0 || _guideStarSeperation > 22)
            throw new IllegalArgumentException(" Altair Guide star distance must be between 0 and 22");

        String guideStarMagnitude = r.getParameter(GUIDE_MAG);
        if (guideStarMagnitude == null) {
            _altairUsed = false;
        }
        _guideStarMagnitude = ITCParameters.parseDouble(guideStarMagnitude, "Guide Star Magnitude");
        if (_guideStarMagnitude > 17 || _guideStarMagnitude < 7)
            throw new IllegalArgumentException(" Altair Guide star Magnitude must be between 7 and 17 ");

    }

    public void parseMultipartParameters(ITCMultiPartParser p) {
        _guideStarSeperation = ITCParameters.parseDouble(p.getParameter(GUIDE_SEPERATION), "Seperation fo Guide Star");
        _guideStarMagnitude = ITCParameters.parseDouble(p.getParameter(GUIDE_MAG), "Guide Star Magnitude");
        _fieldLens = p.getParameter(FIELD_LENS);
        _wfs = getParameter(TeleParameters.Wfs.class, p);
        _wfsMode = p.getParameter(WFS_MODE);

        if (_wfs == TeleParameters.Wfs.AOWFS)
            _altairUsed = true;
        else
            _altairUsed = false;


        if (_guideStarSeperation < 0 || _guideStarSeperation > 25)
            throw new IllegalArgumentException(" Altair Guide star distance must be between 0 and 25 arcsecs.");

        if (_wfsMode.equals(LGS) && _guideStarMagnitude > 19.5)
            throw new IllegalArgumentException(" Altair Guide star Magnitude must be <= 19.5 in R for LGS mode. ");

        if (_wfsMode.equals(NGS) && _guideStarMagnitude > 15.5)
            throw new IllegalArgumentException(" Altair Guide star Magnitude must be <= 15.5 in R for NGS mode. ");


        if (_wfsMode.equals(LGS) && _fieldLens.equals(FIELD_LENS_OUT))
            throw new IllegalArgumentException("The field Lens must be IN when Altair is in LGS mode.");
    }


    /**
     * Constructs a AltairParameters from a servlet request
     *
     * @throws Exception if input data is not parsable.
     */
    public AltairParameters(
            double guideStarSeperation,
            double guideStarMagnitude,
            String fieldLens,
            String wfsMode,
            boolean altairUsed) {
        _guideStarSeperation = guideStarSeperation;
        _guideStarMagnitude = guideStarMagnitude;
        _fieldLens = fieldLens;
        _wfsMode = wfsMode;
        _altairUsed = altairUsed;

    }


    public boolean altairIsUsed() {
        return _altairUsed;
    }

    public double getGuideStarSeperation() {
        return _guideStarSeperation;
    }

    public double getGuideStarMagnitude() {
        return _guideStarMagnitude;
    }

    public String getWFSMode() {
        return _wfsMode;
    }

    public boolean fieldLensIsUsed() {
        if (_fieldLens.equals(FIELD_LENS_IN)) {
            return true;
        } else
            return false;
    }


    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Guide Star Seperation:\t" + getGuideStarSeperation() + "\n");
        sb.append("Guide Star Magnitude:\t" + getGuideStarMagnitude() + "\n");
        sb.append("Field Lens:\t" + _fieldLens + "\n");
        if (getWFSMode().equals(NGS))
            sb.append("Altair Mode:\t Natural guide star");
        else
            sb.append("Altair Mode:\t Laser guide star");

        sb.append("\n");
        return sb.toString();
    }

    public String printParameterSummary() {
        StringBuffer sb = new StringBuffer();

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();

        sb.append("Altair Guide Star properties:");
        if (_wfsMode.equals(LGS)) {
            sb.append("<LI>Laser Guide Star Mode");

        } else {
            sb.append("<LI>Natural Guide Star Mode");
            sb.append("<LI>Guide Star Seperation " + getGuideStarSeperation());
            sb.append("<LI>Guide Star Magnitude " + getGuideStarMagnitude());
        }

        sb.append("<BR>");
        return sb.toString();
    }

}
