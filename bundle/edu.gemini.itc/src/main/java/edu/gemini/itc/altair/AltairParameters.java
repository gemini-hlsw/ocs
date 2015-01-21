// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: AltairParameters.java,v 1.1 2004/01/12 16:22:25 bwalls Exp $
//
package edu.gemini.itc.altair;

import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.itc.shared.NoSuchParameterException;

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
    public static final String WFSCOMB = "wfscomb";

    private static final String FIELD_LENS_IN = "IN";
    private static final String FIELD_LENS_OUT = "OUT";
    public static final String AOWFS = "aowfs";


    // Data members
    private boolean _altairUsed = true; // true or false
    private String _wfsMode;
    private double _guideStarSeperation;
    private double _guideStarMagnitude;
    private String _fieldLens;
    private String _wfs;

    /**
     * Constructs a PlottingDetailsParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public AltairParameters(HttpServletRequest r) throws Exception {
        parseServletRequest(r);
    }

    /**
     * Constructs a AltairParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public AltairParameters(ITCMultiPartParser p) throws Exception {
        parseMultipartParameters(p);
    }

    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) throws Exception {
        // Parse the Plotting details section of the form.


        String guideStarSeperation = r.getParameter(GUIDE_SEPERATION);
        if (guideStarSeperation == null) {
            _altairUsed = false;
        }
        _guideStarSeperation = ITCParameters.parseDouble(guideStarSeperation, "Seperation of Guide Star");
        if (_guideStarSeperation < 0 || _guideStarSeperation > 22)
            throw new Exception(" Altair Guide star distance must be between 0 and 22");

        String guideStarMagnitude = r.getParameter(GUIDE_MAG);
        if (guideStarMagnitude == null) {
            _altairUsed = false;
        }
        _guideStarMagnitude = ITCParameters.parseDouble(guideStarMagnitude, "Guide Star Magnitude");
        if (_guideStarMagnitude > 17 || _guideStarMagnitude < 7)
            throw new Exception(" Altair Guide star Magnitude must be between 7 and 17 ");

    }

    public void parseMultipartParameters(ITCMultiPartParser p) throws Exception {
        // Parse Altair specific section of the form.

        try {
            _guideStarSeperation = ITCParameters.parseDouble(p.getParameter(GUIDE_SEPERATION), "Seperation fo Guide Star");
            _guideStarMagnitude = ITCParameters.parseDouble(p.getParameter(GUIDE_MAG), "Guide Star Magnitude");
            _fieldLens = p.getParameter(FIELD_LENS);
            _wfs = p.getParameter(WFSCOMB);
            _wfsMode = p.getParameter(WFS_MODE);
        } catch (NoSuchParameterException e) {
            _altairUsed = false;
            _guideStarSeperation = 1;
            _guideStarMagnitude = 8;
        }
//        try {
//            _wfsMode = p.getParameter(WFS_MODE);
//            if (_wfsMode.equals(LGS)){
//                _altairUsed=true;
//                //_guideStarSeperation=0;  //old hardcoding of LGS info
//                //_guideStarMagnitude=11;
//            }
//       } catch (NoSuchParameterException e) {
//            //assume NGS mode
//            System.out.println("No such param");
//            _wfsMode=NGS;
//       }

        if (_wfs.equals(AOWFS))
            _altairUsed = true;
        else
            _altairUsed = false;


        if (_guideStarSeperation < 0 || _guideStarSeperation > 25)
            throw new Exception(" Altair Guide star distance must be between 0 and 25 arcsecs.");

        if (_wfsMode.equals(LGS) && _guideStarMagnitude > 19.5)
            throw new Exception(" Altair Guide star Magnitude must be <= 19.5 in R for LGS mode. ");

        if (_wfsMode.equals(NGS) && _guideStarMagnitude > 15.5)
            throw new Exception(" Altair Guide star Magnitude must be <= 15.5 in R for NGS mode. ");


        if (_wfsMode.equals(LGS) && _fieldLens.equals(FIELD_LENS_OUT))
            throw new Exception("The field Lens must be IN when Altair is in LGS mode.");
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
