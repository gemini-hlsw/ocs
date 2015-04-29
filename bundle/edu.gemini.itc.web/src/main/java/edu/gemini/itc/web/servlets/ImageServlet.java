package edu.gemini.itc.web.servlets;

import edu.gemini.itc.gmos.Gmos;
import edu.gemini.itc.nifs.Nifs;
import edu.gemini.itc.nifs.NifsParameters;
import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.html.FormatStringWriter;
import edu.gemini.itc.web.html.PrinterBase;
import org.jfree.chart.ChartUtilities;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
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

    public static final String SigChart             = "sigChart";
    public static final String S2NChart             = "s2nChart";
    public static final String SigSwApChart         = "niriSigChart";
    public static final String NifsSigChart         = "nifsSigChart";
    public static final String NifsS2NChart         = "nifsS2nChart";
    public static final String GnirsSigChart        = "gnirsSigChart";
    public static final String GnirsS2NChart        = "gnirsS2nChart";
    public static final String GmosSigChart        = "gmosSigChart";
    public static final String GmosS2NChart        = "gmosS2nChart";

    private static final Logger Log = Logger.getLogger(ImageServlet.class.getName());

    private static final String IMG = "img";
    private static final String TXT = "txt";

    /**
     * Called by server when an image or a result data file is requested.
     */
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        try {
            final String filename = request.getParameter("filename");
            final String type = request.getParameter("type");
            final String id = request.getParameter("id");
            final int index = Integer.parseInt(request.getParameter("index"));

            // set the content type of reply
            switch (type) {
                case IMG: response.setContentType("image/png");  break;
                case TXT: response.setContentType("text/plain"); break;
                default:  response.setContentType("text/plain");
            }

            switch (type) {
                case TXT:   response.getOutputStream().write(toFile(id, filename, index).getBytes()); break;
                case IMG:   ChartUtilities.writeBufferedImageAsPNG(response.getOutputStream(), toImage(id, filename, index)); break;
                default:    throw new Error();
            }

        } catch (Exception e) {
            Log.log(Level.WARNING, "Session has timed out, this reult is not available anymore", e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);

//        } catch (Exception e) {
//            Log.log(Level.WARNING, "Problem with file: " + filename, e);
//            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // ===

    public static BufferedImage toImage(final String id, final String filename, final int index) {
        final SpectroscopyResult result = PrinterBase.result(id);
        final SpectroscopyResult[] results = PrinterBase.results(id);
        final PlottingDetails pd = new PlottingDetails(PlottingDetails.PlotLimits.AUTO, 0, 1); // TODO how do we get the plot details parameters?? encode them in url??
        final ITCChart file;
        switch (filename) {
            case SigChart:      file = PrinterBase.createSignalChart(pd, result, 0);   break;
            case S2NChart:      file = PrinterBase.createS2NChart(pd, result, 0);      break;
            case SigSwApChart:  file = PrinterBase.createSignalChart(pd, result, "Signal and SQRT(Background) in software aperture of " + result.specS2N()[0].getSpecNpix() + " pixels", 0);   break;
            case NifsSigChart:  file = PrinterBase.createSignalChart(pd, result, getNifsSigChartTitle(result, index), index);   break;
            case NifsS2NChart:  file = PrinterBase.createS2NChart(pd, result, getNifsS2NChartTitle(result, index), index);   break;
            case GnirsSigChart: file = PrinterBase.createGnirsSignalChart(pd, (GnirsSpectroscopyResult) result);   break;
            case GnirsS2NChart: file = PrinterBase.createGnirsS2NChart(pd, (GnirsSpectroscopyResult) result);  break;
            case GmosSigChart:  file = PrinterBase.createGmosChart(pd, results, GmosSigChart);   break;
            case GmosS2NChart:  file = PrinterBase.createGmosChart(pd, results, GmosS2NChart);  break;
            default:            throw new Error();
        }
        return file.getBufferedImage();
    }

    private static String getNifsSigChartTitle(final SpectroscopyResult result, final int index) {
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(3);  // NO decimal places
        device.clear();
        final Nifs instrument = (Nifs) result.instrument();
        final List<Double> ap_offset_list = instrument.getIFU().getApertureOffsetList();
        return instrument.getIFUMethod().equals(NifsParameters.SUMMED_APERTURE_IFU) ?
                "Signal and Background (IFU summed apertures: " +
                        device.toString(instrument.getIFUNumX()) + "x" + device.toString(instrument.getIFUNumY()) +
                        ", " + device.toString(instrument.getIFUNumX() * instrument.getIFU().IFU_LEN_X) + "\"x" +
                        device.toString(instrument.getIFUNumY() * instrument.getIFU().IFU_LEN_Y) + "\")" :
                "Signal and Background (IFU element offset: " + device.toString(ap_offset_list.get(index)) + " arcsec)";
    }
    private static String getNifsS2NChartTitle(final SpectroscopyResult result, final int index) {
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(3);  // NO decimal places
        device.clear();
        final Nifs instrument = (Nifs) result.instrument();
        final List<Double> ap_offset_list = instrument.getIFU().getApertureOffsetList();
        return instrument.getIFUMethod().equals(NifsParameters.SUMMED_APERTURE_IFU) ?
                "Intermediate Single Exp and Final S/N \n(IFU apertures:" +
                        device.toString(instrument.getIFUNumX()) + "x" + device.toString(instrument.getIFUNumY()) +
                        ", " + device.toString(instrument.getIFUNumX() * instrument.getIFU().IFU_LEN_X) + "\"x" +
                        device.toString(instrument.getIFUNumY() * instrument.getIFU().IFU_LEN_Y) + "\")" :
                "Intermediate Single Exp and Final S/N (IFU element offset: " + device.toString(ap_offset_list.get(index)) + " arcsec)";

    }



    // TODO: where should this code live?
    public static String toFile(final String id, final String filename, final int index) {
        final SpectroscopyResult result = PrinterBase.result(id);
        final SpectroscopyResult[] results = PrinterBase.results(id);
        final String file;
        switch (filename) {
            case SigSpec:               file = toFile(result.specS2N()[index].getSignalSpectrum());             break;
            case BackSpec:              file = toFile(result.specS2N()[index].getBackgroundSpectrum());         break;
            case SingleS2N:             file = toFile(result.specS2N()[index].getExpS2NSpectrum());             break;
            case FinalS2N:              file = toFile(result.specS2N()[index].getFinalS2NSpectrum());           break;
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
                // TODO: why are we using the last specS2N element only? this seems fishy..?
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
