package edu.gemini.itc.web.servlets;

import edu.gemini.itc.shared.*;
import org.jfree.chart.ChartUtilities;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This servlet provides data files and charts for spectroscopy results that have previously
 * been calculated and are cached by this servlet.
 */
public final class ImageServlet extends HttpServlet {

    public static final String ParamType        = "type";
    public static final String ParamId          = "id";
    public static final String ParamName        = "filename";
    public static final String ParamIndex       = "index";
    public static final String ParamLoLimit     = "loLimit";
    public static final String ParamHiLimit     = "hiLimit";

    public static final String TypeImg          = "img";
    public static final String TypeTxt          = "txt";

    private static final Logger Log = Logger.getLogger(ImageServlet.class.getName());

    // === Caching
    // We need to keep the results of ITC calculations in memory for a while in order to be able to serve
    // requests for images and data files (spectras) when accessing the ITC calculations through the web page.
    // (The original ITC used to write files to /tmp but this is slower than doing all of this in memory
    // and also can clog up the disk drive if the /tmp files linger around for too long.)

    public static class IdTimedOutException extends RuntimeException {}

    private static class LRU extends LinkedHashMap<UUID, ItcSpectroscopyResult> {
        private static final int CacheLimit = 300;
        @Override protected boolean removeEldestEntry(final Map.Entry<UUID, ItcSpectroscopyResult> eldest) {
            return size() > CacheLimit;
        }
    }

    /** Hash map that temporarily stores calculation results which will be needed for charts and data files. */
    private static final Map<UUID, ItcSpectroscopyResult> cachedResult = Collections.synchronizedMap(new LRU());

    /** Caches a spectroscopy result. Called by Printer classes when creating HTML output. */
    public static UUID cache(final ItcSpectroscopyResult result) {
        final UUID id = UUID.randomUUID();
        cachedResult.put(id, result);
        return id;
    }

    /** Retrieves a cached result from UUID string. */
    private static ItcSpectroscopyResult result(final String id) {
        return cachedResult.get(UUID.fromString(id));
    }
    
    // === End of caching

    /**
     * Called by server when an image or a result data file is requested.
     */
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        try {
            final String filename = request.getParameter(ParamName);
            final String type     = request.getParameter(ParamType);
            final String id       = request.getParameter(ParamId);
            final int index       = Integer.parseInt(request.getParameter(ParamIndex));

            switch (type) {

                case TypeTxt:
                    response.setContentType("text/plain");
                    response.getOutputStream().write(toFile(id, filename, index).getBytes());
                    break;

                case TypeImg:
                    response.setContentType("image/png");
                    final PlottingDetails pd = toPlottingDetails(request);
                    ChartUtilities.writeBufferedImageAsPNG(response.getOutputStream(), toImage(id, filename, index, pd));
                    break;

                default:
                    throw new Error();
            }

        } catch (IdTimedOutException e) {
            Log.log(Level.WARNING, "Session has timed out, the requested result is not available anymore", e);
            response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);

        } catch (NumberFormatException e) {
            Log.log(Level.WARNING, "The request is malformed " + e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);

        } catch (Exception e) {
            Log.log(Level.WARNING, e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static PlottingDetails toPlottingDetails(final HttpServletRequest request) {
        final String loLimitStr = request.getParameter(ParamLoLimit);
        final String upLimitStr = request.getParameter(ParamHiLimit);
        if (loLimitStr != null && upLimitStr != null) {
            final double loLimit = Double.parseDouble(loLimitStr);
            final double upLimit = Double.parseDouble(upLimitStr);
            return new PlottingDetails(PlottingDetails.PlotLimits.USER, loLimit, upLimit);
        } else {
            return PlottingDetails.Auto;
        }
    }

    private static BufferedImage toImage(final String id, final String filename, final int index, final PlottingDetails pd) {
        final ItcSpectroscopyResult results = result(id);
        final ITCChart chart;
        switch (filename) {
            case "SignalChart": chart = ITCChart.forSpcDataSet(results.chart(SignalChart.instance(), index), pd); break;
            case "S2NChart":    chart = ITCChart.forSpcDataSet(results.chart(S2NChart.instance(),    index), pd); break;
            default:            throw new Error();
        }
        return chart.getBufferedImage(675, 500);
    }

    // this is public because we use it for testing
    public static String toFile(final String id, final String filename, final int index) {
        final ItcSpectroscopyResult result = result(id);
        final String file;
        switch (filename) {
            case "SignalData":     file = result.file(SignalData.instance(), index).file(); break;
            case "BackgroundData": file = result.file(BackgroundData.instance(), index).file(); break;
            case "SingleS2NData":  file = result.file(SingleS2NData.instance(), index).file(); break;
            case "FinalS2NData":   file = result.file(FinalS2NData.instance(), index).file(); break;
            default:               throw new Error();
        }
        return "# ITC Data: " + Calendar.getInstance().getTime() + "\n \n" + file;
    }

}
