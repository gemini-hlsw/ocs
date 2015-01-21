// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: GsaoiParameters.java,v 1.7 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.gsaoi;

import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.itc.shared.NoSuchParameterException;

import javax.servlet.http.HttpServletRequest;

/**
 * This class holds the information from the Gsaoi section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class GsaoiParameters extends ITCParameters {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String INSTRUMENT_FILTER = "instrumentFilter";
    public static final String INSTRUMENT_CAMERA = "instrumentCamera";
    public static final String READ_MODE = "readMode";

    public static final String BRIGHT_OBJECTS_READ_MODE = "bright";
    public static final String FAINT_OBJECTS_READ_MODE = "faint";
    public static final String VERY_FAINT_OBJECTS_READ_MODE = "veryFaint";

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final int J = 0;
    public static final int H = 1;
    public static final int K = 2;
    public static final int L = 3;
    public static final int M = 4;

    // Data members
    private String _filter;  // filters
    private String _camera;
    private String _readMode;

    /**
     * Constructs a GsaoiParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public GsaoiParameters(HttpServletRequest r) throws Exception {
        parseServletRequest(r);
    }

    /**
     * Constructs a GsaoiParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */
    public GsaoiParameters(ITCMultiPartParser p) throws Exception {
        parseMultipartParameters(p);
    }

    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) throws Exception {

        // Get Broad Band filter
        _filter = r.getParameter(INSTRUMENT_FILTER);
        if (_filter == null) {
            ITCParameters.notFoundException(INSTRUMENT_FILTER);
        }

        if (_filter.equals("none")) {
            throw new Exception("Must specify a filter");
        }

        // Get Camera Used
        _camera = r.getParameter(INSTRUMENT_CAMERA);
        if (_camera == null) {
            ITCParameters.notFoundException(INSTRUMENT_CAMERA);
        }

        //Get read mode
        _readMode = r.getParameter(READ_MODE);
        if (_readMode == null) {
            ITCParameters.notFoundException(READ_MODE);
        }
    }

    public void parseMultipartParameters(ITCMultiPartParser p) throws Exception {
        try {
            _filter = p.getParameter(INSTRUMENT_FILTER);
            if (_filter.equals("none")) {
                throw new Exception("Must specify a filter or a grism");
            }
            _camera = p.getParameter(INSTRUMENT_CAMERA);
            _readMode = p.getParameter(READ_MODE);

        } catch (NoSuchParameterException e) {
            throw new Exception("The parameter " + e.parameterName + " could not be found in the Telescope" +
                    " Paramters Section of the form.  Either add this value or Contact the Helpdesk.");
        }
    }

    public GsaoiParameters(String filter,
                           String camera,
                           String readMode) {
        _filter = filter;
        _camera = camera;
        _readMode = readMode;
    }

    public String getFilter() {
        return _filter;
    }

    public String getCamera() {
        return _camera;
    }

    public String getReadMode() {
        return _readMode;
    }


    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Filter:\t" + getFilter() + "\n");
//        sb.append("Camera:\t" + getCamera() + "\n");
        sb.append("\n");
        return sb.toString();
    }
}
