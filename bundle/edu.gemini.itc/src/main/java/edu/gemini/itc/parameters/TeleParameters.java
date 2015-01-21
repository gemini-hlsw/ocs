// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: TeleParameters.java,v 1.5 2004/01/12 16:31:43 bwalls Exp $
//
package edu.gemini.itc.parameters;

import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.itc.shared.NoSuchParameterException;

import javax.servlet.http.HttpServletRequest;

/**
 * This class holds the information from the Telescope section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class TeleParameters extends ITCParameters {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String MIRROR_COATING = "mirrorCoating";
    public static final String ISS_PORT = "issPort";
    public static final String WFSCOMB = "wfscomb";

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String ALUMINIUM = "aluminium";
    public static final String SILVER = "silver";
    public static final String UP = "up";
    public static final String SIDE = "side";
    public static final String OIWFS = "oiwfs";
    public static final String PWFS = "pwfs";
    public static final String AOWFS = "aowfs";

    private static final double _telescopeDiameter = 3.95 + 3.95;

    // Data members
    private String _mirrorCoating;  // aluminum or silver
    private String _instrumentPort; // up or side
    private String _wfs;

    /**
     * Constructs a TeleParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public TeleParameters(HttpServletRequest r) throws Exception {
        parseServletRequest(r);
    }

    /**
     * Constructs a TeleParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public TeleParameters(ITCMultiPartParser p) throws Exception {
        parseMultipartParameters(p);
    }


    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) throws Exception {
        // Parse the Telescope specific section of the form.


        // Get mirror coating
        _mirrorCoating = r.getParameter(MIRROR_COATING);
        if (_mirrorCoating == null) {
            ITCParameters.notFoundException(MIRROR_COATING);
        }

        // Get iss port
        _instrumentPort = r.getParameter(ISS_PORT);
        if (_instrumentPort == null) {
            ITCParameters.notFoundException(ISS_PORT);
        }

        _wfs = r.getParameter(WFSCOMB);
        if (_wfs == null) {
            ITCParameters.notFoundException(WFSCOMB);
        }


    }

    public void parseMultipartParameters(ITCMultiPartParser p) throws Exception {
        // Parse Telescope details section of the form.
        try {
            _mirrorCoating = p.getParameter(MIRROR_COATING);
            _instrumentPort = p.getParameter(ISS_PORT);
            _wfs = p.getParameter(WFSCOMB);
        } catch (NoSuchParameterException e) {
            throw new Exception("The parameter " + e.parameterName + " could not be found in the Telescope" +
                    " Parameters Section of the form.  Either add this value or Contact the Helpdesk.");
        }
    }


    public TeleParameters(String mirrorCoating,
                          String instrumentPort,
                          String wfs) {
        _mirrorCoating = mirrorCoating;
        _instrumentPort = instrumentPort;
        _wfs = wfs;
    }

    public String getMirrorCoating() {
        return _mirrorCoating;
    }

    public String getInstrumentPort() {
        return _instrumentPort;
    }

    public String getWFS() {
        if (_wfs.equals(AOWFS))
            return OIWFS;  //AO/tiptilt will be handled by Altair return something the rest of the
            // code can understand
        else
            return _wfs;
    }

    public double getTelescopeDiameter() {
        return _telescopeDiameter;
    }

    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Mirror Coating:\t" + getMirrorCoating() + "\n");
        sb.append("ISS Port:\t" + getInstrumentPort() + "\n");
        sb.append("WFS:\t" + getWFS() + "\n");
        sb.append("\n");
        return sb.toString();
    }

    public String printParameterSummary() {
        return printParameterSummary(getWFS());
    }

    public String printParameterSummary(String wfs) {
        StringBuffer sb = new StringBuffer();
        sb.append("Telescope configuration: \n");
        sb.append("<LI>" + getMirrorCoating() + " mirror coating.\n");
        sb.append("<LI>" + getInstrumentPort() + " looking port.\n");
        sb.append("<LI>wavefront sensor: " + wfs + "\n");
        return sb.toString();
    }

}
