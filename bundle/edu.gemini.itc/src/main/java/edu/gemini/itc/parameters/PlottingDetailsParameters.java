// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: PlottingDetailsParameters.java,v 1.3 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.parameters;

import javax.servlet.http.HttpServletRequest;

import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.NoSuchParameterException;
import edu.gemini.itc.shared.FormatStringWriter;


/**
 * This class holds the information from the Plotting Details section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class PlottingDetailsParameters extends ITCParameters {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.

    public static final String PLOT_LIMITS = "plotLimits";
    public static final String PLOT_WAVE_L = "plotWavelengthL";
    public static final String PLOT_WAVE_U = "plotWavelengthU";

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String AUTO_LIMITS = "autoLimits";
    public static final String USER_LIMITS = "userLimits";

    // Data members
    private String _plotLimits; // auto or user
    private double _plotWaveL;
    private double _plotWaveU;

    /**
     * Constructs a PlottingDetailsParameters from a servlet request
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public PlottingDetailsParameters(HttpServletRequest r) throws Exception {
        parseServletRequest(r);
    }

    /**
     *Constructs a PlottingDetailsParameters from a MultipartParser
     * @param p MutipartParser that has all of the parameters and files Parsed
     *@throws Exception of cannot parse any of the parameters.
     */

    public PlottingDetailsParameters(ITCMultiPartParser p) throws Exception {
        parseMultipartParameters(p);
    }

    /** Parse parameters from a servlet request. */
    public void parseServletRequest(HttpServletRequest r) throws Exception {
        // Parse the Plotting details section of the form.

        // new plot code
        _plotLimits = r.getParameter(PLOT_LIMITS);
        if (_plotLimits == null) {
            ITCParameters.notFoundException(PLOT_LIMITS);
        }
        if (_plotLimits.equals(USER_LIMITS)) {
            String plotWaveL = r.getParameter(PLOT_WAVE_L);
            if (plotWaveL == null) {
                ITCParameters.notFoundException(PLOT_WAVE_L);
            }
            _plotWaveL = ITCParameters.parseDouble(plotWaveL, "Lower Bound of Plotting");
            if (_plotWaveL < 0) _plotWaveL *= -1;

            String plotWaveU = r.getParameter(PLOT_WAVE_U);
            if (plotWaveU == null) {
                ITCParameters.notFoundException(PLOT_WAVE_U);
            }
            _plotWaveU = ITCParameters.parseDouble(plotWaveU, "Upper Bound of Plotting");
            if (_plotWaveU < 0) _plotWaveU *= -1;
            if (_plotWaveU <= _plotWaveL) throw new Exception(" The Upper bound for the plotted spectra must be greater than the Lower bound. ");

        }
    }

    public void parseMultipartParameters(ITCMultiPartParser p) throws Exception {
        // Parse Plotting details section of the form.
        try {
            _plotLimits = p.getParameter(PLOT_LIMITS);
            _plotWaveL = ITCParameters.parseDouble(p.getParameter(PLOT_WAVE_L), "Lower Bound of Plotting");
            if (_plotWaveL < 0) _plotWaveL *= -1;
            _plotWaveU = ITCParameters.parseDouble(p.getParameter(PLOT_WAVE_U), "Upper Bound of Plotting");
            if (_plotWaveU < 0) _plotWaveU *= -1;
        } catch (NoSuchParameterException e) {
            throw new Exception("The parameter " + e.parameterName + " could not be found in the Plotting" +
                                " Parameters Section of the form.  Either add this value or Contact the Helpdesk.");
        }

        if (_plotWaveU <= _plotWaveL) throw new Exception("The Upper bound for the plotted spectra must be greater than the Lower bound. ");
    }

    /**
     * Constructs a ObservationDetailsParameters from a servlet request
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public PlottingDetailsParameters(
            String plotLimits,
            double plotWaveU,
            double plotWaveL) {
        _plotLimits = plotLimits;
        _plotWaveU = plotWaveU;
        _plotWaveL = plotWaveL;

    }


    public String getPlotLimits() {
        return _plotLimits;
    }

    public double getPlotWaveU() {
        return _plotWaveU * 1000;
    }  //convert microns to nm

    public double getPlotWaveL() {
        return _plotWaveL * 1000;
    }   //convert microns to nm

    /** Return a human-readable string for debugging */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("PlotMethod:\t" + getPlotLimits() + "\n");
        sb.append("PlotLowerLimit:\t" + getPlotWaveL() + "\n");
        sb.append("PlotUpperLimit:\t" + getPlotWaveU() + "\n");
        sb.append("\n");
        return sb.toString();
    }

    public String printParameterSummary() {
        StringBuffer sb = new StringBuffer();

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();

        sb.append("Output:\n<LI>Spectra ");
        if (getPlotLimits().equals(AUTO_LIMITS)) {
            sb.append("autoscaled.");
        } else {
            sb.append("plotted over range " + _plotWaveL + " - " + _plotWaveU);
        }
        sb.append("\n");
        return sb.toString();
    }

}
