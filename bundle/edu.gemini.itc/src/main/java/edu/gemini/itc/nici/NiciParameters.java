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
// Most of this class will need to be modified to accept NICI filters/parameters.
// Any selectable NICI option from the webform will need to be here (read noise,
// filters, etc)
//
package edu.gemini.itc.nici;

import javax.servlet.http.HttpServletRequest;
import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.NoSuchParameterException;


/**
 * This class holds the information from the Nici section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class NiciParameters extends ITCParameters {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String CHANNEL1_FILTER = "channel1Filter";
    public static final String CHANNEL2_FILTER = "channel2Filter";
    public static final String PUPIL_MASK = "pupilMask";
    public static final String DICHROIC_POSITION="dichroicPosition";
    
    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String CLEAR = "clear";
    public static final String NDA = "NDa";
    public static final String NDB = "NDb";
    public static final String NDC = "NDc";
    public static final String NDD = "NDd";

    // ITC web form value
    // Determines which mode NICI is set for: single/dual channel imaging
    // or coronagraphic observations
    public static final String INSTRUMENT_MODE = "calcMode";
    
    // Data members
    private String _channel1Filter;  //
    private String _channel2Filter;  //
    private String _pupilMask;
    private String _instrumentMode;
    private String _dichroicPosition;
    
    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public NiciParameters(HttpServletRequest r) throws Exception {
        parseServletRequest(r);
    }
    
    /**
     *Constructs a AcquisitionCamParameters from a MultipartParser
     * @param p MutipartParser that has all of the parameters and files Parsed
     *@throws Exception of cannot parse any of the parameters.
     */
    
    public NiciParameters(ITCMultiPartParser p) throws Exception {
        parseMultipartParameters(p);
    }
    
    /** Parse parameters from a servlet request. */
    public void parseServletRequest(HttpServletRequest r) throws Exception {
        // Parse the acquisition camera section of the form.
        
        // Get channel 1 filter
        _channel1Filter = r.getParameter(CHANNEL1_FILTER);
        if (_channel1Filter == null) {
            ITCParameters.notFoundException(CHANNEL1_FILTER);
        }
        
        // Get channel 2 filter
        _channel2Filter = r.getParameter(CHANNEL2_FILTER);
        if (_channel2Filter == null) {
            ITCParameters.notFoundException(CHANNEL2_FILTER);
        }

        // Get instrument mode
        _instrumentMode = r.getParameter(INSTRUMENT_MODE);
        if (_instrumentMode == null) {
            ITCParameters.notFoundException(INSTRUMENT_MODE);
	}

        // Get pupil mask
        _pupilMask = r.getParameter(PUPIL_MASK);
        if (_instrumentMode == null) {
            ITCParameters.notFoundException(PUPIL_MASK); 
	}       

	// Get dichroic position
        _dichroicPosition = r.getParameter(DICHROIC_POSITION);
        if (_instrumentMode == null) {
            ITCParameters.notFoundException(DICHROIC_POSITION);
	}
    }
    /** Parse Parameters from a multipart servlet request */
    public void parseMultipartParameters(ITCMultiPartParser p) throws Exception {
        // Parse NICI details section of the form.
        try {
            _channel1Filter = p.getParameter(CHANNEL1_FILTER);
            _channel2Filter = p.getParameter(CHANNEL2_FILTER);
            _instrumentMode = p.getParameter(INSTRUMENT_MODE);
	    _pupilMask = p.getParameter(PUPIL_MASK);
	    _dichroicPosition = p.getParameter(DICHROIC_POSITION);
        } catch (NoSuchParameterException e) {
            throw new Exception("The parameter " + e.parameterName + " could not be found in the Telescope" +
                    " Paramters Section of the form.  Either add this value or Contact the Helpdesk.");
        }
    }
    
    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public NiciParameters(String channel1Filter,
            String channel2Filter) {
        _channel1Filter = channel1Filter;
        _channel2Filter = channel2Filter;
    }
    
    public String getChannel1Filter() {
        return _channel1Filter;
    }
    
    public String getChannel2Filter() {
        return _channel2Filter;
    }

    public String getInstrumentMode() {
        return _instrumentMode;
    }

    public String getPupilMask() {
        return _pupilMask;
    }
    
    public String getDichroicPosition() {
	return _dichroicPosition;
    }


    public boolean isDualChannel() {
        System.out.println("MODE: "+_instrumentMode);
        if (_instrumentMode.equals("imaging2c")) {
            System.out.println("TRUE");
            return true;
	}
	else {
            System.out.println("FALSE");
            return false;
	}
    }
    
    /** Return a human-readable string for debugging */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Channel 1 Filter:\t" + getChannel1Filter() + "\n");
        sb.append("Channel 2 Filter:\t" + getChannel2Filter() + "\n");
        sb.append("\n");
        return sb.toString();
    }
}
