package edu.gemini.itc.parameters;

import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.ITCParameters;

/**
 * This class holds the information from the Plotting Details section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class PlottingDetailsParameters extends ITCParameters {

    public static enum PlotLimits {
        AUTO,
        USER,
        ;
    }

    private final PlotLimits plotLimits; // auto or user
    private final double plotWaveL;
    private final double plotWaveU;

    /**
     * Constructs a ObservationDetailsParameters from a servlet request
     * @throws Exception if input data is not parsable.
     */
    public PlottingDetailsParameters(final PlotLimits plotLimits, final double plotWaveL, final double plotWaveU) {
        this.plotLimits = plotLimits;
        this.plotWaveU = plotWaveU;
        this.plotWaveL = plotWaveL;

        if (this.plotWaveU <= this.plotWaveL) {
            throw new IllegalArgumentException("The Upper bound for the plotted spectra must be greater than the Lower bound. ");
        }
    }


    public PlotLimits getPlotLimits() {
        return plotLimits;
    }

    public double getPlotWaveU() {
        return plotWaveU * 1000;
    }  //convert microns to nm

    public double getPlotWaveL() {
        return plotWaveL * 1000;
    }   //convert microns to nm

    /**
     * Return a human-readable string for debugging
     */
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
        switch (plotLimits) {
            case AUTO: sb.append("autoscaled."); break;
            default:   sb.append("plotted over range " + plotWaveL + " - " + plotWaveU);
        }
        sb.append("\n");
        return sb.toString();
    }

}
