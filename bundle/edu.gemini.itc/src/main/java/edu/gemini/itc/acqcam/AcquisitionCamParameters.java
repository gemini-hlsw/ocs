// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: AcquisitionCamParameters.java,v 1.4 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.acqcam;

import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.itc.shared.NoSuchParameterException;

import javax.servlet.http.HttpServletRequest;


/**
 * This class holds the information from the Acquisition Camera section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class AcquisitionCamParameters extends ITCParameters {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String INSTRUMENT_FILTER = "instrumentFilter";
    public static final String INSTRUMENT_ND_FILTER = "instrumentNDFilter";

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String CLEAR = "clear";
    public static final String NDA = "NDa";
    public static final String NDB = "NDb";
    public static final String NDC = "NDc";
    public static final String NDD = "NDd";

    // Data members
    private String _colorFilter;  // U, V, B, ...
    private String _ndFilter;  // NDa, NDb, ...  or null for clear

    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public AcquisitionCamParameters(HttpServletRequest r) throws Exception {
        parseServletRequest(r);
    }

    /**
     * Constructs a AcquisitionCamParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public AcquisitionCamParameters(ITCMultiPartParser p) throws Exception {
        parseMultipartParameters(p);
    }

    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) throws Exception {
        // Parse the acquisition camera section of the form.

        // Get color filter
        _colorFilter = r.getParameter(INSTRUMENT_FILTER);
        if (_colorFilter == null) {
            ITCParameters.notFoundException(INSTRUMENT_FILTER);
        }

        // Get ND filter
        _ndFilter = r.getParameter(INSTRUMENT_ND_FILTER);
        if (_ndFilter == null) {
            ITCParameters.notFoundException(INSTRUMENT_ND_FILTER);
        }
    }

    /**
     * Parse Parameters from a multipart servlet request
     */
    public void parseMultipartParameters(ITCMultiPartParser p) throws Exception {
        // Parse Acquisition Cam details section of the form.
        try {
            _colorFilter = p.getParameter(INSTRUMENT_FILTER);
            _ndFilter = p.getParameter(INSTRUMENT_ND_FILTER);
        } catch (NoSuchParameterException e) {
            throw new Exception("The parameter " + e.parameterName + " could not be found in the Telescope" +
                    " Paramters Section of the form.  Either add this value or Contact the Helpdesk.");
        }
    }

    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public AcquisitionCamParameters(String colorFilter,
                                    String ndFilter) {
        _colorFilter = colorFilter;
        _ndFilter = ndFilter;
    }

    public String getColorFilter() {
        return _colorFilter;
    }

    public String getNDFilter() {
        return _ndFilter;
    }

    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Color Filter:\t" + getColorFilter() + "\n");
        sb.append("ND Filter:\t" + getNDFilter() + "\n");
        sb.append("\n");
        return sb.toString();
    }
}
