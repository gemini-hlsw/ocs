package edu.gemini.itc.web.servlets;

import edu.gemini.itc.gmos.Gmos;
import edu.gemini.itc.gmos.GmosRecipe;
import edu.gemini.itc.gnirs.GnirsRecipe;
import edu.gemini.itc.nifs.Nifs;
import edu.gemini.itc.nifs.NifsParameters;
import edu.gemini.itc.nifs.NifsRecipe;
import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.html.FormatStringWriter;
import edu.gemini.itc.web.html.PrinterBase;
import org.jfree.chart.ChartUtilities;
import scala.Tuple2;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This servlet provides data files and charts for spectroscopy results that have previously
 * been calculated and are cached by this servlet.
 */
public final class ImageServlet extends HttpServlet {

    public static final String SigSpec              = "sigSpec";
    public static final String BackSpec             = "backSpec";
    public static final String SingleS2N            = "singleS2N";
    public static final String FinalS2N             = "finalS2N";

    public static final String GnirsSigOrder        = "gnirsSigOrder";
    public static final String GnirsBgOrder         = "gnirsBgOrder";
    public static final String GnirsFinalS2NOrder   = "gnirsFinalS2NOrder";

    public static final String GmosSigSpec          = "gmosSigSpec";
    public static final String GmosBackSpec         = "gmosBackSpec";
    public static final String GmosSingleS2N        = "gmosSingleS2N";
    public static final String GmosFinalS2N         = "gmosFinalS2N";

    public static final String SigChart             = "sigChart";
    public static final String S2NChart             = "s2nChart";
    public static final String SigSwApChart         = "niriSigChart";
    public static final String NifsSigChart         = "nifsSigChart";
    public static final String NifsS2NChart         = "nifsS2nChart";
    public static final String GnirsSigChart        = "gnirsSigChart";
    public static final String GnirsS2NChart        = "gnirsS2nChart";
    public static final String GmosSigChart         = "gmosSigChart";
    public static final String GmosS2NChart         = "gmosS2nChart";

    private static final Logger Log = Logger.getLogger(ImageServlet.class.getName());

    private static final String IMG = "img";
    private static final String TXT = "txt";

    // === Caching
    // We need to keep the results of ITC calculations in memory for a while in order to be able to serve
    // requests for images and data files (spectras) when accessing the ITC calculations through the web page.

    public static class IdTimedOutException extends RuntimeException {}

    private static final int UpperLimit = 600;

    /** Hash map that temporarily stores calculation results which will be needed for charts and data files. */
    private static final Map<UUID, Tuple2<Long, ItcSpectroscopyResult>> cachedResult = new ConcurrentHashMap<>(UpperLimit);

    /** Caches a spectroscopy result */
    public static UUID cache(final ItcSpectroscopyResult result) {
        if (cachedResult.size() > UpperLimit) {
            cleanCache();
        }
        final UUID id = UUID.randomUUID();
        cachedResult.put(id, new Tuple2<>(System.currentTimeMillis(), result));
        return id;
    }

    /** Retrieves an array of results for multiple CCDs. */
    private static ItcSpectroscopyResult result(final String id) {
        return result(UUID.fromString(id));
    }

    private static ItcSpectroscopyResult result(final UUID id) {
        final Tuple2<Long, ItcSpectroscopyResult> r = cachedResult.get(id);
        if (r == null) throw new IdTimedOutException();
        return cachedResult.get(id)._2();

    }

    // A very basic LRU caching strategy that tosses the oldest 1/3 elements whenever we hit the UpperLimit.
    private static synchronized void cleanCache() {
        if (cachedResult.size() > UpperLimit) {
            cachedResult.keySet().stream().
                    sorted((id1, id2) -> (int) (cachedResult.get(id1)._1() - cachedResult.get(id2)._1())).
                    limit(UpperLimit/3).
                    forEach(cachedResult::remove);
        }
    }

    // === End of caching

    /**
     * Called by server when an image or a result data file is requested.
     */
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        try {
            final String filename = request.getParameter("filename");
            final String type = request.getParameter("type");
            final String id = request.getParameter("id");
            final int index = Integer.parseInt(request.getParameter("index"));

            switch (type) {

                case TXT:
                    response.setContentType("text/plain");
                    response.getOutputStream().write(toFile(id, filename, index).getBytes());
                    break;

                case IMG:
                    response.setContentType("image/png");
                    ChartUtilities.writeBufferedImageAsPNG(response.getOutputStream(), toImage(id, filename, index));
                    break;

                default:
                    throw new Error();
            }

        } catch (IdTimedOutException e) {
            Log.log(Level.WARNING, "Session has timed out, the requested result is not available anymore", e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);

        } catch (Exception e) {
            Log.log(Level.WARNING, e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // ===

    public static BufferedImage toImage(final String id, final String filename, final int index) {
        final ItcSpectroscopyResult results = result(id);
        final PlottingDetails pd = new PlottingDetails(PlottingDetails.PlotLimits.AUTO, 0, 1); // TODO how do we get the plot details parameters?? encode them in url??
        final ITCChart file;
        switch (filename) {
            case SigChart:      file = ITCChart.forSpcDataSet(results.dataSets().apply(0), pd);   break;
            case S2NChart:      file = ITCChart.forSpcDataSet(results.dataSets().apply(1), pd);   break;
            case SigSwApChart:  file = ITCChart.forSpcDataSet(results.dataSets().apply(0), pd);   break;
            case NifsSigChart:  file = ITCChart.forSpcDataSet(results.dataSets().apply(2*index),   pd);   break;
            case NifsS2NChart:  file = ITCChart.forSpcDataSet(results.dataSets().apply(2 * index + 1), pd); break;
            case GnirsSigChart: file = ITCChart.forSpcDataSet(results.dataSets().apply(0), pd);   break;
            case GnirsS2NChart: file = ITCChart.forSpcDataSet(results.dataSets().apply(1), pd);  break;
            case GmosSigChart:  file = ITCChart.forSpcDataSet(results.dataSets().apply(0), pd);   break;
            case GmosS2NChart:  file = ITCChart.forSpcDataSet(results.dataSets().apply(1), pd);  break;
            default:            throw new Error();
        }
        return file.getBufferedImage();
    }


    public static String toFile(final String id, final String filename, final int index) {
        final ItcSpectroscopyResult result = result(id);
        final String file =
                "# ITC: " +
                        Calendar.getInstance().getTime() + "\n \n" +
                        result.dataFiles().apply(index).file();
        return file;
    }

}
