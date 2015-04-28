package edu.gemini.itc.web.servlets;

import edu.gemini.itc.gmos.Gmos;
import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.GnirsSpectroscopyResult;
import edu.gemini.itc.shared.ITCImageFileIO;
import edu.gemini.itc.shared.SpectroscopyResult;
import edu.gemini.itc.shared.VisitableSampledSpectrum;
import edu.gemini.itc.web.html.PrinterBase;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Image servlet for ITC. The instrument Servlet adds a tag the points to .
 * this servlet for each Image it wants to send the user. This servlet reads
 * the tag, opens the file and sends it to the user using a servlet output
 * stream.
 */
public final class ImageServlet extends HttpServlet {

    public static final String SigSpec          = "sigSpec";
    public static final String BackSpec         = "backSpec";
    public static final String SingleS2N        = "singleS2N";
    public static final String FinalS2N         = "finalS2N";

    public static final String GnirsSigOrder       = "gnirsSigOrder";
    public static final String GnirsBgOrder        = "gnirsBgOrder";
    public static final String GnirsFinalS2NOrder  = "gnirsFinalS2NOrder";

    public static final String GmosSigSpec          = "gmosSigSpec";
    public static final String GmosBackSpec         = "gmosBackSpec";
    public static final String GmosSingleS2N        = "gmosSingleS2N";
    public static final String GmosFinalS2N         = "gmosFinalS2N";

    private static final Logger Log = Logger.getLogger(ImageServlet.class.getName());

    private static final String IMG = "img";
    private static final String TXT = "txt";

    /**
     * Called by server when an image or a result data file is requested.
     */
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        final String filename = request.getParameter("filename");
        final String type = request.getParameter("type");
        final String id = request.getParameter("id");

        // set the content type of reply
        switch (type) {
            case IMG: response.setContentType("image/png");  break;
            case TXT: response.setContentType("text/plain"); break;
            default:  response.setContentType("text/plain");
        }

        try {

            if (id != null) {
                response.getOutputStream().write(toFile(id, filename).getBytes());

            } else {
                // copy file to output stream
                ITCImageFileIO.sendFiletoServOut(filename, response.getOutputStream());
            }

        } catch (FileNotFoundException e) {
            Log.log(Level.WARNING, "Unknown file requested: " + filename, e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);

        } catch (Exception e) {
            Log.log(Level.WARNING, "Problem with file: " + filename, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // ===
    // TODO: where should this code live?
    public static String toFile(final String id, final String filename) {
        final SpectroscopyResult result = PrinterBase.result(id);
        final SpectroscopyResult[] results = PrinterBase.results(id);
        final String file;
        switch (filename) {
            case SigSpec:               file = toFile(result.specS2N()[0].getSignalSpectrum());             break;
            case BackSpec:              file = toFile(result.specS2N()[0].getBackgroundSpectrum());         break;
            case SingleS2N:             file = toFile(result.specS2N()[0].getExpS2NSpectrum());             break;
            case FinalS2N:              file = toFile(result.specS2N()[0].getFinalS2NSpectrum());           break;
            case GnirsSigOrder:         file = toFile(((GnirsSpectroscopyResult)result).signalOrder());     break;
            case GnirsBgOrder:          file = toFile(((GnirsSpectroscopyResult)result).backGroundOrder()); break;
            case GnirsFinalS2NOrder:    file = toFile(((GnirsSpectroscopyResult)result).finalS2NOrder());   break;
            case GmosSigSpec:           file = toFile(results, SigSpec);                                    break;
            case GmosBackSpec:          file = toFile(results, BackSpec);                                   break;
            case GmosSingleS2N:         file = toFile(results, SingleS2N);                                  break;
            case GmosFinalS2N:          file = toFile(results, FinalS2N);                                   break;
            default:                    throw new Error();
        }
        return file;
    }

    protected static String toFile(final VisitableSampledSpectrum sed) {
        return "# ITC: " +
                Calendar.getInstance().getTime() + "\n \n" +
               sed.printSpecAsString();
    }

    protected static String toFile(final VisitableSampledSpectrum[] sedArr) {
        final StringBuilder sb = new StringBuilder("# ITC: " + Calendar.getInstance().getTime() + "\n \n");
        for (final VisitableSampledSpectrum sed : sedArr){
            sb.append(sed.printSpecAsString());
        }
        return sb.toString();
    }

    // TODO: this sucks, GMOS has an entirely device specific BS going on for multi CCD support
    // TODO: can we somehow unify this??
    protected static String toFile(final SpectroscopyResult[] results, final String filename) {
        final Gmos mainInstrument = (Gmos) results[0].instrument(); // TODO: make sure this is indeed GMOS!
        final DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();
        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();

        final StringBuilder sb = new StringBuilder("# ITC: " + Calendar.getInstance().getTime() + "\n \n");

        for (final Gmos instrument : ccdArray) {

            final int ccdIndex = instrument.getDetectorCcdIndex();
            final int first = tv.getDetectorCcdStartIndex(ccdIndex);
            final int last = tv.getDetectorCcdEndIndex(ccdIndex, ccdArray.length);
            // REL-478: include the gaps in the text data output
            final int lastWithGap = (ccdIndex < 2 && ccdArray.length > 1)
                    ? tv.getDetectorCcdStartIndex(ccdIndex + 1)
                    : last;

            final SpectroscopyResult result = results[ccdIndex];
            final VisitableSampledSpectrum sed;
            switch (filename) {
                case SigSpec:   sed = result.specS2N()[result.specS2N().length - 1].getSignalSpectrum(); break;
                case BackSpec:  sed = result.specS2N()[result.specS2N().length - 1].getBackgroundSpectrum(); break;
                case SingleS2N: sed = result.specS2N()[result.specS2N().length - 1].getExpS2NSpectrum(); break;
                case FinalS2N:  sed = result.specS2N()[result.specS2N().length - 1].getFinalS2NSpectrum(); break;
                default:
                    throw new Error();
            }
            sb.append(sed.printSpecAsString(first, lastWithGap));
        }
        return sb.toString();
    }
}
