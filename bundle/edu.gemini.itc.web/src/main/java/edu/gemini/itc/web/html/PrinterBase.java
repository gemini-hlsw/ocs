package edu.gemini.itc.web.html;

import edu.gemini.itc.base.AOSystem;
import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.Result;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.servlets.FilesServlet;
import edu.gemini.itc.web.servlets.ServerInfo;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.core.PointSource$;
import edu.gemini.spModel.core.UniformSource$;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.time.TimeAmountFormatter;
import scala.collection.JavaConversions;

import java.io.PrintWriter;
import java.util.*;

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
        _printFileLink(id, type, 0, new ArrayList(), Optional.empty());
    }

    /** Adds a file link with a concatenation of all series of the given data type and chart. */
    protected void _printFileLinkAllSeries(final UUID id, final SpcDataType type, final int chartIndex) {
        _printFileLink(id, type, chartIndex, new ArrayList(), Optional.empty());
    }

    /** Adds a file link with series 0 of the given data type of chart 0. */
    protected void _printFileLink(final UUID id, final SpcDataType type) {
        _printFileLink(id, type, 0, Arrays.asList(0), Optional.empty());
    }

    /** Adds a file link with the series 0 of the given type and chart. */
    protected void _printFileLink(final UUID id, final SpcDataType type, final int chartIndex) {
        _printFileLink(id, type, chartIndex, Arrays.asList(0), Optional.empty());
    }

    protected void _printFileLink(final UUID id, final SpcDataType type, final int chartIndex, final int seriesIndex, final String txt) {
        _printFileLink(id, type, chartIndex, Arrays.asList(seriesIndex), Optional.of(txt));
    }

    /** Adds a file link with the series with the given index for the given type and chart and with an additional text in the label. */
    protected void _printFileLink(final UUID id, final SpcDataType type, final int chartIndex, final List<Integer> seriesIndices, final String txt) {
        _printFileLink(id, type, chartIndex, seriesIndices, Optional.of(txt));
    }

    /** Adds a link to a file representing one (or all) series of data (x,y value pairs) of a chart.
     *  The file will be created on-demand by the FilesServlet. If no series index is given, all series of the given
     *  type are stitched together and represented in a single file. This is used for displaying the values covering
     *  several CCDs which are represented by different colors in the charts and therefore by different data series;
     *  currently this is only used for GMOS' HAMAMATSU CCDs but might be interesting for GHOST in the future.
     * @param id             the UUID representing the cached calculation results
     * @param type           the type of data (e.g. signal, background etc)
     * @param chartIndex     the index of the chart (e.g. signal or s2n)
     * @param seriesIndices  a list of indices for the chart series
     * @param txt            an optional text that will be added to the HTML link description if present
     */
    protected void _printFileLink(final UUID id, final SpcDataType type, final int chartIndex, final List<Integer> seriesIndices, final Optional<String> txt) {
        StringBuilder sb = new StringBuilder();

        sb.append(ServerInfo.getServerURL());
        sb.append("itc/servlet/images");
        sb.append("?" + FilesServlet.ParamType        + "=" + FilesServlet.TypeTxt);
        sb.append("&" + FilesServlet.ParamName        + "=" + type.toString());
        sb.append("&" + FilesServlet.ParamChartIndex  + "=" + chartIndex);
        for (int seriesIndex : seriesIndices) {
            sb.append("&" + FilesServlet.ParamSeriesIndex + "=" + seriesIndex);
        }
        sb.append("&" + FilesServlet.ParamId          + "=" + id);

        _println("<a href =\"" +
                sb.toString() +
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

    protected void _printPeakPixelInfo(final scala.Option<ItcCcd> ccd) {
        if (ccd.isDefined()) {
            _println(
                    String.format("The peak pixel signal + background is %.0f e- (%d ADU). This is %.0f%% of the full well depth of %.0f e-.",
                            ccd.get().peakPixelFlux(), ccd.get().adu(), ccd.get().percentFullWell(), ccd.get().wellDepth()));
        }
    }

    protected void _printWarnings(final scala.collection.immutable.Seq<ItcWarning> warnings) {
        for (final ItcWarning w : JavaConversions.seqAsJavaList(warnings)) {
            _print("Warning: " + w.msg());
        }
        _println("");
    }

    protected void _printSoftwareAperture(final SpectroscopyResult result, final double slitWidth) {
        if (result.observation().analysisMethod() instanceof UserAperture) {
            _println(String.format("software aperture extent along slit = %.2f arcsec", ((UserAperture) result.observation().analysisMethod()).diameter()));
        } else {
            if (result.source().profile() == UniformSource$.MODULE$) {
                _println(String.format("software aperture extent along slit = %.2f arcsec", slitWidth));
            } else if (result.source().profile() == PointSource$.MODULE$) {
                if (result.aoSystem().nonEmpty()) {
                     AOSystem ao = result.aoSystem().get();
                    _println(String.format("software aperture extent along slit = %.2f arcsec", 1.4 * ao.getAOCorrectedFWHM()));
                } else {
                    _println(String.format("software aperture extent along slit = %.2f arcsec", 1.4 * result.iqCalc().getImageQuality()));
                }
            }
        }

        if (!result.parameters().source().isUniform()) {
            _println(String.format("fraction of source flux in aperture = %.2f", result.slitThrougput()));
        }
    }

    protected void _printRequestedIntegrationTime(final SpectroscopyResult result) {
        _printRequestedIntegrationTime(result, 1);
    }

    // TODO: The correction factor here is only used for Michelle. This is an ancient hack around some special
    // TODO: behavior for Michelle's polarimetry mode which needs to be corrected.
    protected void _printRequestedIntegrationTime(final SpectroscopyResult result, final int correction) {
        if (result.observation().calculationMethod() instanceof S2NMethod) {
            final double numExposures = ((S2NMethod) result.observation().calculationMethod()).exposures();
            final double exposureTime = result.observation().calculationMethod().exposureTime() * correction * result.observation().calculationMethod().coaddsOrElse(1);
            _printRequestedIntegrationTime(result, exposureTime, numExposures);
        } else {
            throw new Error("Unsupported analysis method");
        }
    }

    protected void _printSkyAperture(final Result result) {
        final AnalysisMethod method = result.observation().analysisMethod();
        final double aperture;
        if      (method instanceof ApertureMethod) aperture = ((ApertureMethod) method).skyAperture();
        else if (method instanceof IfuMethod)      aperture = ((IfuMethod) method).skyFibres();
        else throw new Error();
        _println("Sky subtraction aperture = " + aperture + " times the software aperture.");
    }

    private void _printRequestedIntegrationTime(final SpectroscopyResult result, final double exposureTime, final double numExposures) {
        _println(String.format(
                "Requested total integration time = %.2f secs, of which %.2f secs is on source.",
                exposureTime * numExposures,
                exposureTime * numExposures * result.observation().sourceFraction()));
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
        if      (type == SignalChart.instance())        return "Signal/Background Chart";
        else if (type == S2NChart.instance())           return "Signal to Noise Chart";
        else if (type == SignalPixelChart.instance())   return "Signal/Background Pixel Chart";
        else    throw new Error();
    }

    private String toFileLabel(final SpcDataType type) {
        if      (type == SignalData.instance())         return "ASCII signal spectrum";
        else if (type == BackgroundData.instance())     return "ASCII background spectrum";
        else if (type == SingleS2NData.instance())      return "Single Exposure S/N ASCII data";
        else if (type == FinalS2NData.instance())       return "Final S/N ASCII data";
        else if (type == PixSigData.instance())         return "Pixel ASCII signal spectrum";
        else if (type == PixBackData.instance())        return "Pixel ASCII SQRT(background) spectrum";
        else    throw new Error();
    }

    public String _printOverheadTable(ItcParameters p, Config config, double readoutTimePerCoadd, PlannedTime pta, int step) {
        PlannedTime.Step s = pta.steps.get(step);
        Config conf = config;
        Map<PlannedTime.Category, PlannedTime.CategorizedTime> m = s.times.maxTimes();
        String setupStr = "";
        String reacqStr = "";
        String secStr;
        int numAcq = pta.numAcq();
        int numReacq = 0;
        long totalTime = pta.totalTimeWithMultipleAcq(conf);

        if (p.observation().calculationMethod() instanceof Spectroscopy) {
            numReacq = pta.numReacq(conf);
            totalTime = pta.totalTimeWithMultipleAcqReacq(conf);
        }

        StringBuilder buf = new StringBuilder("<html><body>");

        buf.append("<table><tr><th>Observation Overheads</th></tr>");
        buf.append("<tr>");
        buf.append("<td>").append("Setup ").append("</td>");

        if (numAcq == 1) {
            setupStr = String.format("%.1f s", pta.setup.time / 1000.0);
        } else if (numAcq > 1) {
            setupStr = String.format("%d visits x %.1f s", numAcq, pta.setup.time / 1000.0);
        }

        buf.append("<td align=\"right\"> ").append(setupStr).append("</td>");
        buf.append("</tr>");

        reacqStr = String.format("%d x %.1f s", numReacq, pta.setup.reacquisitionTime / 1000.0);

        if (numReacq > 0) {
            buf.append("<tr>");
            buf.append("<td>").append("Re-centering ").append("</td>");
            buf.append("<td align=\"right\"> ").append(reacqStr).append("</td>");
            buf.append("</tr>");
        }

        for (PlannedTime.Category c : PlannedTime.Category.values()) {
            PlannedTime.CategorizedTime ct = m.get(c);
            if (ct == null) continue;
            int numOfExposures = pta.steps.size();
            int numOfOffsets = numOfExposures - 1;
            int coadds = p.observation().calculationMethod().coaddsOrElse(1);
            String category = (ct.detail == null) ? ct.category.display : ct.detail;

            buf.append("<tr>");
            buf.append("<td>").append(category).append("</td>");

            if (category.equals("Exposure") && (coadds !=1 )) {
                secStr = String.format("%d exp x (%d coadds x %.1f s)", numOfExposures, coadds, ct.time/1000.0/coadds);
            } else if ((category.equals("Readout") && (coadds!=1) && (readoutTimePerCoadd != 0) )) {
                secStr = String.format("%d exp x (%d coadds x %.1f s)", numOfExposures, coadds, readoutTimePerCoadd, ct.time/1000.0);
            } else if (category.equals("Telescope Offset")) {
                if (p.observation().calculationMethod() instanceof Spectroscopy) { numOfOffsets = numOfExposures/2; }
                secStr = String.format("%d x %.1f s", numOfOffsets, ct.time / 1000.0);
            } else {
                secStr = String.format("%d exp x %.1f s", numOfExposures, ct.time/1000.0);
            }

            buf.append("<td align=\"right\"> ").append(secStr).append("</td>");
            buf.append("</tr>");
        }
        buf.append("<tr><td><b>Total time</b></td><td align=\"right\"><b>").append(String.format("%s", TimeAmountFormatter.getDescriptiveFormat(totalTime))).append("</b></td></tr>");
        buf.append("</table>");

        buf.append("</body></html>");
        return buf.toString();
    }

}
