package edu.gemini.itc.nici;

import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;

import javax.servlet.http.HttpServletRequest;


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
    public static final String DICHROIC_POSITION = "dichroicPosition";

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
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public NiciParameters(HttpServletRequest r) {
        parseServletRequest(r);
    }

    /**
     * Constructs a AcquisitionCamParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public NiciParameters(ITCMultiPartParser p) {
        parseMultipartParameters(p);
    }

    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) {
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

    /**
     * Parse Parameters from a multipart servlet request
     */
    public void parseMultipartParameters(ITCMultiPartParser p) {
        _channel1Filter = p.getParameter(CHANNEL1_FILTER);
        _channel2Filter = p.getParameter(CHANNEL2_FILTER);
        _instrumentMode = p.getParameter(INSTRUMENT_MODE);
        _pupilMask = p.getParameter(PUPIL_MASK);
        _dichroicPosition = p.getParameter(DICHROIC_POSITION);
    }

    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public NiciParameters(String channel1Filter,
                          String channel2Filter,
                          String pupilMask,
                          String instrumentMode,
                          String dichroicPosition
    ) {
        _channel1Filter = channel1Filter;
        _channel2Filter = channel2Filter;
        _pupilMask = pupilMask;
        _instrumentMode = instrumentMode;
        _dichroicPosition = dichroicPosition;
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

    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Channel 1 Filter:\t" + getChannel1Filter() + "\n");
        sb.append("Channel 2 Filter:\t" + getChannel2Filter() + "\n");
        sb.append("\n");
        return sb.toString();
    }
}
