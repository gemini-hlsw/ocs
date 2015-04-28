package edu.gemini.itc.web.html;

import edu.gemini.itc.shared.*;
import scala.Tuple2;

import java.awt.image.BufferedImage;
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

    protected void _println(final BufferedImage image, final String imageName) {
        try {
            final String fileName = ITCImageFileIO.saveCharttoDisk(image);
            _print("<IMG alt=\"" + fileName
                    + "\" height=500 src=\"" + ServerInfo.getServerURL()
                    + "itc/servlet/images?type=img&filename="
                    + fileName + "\" width=675 border=0>");
        } catch (Exception ex) {
            System.out.println("Unable to save file");
            _print("<br>Failed to save image " + imageName + "<br>");
            ex.printStackTrace();
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
        _printLink(id, "txt", name, label);
    }
    protected void _printImageLink(final UUID id, final String name, final String label) {
        _printLink(id, "img", name, label);
    }
    // TODO: SessionID is only here for science regression tests, can be removed asap
    protected void _printLink(final UUID id, final String type, final String name, final String label) {
        _println("<a href ="
                +
                "\"" + ServerInfo.getServerURL()
                + "itc/servlet/images?type="+type+"&filename=" + name + "&id=" + id //+ "&SessionID=0"
                + "\"> Click here for " + label + ". </a>");
    }

}
