package edu.gemini.itc.web.servlets;

import edu.gemini.itc.shared.*;
import org.jfree.chart.ChartUtilities;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.logging.Level;
import java.util.logging.Logger;

import scala.collection.JavaConversions;

/**
 * This servlet provides data files and charts for spectroscopy results that have previously
 * been calculated and are cached by this servlet.
 */
public final class FilesServlet extends HttpServlet {

    public static final String ParamType        = "type";
    public static final String ParamId          = "id";
    public static final String ParamName        = "filename";
    public static final String ParamChartIndex  = "chartIndex";
    public static final String ParamSeriesIndex = "seriesIndex";
    public static final String ParamLoLimit     = "loLimit";
    public static final String ParamHiLimit     = "hiLimit";

    public static final String TypeImg          = "img";
    public static final String TypeTxt          = "txt";

    private static final Logger Log = Logger.getLogger(FilesServlet.class.getName());

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
        final ItcSpectroscopyResult r = cachedResult.get(UUID.fromString(id));
        if (r == null) throw new IdTimedOutException();
        return r;
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
            final int chartIndex  = Integer.parseInt(request.getParameter(ParamChartIndex));
            final Optional<List<Integer>> seriesIndex = Optional.ofNullable(request.getParameterValues(ParamSeriesIndex)).map(i -> Stream.of(i).map(Integer::parseInt).collect(Collectors.toList()));

            switch (type) {

                case TypeTxt:
                    response.setContentType("text/plain");
                    response.getOutputStream().write(toFile(id, filename, chartIndex, seriesIndex).getBytes());
                    break;

                case TypeImg:
                    response.setContentType("image/png");
                    final PlottingDetails pd = toPlottingDetails(request);
                    ChartUtilities.writeBufferedImageAsPNG(response.getOutputStream(), toImage(id, filename, chartIndex, pd));
                    break;

                default:
                    throw new Error();
            }

        } catch (final IdTimedOutException e) {
            // if this message comes up a lot we might need to tweak the cache settings
            Log.log(Level.WARNING, "Session has timed out, the requested result is not available anymore");
            response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);

        } catch (final IllegalArgumentException e) {
            Log.log(Level.WARNING, "The request is malformed " + e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);

        } catch (final Exception e) {
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
            case "SignalChart":       chart = ITCChart.forSpcDataSet(results.chart(SignalChart.instance(),      index), pd); break;
            case "S2NChart": {
                scala.collection.immutable.List<SpcChartGroup> list = results.chartGroups();
                Iterable<SpcChartGroup> it = JavaConversions.asJavaIterable(list);
                Iterator<SpcChartGroup> it2 = it.iterator();
                while(it2.hasNext()){
                    SpcChartGroup spcChartGroup = it2.next();
                    System.out.println("lenght of charGroup: " + spcChartGroup.charts().length());
                }
                chart = ITCChart.forSpcDataSet(results.chart(S2NChart.instance(),         index), pd);
                break;
            }
            case "S2NChartPerRes": {
                scala.collection.immutable.List<SpcChartGroup> list = results.chartGroups();
                Iterable<SpcChartGroup> it = JavaConversions.asJavaIterable(list);
                Iterator<SpcChartGroup> it2 = it.iterator();
                while(it2.hasNext()){
                    SpcChartGroup spcChartGroup = it2.next();
                    System.out.println("lenght2 of charGroup: " + spcChartGroup.charts().length());
                }
                chart = ITCChart.forSpcDataSet(results.chart(S2NChartPerRes.instance(),         index), pd);
                break;
            }

            case "SignalPixelChart":  chart = ITCChart.forSpcDataSet(results.chart(SignalPixelChart.instance(), index), pd); break;
            default:            throw new Error();
        }
        return chart.getBufferedImage(800, 600);
    }

    // this is public because we use it for testing
    public static String toFile(final String id, final String filename, final int chartIndex, final Optional<List<Integer>> seriesIndex) {
        final ItcSpectroscopyResult result = result(id);
        final String file;
        switch (filename) {
            case "SignalData":     file = toFile(result.chart(SignalChart.instance(), chartIndex).allSeriesAsJava(SignalData.instance()),     seriesIndex); break;
            case "BackgroundData": file = toFile(result.chart(SignalChart.instance(), chartIndex).allSeriesAsJava(BackgroundData.instance()), seriesIndex); break;
            case "SingleS2NData":  file = toFile(result.chart(S2NChart.instance(),    chartIndex).allSeriesAsJava(SingleS2NData.instance()),  seriesIndex); break;
            case "FinalS2NData":   file = toFile(result.chart(S2NChart.instance(),    chartIndex).allSeriesAsJava(FinalS2NData.instance()),   seriesIndex); break;
            case "SingleS2NPerResEle":  file = toFile(result.chart(S2NChartPerRes.instance(),    chartIndex).allSeriesAsJava(SingleS2NPerResEle.instance()),  seriesIndex); break;
            case "FinalS2NPerResEle":   file = toFile(result.chart(S2NChartPerRes.instance(),    chartIndex).allSeriesAsJava(FinalS2NPerResEle.instance()),   seriesIndex); break;
            case "PixSigData":     file = toFile(result.chart(SignalPixelChart.instance(),    chartIndex).allSeriesAsJava(SignalData.instance()),  seriesIndex); break;
            case "PixBackData":    file = toFile(result.chart(SignalPixelChart.instance(),    chartIndex).allSeriesAsJava(BackgroundData.instance()),   seriesIndex); break;
            default:               throw new Error();
        }
        return "# ITC Data: " + Calendar.getInstance().getTime() + "\n \n" + file;
    }

    private static String toFile(final List<SpcSeriesData> dataSeries, final Optional<List<Integer>> seriesIndex) {
        return seriesIndex.
                map(si -> toFiles(dataSeries, si)).
                orElse(toFiles(dataSeries));
    }

    private static String toFiles(final List<SpcSeriesData> dataSeries) {
        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < dataSeries.size(); i++) {
            indices.add(i);
        }

        return toFiles(dataSeries, indices);
    }

    private static String toFiles(final List<SpcSeriesData> dataSeries, final List<Integer> indices) {
        final StringBuilder sb = new StringBuilder();

        for (int i: indices) {
            sb.append(toFile(dataSeries.get(i)));
        }

        return sb.toString();
    }

    private static String toFile(final SpcSeriesData data) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.data()[0].length; i++) {
            sb.append(String.format("%.3f\t%.3f\n", data.data()[0][i], data.data()[1][i]));
        }
        return sb.toString();
    }
}
