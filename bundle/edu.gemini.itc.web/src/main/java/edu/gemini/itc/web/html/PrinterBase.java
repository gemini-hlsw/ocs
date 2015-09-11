package edu.gemini.itc.web.html;

import edu.gemini.itc.base.*;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.servlets.FilesServlet;
import edu.gemini.itc.web.servlets.ServerInfo;

import java.io.PrintWriter;
import java.util.Optional;
import java.util.UUID;

public abstract class PrinterBase {

    private final PrintWriter _out;

    protected PrinterBase(final PrintWriter pr) {
        _out = pr;
    }

    public abstract void writeOutput();

    protected UUID cache(final ItcSpectroscopyResult result) {
        return FilesServlet.cache(result);
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

    /** Adds a file link with a concatenation of all series of the given data type of chart 0. */
    protected void _printFileLinkAllSeries(final UUID id, final SpcDataType type) {
        _printFileLink(id, type, 0, Optional.empty(), Optional.empty());
    }

    /** Adds a file link with series 0 of the given data type of chart 0. */
    protected void _printFileLink(final UUID id, final SpcDataType type) {
        _printFileLink(id, type, 0, Optional.of(0), Optional.empty());
    }

    /** Adds a file link with the series 0 of the given type and chart. */
    protected void _printFileLink(final UUID id, final SpcDataType type, final int chartIndex) {
        _printFileLink(id, type, chartIndex, Optional.of(0), Optional.empty());
    }

    /** Adds a file link with the series with the given index for the given type and chart and with an additional text in the label. */
    protected void _printFileLink(final UUID id, final SpcDataType type, final int chartIndex, final int seriesIndex, final String txt) {
        _printFileLink(id, type, chartIndex, Optional.of(seriesIndex), Optional.of(txt));
    }

    /** Adds a link to a file representing one (or all) series of data (x,y value pairs) of a chart.
     *  The file will be created on-demand by the FilesServlet. If no series index is given, all series of the given
     *  type are stitched together and represented in a single file. This is used for displaying the values covering
     *  several CCDs which are represented by different colors in the charts and therefore by different data series;
     *  currently this is only used for GMOS' HAMAMATSU CCDs but might be interesting for GHOST in the future.
     * @param id            the UUID representing the cached calculation results
     * @param type          the type of data (e.g. signal, background etc)
     * @param chartIndex    the index of the chart (e.g. signal or s2n)
     * @param seriesIndex   the index of the chart series
     * @param txt           an optional text that will be added to the HTML link description if present
     */
    protected void _printFileLink(final UUID id, final SpcDataType type, final int chartIndex, final Optional<Integer> seriesIndex, final Optional<String> txt) {
        _println("<a href =" +
                "\"" + ServerInfo.getServerURL() +
                "itc/servlet/images" +
                "?" + FilesServlet.ParamType        + "=" + FilesServlet.TypeTxt +
                "&" + FilesServlet.ParamName        + "=" + type.toString() +
                "&" + FilesServlet.ParamChartIndex  + "=" + chartIndex +
                seriesIndex.map(i -> "&" + FilesServlet.ParamSeriesIndex + "=" + i).orElse("") +
                "&" + FilesServlet.ParamId          + "=" + id +
                "\"> Click here for " + toFileLabel(type) +
                txt.orElse("") +
                ".</a>");
    }

    // Adds a link to an image
    protected void _printImageLink(final UUID id, final SpcChartType type, final PlottingDetails pd) {
        _printImageLink(id, type, 0, pd);
    }

    protected void _printImageLink(final UUID id, final SpcChartType type, final int index, final PlottingDetails pd) {
        _print("<img alt=\"" + toImgAlt(type) + "\" src=\"" + ServerInfo.getServerURL() +
                "itc/servlet/images" +
                "?" + FilesServlet.ParamType        + "=" + FilesServlet.TypeImg +
                "&" + FilesServlet.ParamName        + "=" + type.toString() +
                "&" + FilesServlet.ParamChartIndex  + "=" + index +
                "&" + FilesServlet.ParamId          + "=" + id +
                toPlotLimits(pd) + "\"/>");
    }

    private String toPlotLimits(final PlottingDetails pd) {
        if (pd.getPlotLimits() == PlottingDetails.PlotLimits.AUTO) {
            return "";
        } else {
            return "&" + FilesServlet.ParamLoLimit + "=" + pd.getPlotWaveL() +
                   "&" + FilesServlet.ParamHiLimit + "=" + pd.getPlotWaveU();
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
