// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: GemsParameters.java,v 1.1 2004/01/12 16:22:25 bwalls Exp $
//
package edu.gemini.itc.gems;

import javax.servlet.http.HttpServletRequest;

import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.NoSuchParameterException;
import edu.gemini.itc.shared.FormatStringWriter;


/**
 * This class holds the information from the Gems section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class GemsParameters extends ITCParameters {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.

    public static final String AVG_STREHL = "avgStrehl";
    public static final String STREHL_BAND = "strehlBand";


    // Data members
    private double _avgStrehl;
    private String _strehlBand;

    /**
     * Constructs a PlottingDetailsParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public GemsParameters(HttpServletRequest r) throws Exception {
        parseServletRequest(r);
    }

    public GemsParameters(ITCMultiPartParser p) throws Exception {
        parseMultipartParameters(p);
    }

    public GemsParameters(double avgStreh, String strehlBand) {
        _avgStrehl = avgStreh;
        _strehlBand = strehlBand;
    }

    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) throws Exception {
    }

    public void parseMultipartParameters(ITCMultiPartParser p) throws Exception {
        // Parse Gems specific section of the form.

        try {
            _avgStrehl = ITCParameters.parseDouble(p.getParameter(AVG_STREHL), "Average Strehl");
            _avgStrehl = _avgStrehl / 100.; // Value is percent

            _strehlBand = p.getParameter(STREHL_BAND);
        } catch (NoSuchParameterException e) {
            e.printStackTrace();
        }
    }

    public boolean gemsIsUsed() {
        return true;
    }

    public double getAvgStrehl() {
        return _avgStrehl;
    }

    public String getStrehlBand() {
        return _strehlBand;
    }

    public String printParameterSummary() {
        StringBuffer sb = new StringBuffer();

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places

        sb.append("Average Strehl:\t" + getAvgStrehl() + "\n");
        sb.append("Strehl Band:\t" + getStrehlBand() + "\n");
        sb.append("\n");

        device.clear();

        return sb.toString();
    }

}
