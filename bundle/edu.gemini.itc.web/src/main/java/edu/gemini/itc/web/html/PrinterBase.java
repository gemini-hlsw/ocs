package edu.gemini.itc.web.html;

import edu.gemini.itc.base.*;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.servlets.ImageServlet;
import edu.gemini.itc.web.servlets.ServerInfo;

import java.io.PrintWriter;
import java.util.UUID;

public abstract class PrinterBase {

    private final PrintWriter _out;

    protected PrinterBase(final PrintWriter pr) {
        _out = pr;
    }

    public abstract void writeOutput();

    protected UUID cache(final ItcSpectroscopyResult result) {
        return ImageServlet.cache(result);
    }

    /* TODO: this needs to be validated for spectroscopy for all instruments, find a better place to do this */
    protected void validatePlottingDetails(final PlottingDetails pdp, final Instrument instrument) {
        if (pdp != null && pdp.getPlotLimits().equals(PlottingDetails.PlotLimits.USER)) {
            if (pdp.getPlotWaveL() * 1000 > instrument.getObservingEnd() || pdp.getPlotWaveU() * 1000 < instrument.getObservingStart()) {
                throw new IllegalArgumentException("User limits for plotting do not overlap with filter.");
            }
        }
    }

    protected void _print(String s) {
        if (_out == null) {
            s = s.replaceAll("<br>", "\n");
            s = s.replaceAll("<BR>", "\n");
            s = s.replaceAll("<LI>", "-");
            System.out.print(s);
        } else {
            s = s.replaceAll("\n", "<br>");
            _out.print(s.replaceAll("\n", "<br>") + "<br>");
        }
    }

    protected void _println(final String s) {
        _print(s);
        if (_out == null)
            System.out.println();
        else
            _out.println();
    }

    // Display an error text
    protected void _error(final String s) {
        _println("<span style=\"color:red; font-style:italic;\">" + s + "</span>");
    }

    protected void _printFileLink(final UUID id, final SpcDataType type) {
        _printFileLink(id, type, 0);
    }

    protected void _printFileLink(final UUID id, final SpcDataType type, final int index) {
        _println("<a href =" +
                "\"" + ServerInfo.getServerURL() +
                "itc/servlet/images" +
                "?" + ImageServlet.ParamType  + "=" + ImageServlet.TypeTxt +
                "&" + ImageServlet.ParamName  + "=" + type.toString() +
                "&" + ImageServlet.ParamIndex + "=" + index +
                "&" + ImageServlet.ParamId    + "=" + id +
                "\"> Click here for " + toFileLabel(type) + ". </a>");
    }

    // Adds an HTML image link
    protected void _printImageLink(final UUID id, final SpcChartType type, final PlottingDetails pd) {
        _printImageLink(id, type, 0, pd);
    }

    protected void _printImageLink(final UUID id, final SpcChartType type, final int index, final PlottingDetails pd) {
        _print("<img alt=\"" + toImgAlt(type) + "\" src=\"" + ServerInfo.getServerURL() +
                "itc/servlet/images" +
                "?" + ImageServlet.ParamType  + "=" + ImageServlet.TypeImg +
                "&" + ImageServlet.ParamName  + "=" + type.toString() +
                "&" + ImageServlet.ParamIndex + "=" + index +
                "&" + ImageServlet.ParamId    + "=" + id +
                toPlotLimits(pd) + "\"/>");
    }

    private String toPlotLimits(final PlottingDetails pd) {
        if (pd.getPlotLimits() == PlottingDetails.PlotLimits.AUTO) {
            return "";
        } else {
            return "&" + ImageServlet.ParamLoLimit + "=" + pd.getPlotWaveL() +
                   "&" + ImageServlet.ParamHiLimit + "=" + pd.getPlotWaveU();
        }
    }

    private String toImgAlt(final SpcChartType type) {
        if      (type == SignalChart.instance())    return "Signal/Background Chart";
        else if (type == S2NChart.instance())       return "Signal to Noise Chart";
        else    throw new Error();
    }

    private String toFileLabel(final SpcDataType type) {
        if      (type == SignalData.instance())     return "ASCII signal spectrum";
        else if (type == BackgroundData.instance()) return "ASCII background spectrum";
        else if (type == SingleS2NData.instance())  return "Single Exposure S/N ASCII data";
        else if (type == FinalS2NData.instance())   return "Final S/N ASCII data";
        else    throw new Error();
    }

}
