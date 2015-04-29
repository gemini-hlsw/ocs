package edu.gemini.itc.web.html;

import edu.gemini.itc.gmos.Gmos;
import edu.gemini.itc.gnirs.GnirsRecipe;
import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.servlets.ImageServlet;
import org.jfree.chart.ChartColor;
import scala.Tuple2;

import java.awt.*;
import java.io.PrintWriter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class PrinterBase {

    // === Caching
    // TODO: this could can maybe live in the ImageServlet?
    // TODO: how to deal with single result vs array of resulst (GMOS CCDs)??
    private static final Map<UUID, Tuple2<Long, SpectroscopyResult[]>> cachedResult = new ConcurrentHashMap<>();

    protected Tuple2<UUID, SpectroscopyResult> cache(final SpectroscopyResult result) {
        final SpectroscopyResult[] arr = new SpectroscopyResult[1];
        arr[0] = result;
        final Tuple2<UUID, SpectroscopyResult[]> stored = cache(arr);
        return new Tuple2<>(stored._1(), result);
    }
    protected Tuple2<UUID, SpectroscopyResult[]> cache(final SpectroscopyResult[] result) {
        if (cachedResult.size() > 100) {
            cleanCache();
        }
        final UUID id = UUID.randomUUID();
        cachedResult.put(id, new Tuple2<>(System.currentTimeMillis(), result));
        return new Tuple2<>(id, result);
    }

    public static SpectroscopyResult result(final String id) {
        final UUID uuid = UUID.fromString(id);
        return cachedResult.get(uuid)._2()[0]; //TODO : how to deal with missing results ??
    }
    public static SpectroscopyResult[] results(final String id) {
        final UUID uuid = UUID.fromString(id);
        return cachedResult.get(uuid)._2(); //TODO : how to deal with missing results ??
    }

    // TODO: what is a good caching strategy? how long must/can we keep results?
    private static synchronized void cleanCache() {
        final long now = System.currentTimeMillis();
        final long maxKeepTime;
        if      (cachedResult.size() > 1000) maxKeepTime = 10000;   // 10 s
        else if (cachedResult.size() > 500)  maxKeepTime = 20000;  // 20 s
        else if (cachedResult.size() > 250)  maxKeepTime = 60000;   // 1 minute
        else                                 maxKeepTime = 3600000; // 60 minutes

        cachedResult.keySet().stream().
                filter(s -> (now - cachedResult.get(s)._1()) > maxKeepTime).
                forEach(cachedResult::remove);
    }

    // === Caching

    public abstract void writeOutput();

    private final PrintWriter _out;

    protected PrinterBase(final PrintWriter pr) {
        _out = pr;
    }

    /* TODO: this needs to be validated for spectroscopy for all instruments, find a better place to do this */
    protected void validatePlottingDetails(final PlottingDetails pdp, final Instrument instrument) {
        if (pdp != null && pdp.getPlotLimits().equals(PlottingDetails.PlotLimits.USER)) {
            if (pdp.getPlotWaveL() > instrument.getObservingEnd() || pdp.getPlotWaveU() < instrument.getObservingStart()) {
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

    // =============

    protected void _printFileLink(final UUID id, final String name, final String label) {
        _printFileLink(id, name, 0, label);
    }

    protected void _printFileLink(final UUID id, final String name, final int index, final String label) {
        _println("<a href =" +
                "\"" + ServerInfo.getServerURL()
                + "itc/servlet/images?type=txt"
                + "&filename=" + name
                + "&index=" + index
                + "&id=" + id
                //+ "&SessionID=0" // TODO: add fake "SessionID" in order to have URL ignored in science regression tests
                + "\"> Click here for " + label + ". </a>");
    }

    protected void _printImageLink(final UUID id, final String name) {
        _printImageLink(id, name, 0);
    }
    protected void _printImageLink(final UUID id, final String name, final int index) {
        _print("<IMG alt=\"SessionID123456.png\" height=500 src=\"" + ServerInfo.getServerURL() // TODO: get rid of SessionID when recreating baseline
                + "itc/servlet/images?type=img"
                + "&filename=" + name
                + "&index=" + index
                + "&id=" + id
                //+ "&SessionID=0" // TODO: add fake "SessionID" in order to have URL ignored in science regression tests
                + "\" width=675 border=0>");
    }


    // =============

    // CHART CREATION
    // Utility functions that create generic signal and signal to noise charts for several instruments.

    public static ITCChart createSignalChart(final PlottingDetails pd, final SpectroscopyResult result, final int index) {
        return createSignalChart(pd, result, "Signal and Background ", index);
    }

    public static ITCChart createSignalChart(final PlottingDetails pd, final SpectroscopyResult result, final String title, final int index) {
        final ITCChart chart = new ITCChart(title, "Wavelength (nm)", "e- per exposure per spectral pixel", pd);
        chart.addArray(result.specS2N()[index].getSignalSpectrum().getData(), "Signal ");
        chart.addArray(result.specS2N()[index].getBackgroundSpectrum().getData(), "SQRT(Background)  ");
        return chart;
    }


    public static ITCChart createS2NChart(final PlottingDetails pd, final SpectroscopyResult result, final int index) {
        return createS2NChart(pd, result, "Intermediate Single Exp and Final S/N", index);
    }

    public static ITCChart createS2NChart(final PlottingDetails pd, final SpectroscopyResult result, final String title, final int index) {
        final ITCChart chart = new ITCChart(title, "Wavelength (nm)", "Signal / Noise per spectral pixel", pd);
        chart.addArray(result.specS2N()[index].getExpS2NSpectrum().getData(), "Single Exp S/N");
        chart.addArray(result.specS2N()[index].getFinalS2NSpectrum().getData(), "Final S/N  ");
        return chart;
    }

    // -- GNIRS specific

    private static final Color[] ORDER_COLORS =
            new Color[] {ChartColor.DARK_RED,      ChartColor.DARK_BLUE,       ChartColor.DARK_GREEN,
                    ChartColor.DARK_MAGENTA,       ChartColor.black,           ChartColor.DARK_CYAN};
    private static final Color[] ORDER_BG_COLORS =
            new Color[] {ChartColor.VERY_LIGHT_RED,ChartColor.VERY_LIGHT_BLUE, ChartColor.VERY_LIGHT_GREEN,
                    ChartColor.VERY_LIGHT_MAGENTA, ChartColor.lightGray,       ChartColor.VERY_LIGHT_CYAN};

    public static ITCChart createGnirsSignalChart(final PlottingDetails pd, final GnirsSpectroscopyResult result) {
        final ITCChart chart = new ITCChart("Signal and Background in software aperture of " + result.specS2N()[0].getSpecNpix() + " pixels", "Wavelength (nm)", "e- per exposure per spectral pixel", pd);
        for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
            chart.addArray(result.signalOrder()[i].getData(), "Signal Order " + (i + 3), ORDER_COLORS[i]);
            chart.addArray(result.backGroundOrder()[i].getData(), "SQRT(Background) Order " + (i + 3), ORDER_BG_COLORS[i]);
        }
        return chart;
    }

    public static ITCChart createGnirsS2NChart(final PlottingDetails pd, final GnirsSpectroscopyResult result) {
        final ITCChart chart = new ITCChart("Final S/N", "Wavelength (nm)", "Signal / Noise per spectral pixel", pd);
        for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
            chart.addArray(result.finalS2NOrder()[i].getData(), "Final S/N Order " + (i + 3), ORDER_COLORS[i]);
        }
        return chart;
    }

    // -- GMOS specific
    // TODO: this sucks, GMOS has an entirely device specific BS going on for multi CCD support
    // TODO: can we somehow unify this??
    public static ITCChart createGmosChart(final PlottingDetails pd, final SpectroscopyResult[] results, final String filename) {
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        final Gmos mainInstrument = (Gmos) results[0].instrument(); // TODO: make sure this is indeed GMOS!
        final DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();
        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();

        final boolean ifuAndNotUniform = mainInstrument.isIfuUsed() && !(results[0].source().isUniform());
        final double ifu_offset = ifuAndNotUniform ? mainInstrument.getIFU().getApertureOffsetList().iterator().next() : 0.0;
        final String title;
        final String title2;
        switch (filename) {
            case ImageServlet.GmosSigChart:
                title = ifuAndNotUniform ? "Signal and Background (IFU element offset: " + device.toString(ifu_offset) + " arcsec)" : "Signal and Background ";
                title2 = "e- per exposure per spectral pixel";
                break;

            case ImageServlet.GmosS2NChart:
                title = ifuAndNotUniform ? "Intermediate Single Exp and Final S/N (IFU element offset: " + device.toString(ifu_offset) + " arcsec)" : "Intermediate Single Exp and Final S/N";
                title2 = "Signal / Noise per spectral pixel";
                break;

            default:
                throw new Error();
        }

        final ITCChart chart = new ITCChart(title, "Wavelength (nm)", title2, pd);

        for (final Gmos instrument : ccdArray) {

            final int ccdIndex = instrument.getDetectorCcdIndex();
            final String ccdName = instrument.getDetectorCcdName();
            final Color ccdColor = instrument.getDetectorCcdColor();
            final Color ccdColorDarker = ccdColor == null ? null : ccdColor.darker().darker();
            final int first = tv.getDetectorCcdStartIndex(ccdIndex);
            final int last = tv.getDetectorCcdEndIndex(ccdIndex, ccdArray.length);

            final SpectroscopyResult result = results[ccdIndex];
            for (int i = 0; i < result.specS2N().length; i++) {
                switch (filename) {
                    case ImageServlet.GmosSigChart:
                        chart.addArray(result.specS2N()[i].getSignalSpectrum().getData(first, last), "Signal " + ccdName, ccdColor);
                        chart.addArray(result.specS2N()[i].getBackgroundSpectrum().getData(first, last), "SQRT(Background) " + ccdName, ccdColorDarker);
                        break;

                    case ImageServlet.GmosS2NChart:
                        chart.addArray(result.specS2N()[i].getExpS2NSpectrum().getData(first, last), "Single Exp S/N " + ccdName, ccdColor);
                        chart.addArray(result.specS2N()[i].getFinalS2NSpectrum().getData(first, last), "Final S/N " + ccdName, ccdColorDarker);
                        break;

                    default:
                        throw new Error();
                }
            }
        }

        return chart;
    }



}
