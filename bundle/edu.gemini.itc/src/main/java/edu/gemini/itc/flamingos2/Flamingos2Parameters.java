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
package edu.gemini.itc.flamingos2;

import javax.servlet.http.HttpServletRequest;
import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.NoSuchParameterException;


/**
 * This class holds the information from the Acquisition Camera section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class Flamingos2Parameters extends ITCParameters {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String INSTRUMENT_FILTER = "instrumentFilter";
    public static final String INSTRUMENT_GRISM = "instrumentDisperser";
    public static final String READ_NOISE = "readNoise";
    public static final String FP_MASK = "instrumentFPMask";

    
    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String CLEAR = "Open";
    
    // Grism names
    public static final String JHGRISM = "JH";
    public static final String HKGRISM = "HK";
    public static final String R3KGRISM = "R3K";
    public static final String NOGRISM = "None";
    
    // Data members
    private String _colorFilter;  // U, V, B, ...
	private String _grism;
	private String _readNoise;
	private String _fpMask;
    
    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public Flamingos2Parameters(HttpServletRequest r) throws Exception {
        parseServletRequest(r);
    }
    
    /**
     *Constructs a AcquisitionCamParameters from a MultipartParser
     * @param p MutipartParser that has all of the parameters and files Parsed
     *@throws Exception of cannot parse any of the parameters.
     */
    
    public Flamingos2Parameters(ITCMultiPartParser p) throws Exception {
        parseMultipartParameters(p);
    }
    
    /** Parse parameters from a servlet request. */
    public void parseServletRequest(HttpServletRequest r) throws Exception {
        // Parse the acquisition camera section of the form.
        
        // Get color filter
        _colorFilter = r.getParameter(INSTRUMENT_FILTER);
        if (_colorFilter == null) {
            ITCParameters.notFoundException(INSTRUMENT_FILTER);
        }
        
        // Get Grism
        _grism = r.getParameter(INSTRUMENT_GRISM);
        if (_grism == null) {
            ITCParameters.notFoundException(INSTRUMENT_GRISM);
        }

        if (_colorFilter.equalsIgnoreCase("none") && _grism.equalsIgnoreCase("none")) {
            throw new Exception("Must specify a filter or a grism");
        }

        //Get High or low read noise
        _readNoise = r.getParameter(READ_NOISE);
        if (_readNoise == null) {
            ITCParameters.notFoundException(READ_NOISE);
        }

        _fpMask = r.getParameter(FP_MASK);
        if (_fpMask == null) {
            ITCParameters.notFoundException(FP_MASK);
        }

        // Flamingos 2 has no ND filter
        /*
        _ndFilter = r.getParameter(INSTRUMENT_ND_FILTER);
        if (_ndFilter == null) {
            ITCParameters.notFoundException(INSTRUMENT_ND_FILTER);
        }
        */
    }
    /** Parse Parameters from a multipart servlet request */
    public void parseMultipartParameters(ITCMultiPartParser p) throws Exception {
        // Parse Acquisition Cam details section of the form.
        try {
            _colorFilter = p.getParameter(INSTRUMENT_FILTER);            
            //_ndFilter = p.getParameter(INSTRUMENT_ND_FILTER);

            _grism = p.getParameter(INSTRUMENT_GRISM);            
            //Get High or low read noise
            _readNoise = p.getParameter(READ_NOISE);
            _fpMask = p.getParameter(FP_MASK);
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
    public Flamingos2Parameters(String colorFilter, String grismName, String fpMask, String readNoise) {
        _colorFilter = colorFilter;
        _grism = grismName;
        _fpMask = fpMask;
        _readNoise = readNoise;
    }
    
    public String getColorFilter() {
        return _colorFilter;
    }  
       
    /** Return a human-readable string for debugging */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Color Filter:\t" + getColorFilter() + "\n");
        sb.append("\n");
        return sb.toString();
    }
    
    public String getFPMask () {
    	return _fpMask;
    }
    
    public double getSlitSize () {
    	if (_fpMask.equalsIgnoreCase("none")) {
    		return 1;
    	}
    	return Double.parseDouble(_fpMask);
    }

	public String getReadNoise() {
		return _readNoise;
	}

	public String getGrism() {
		return _grism;
	}
}
