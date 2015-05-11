package edu.gemini.itc.web.html;

import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.itc.flamingos2.Flamingos2;
import edu.gemini.itc.flamingos2.Flamingos2Recipe;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.FPUnit;
import scala.Tuple2;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing F2 calculation results to an output stream.
 */
public final class Flamingos2Printer extends PrinterBase {

    private final PlottingDetails pdp;
    private final Flamingos2Recipe recipe;
    private final boolean isImaging;

    public Flamingos2Printer(final Parameters p, final Flamingos2Parameters ip, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.pdp       = pdp;
        this.recipe    = new Flamingos2Recipe(p.source(), p.observation(), p.conditions(), ip, p.telescope());
        this.isImaging = p.observation().getMethod().isImaging();
    }

    /**
     * Performs recipe calculation and writes results to a cached PrintWriter or to System.out.
     */
    public void writeOutput() {
        if (isImaging) {
            final ImagingResult result = recipe.calculateImaging();
            writeImagingOutput(result);
        } else {
            final Tuple2<ItcSpectroscopyResult, SpectroscopyResult> r = recipe.calculateSpectroscopy();
            final UUID id = cache(r._1());
            writeSpectroscopyOutput(id, r._2());
            validatePlottingDetails(pdp, r._2().instrument());
        }
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult result) {

        // we know this is Flamingos
        final Flamingos2 instrument = (Flamingos2) result.instrument();

        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();
        _println("");

        _print(CalculatablePrinter.getTextResult(result.sfCalc(), device));
        _println(CalculatablePrinter.getTextResult(result.iqCalc(), device));

        if (!result.parameters().observation().isAutoAperture()) {
            _println("software aperture extent along slit = " + device.toString(result.parameters().observation().getApertureDiameter()) + " arcsec");
        } else {
            switch (result.parameters().source().getProfileType()) {
                case UNIFORM:
                    _println("software aperture extent along slit = "
                            + device.toString(1 / instrument.getSlitSize() * result.instrument().getPixelSize()) + " arcsec");
                    break;
                case POINT:
                    _println("software aperture extent along slit = "
                            + device.toString(1.4 * result.iqCalc().getImageQuality()) + " arcsec");
                    break;
            }
        }

        if (!result.parameters().source().isUniform()) {
            _println("fraction of source flux in aperture = " + device.toString(result.st().getSlitThroughput()));
        }

        _println("derived image size(FWHM) for a point source = " + device.toString(result.iqCalc().getImageQuality()) + " arcsec");

        _println("");
        _println("Requested total integration time = "
                + device.toString(result.parameters().observation().getExposureTime() * result.parameters().observation().getNumExposures())
                + " secs, of which "
                + device.toString(result.parameters().observation().getExposureTime() * result.parameters().observation().getNumExposures()
                * result.specS2N()[0].getSpecFracWithSource()) + " secs is on source.");

        _print("<HR align=left SIZE=3>");

        _println("<p style=\"page-break-inside: never\">");

        _printImageLink(id, SignalChart.instance(), pdp);
        _println("");

        _printFileLink(id, SignalData.instance());
        _printFileLink(id, BackgroundData.instance());

        _printImageLink(id, S2NChart.instance(), pdp);
        _println("");

        _printFileLink(id, SingleS2NData.instance());
        _printFileLink(id, FinalS2NData.instance());

        printConfiguration((Flamingos2) result.instrument(), result.parameters());

        _println(HtmlPrinter.printParameterSummary(pdp));

    }


    private void writeImagingOutput(final ImagingResult result) {

        // we know this is Flamingos
        final Flamingos2 instrument = (Flamingos2) result.instrument();

        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();
        _println("");

        _print(CalculatablePrinter.getTextResult(result.sfCalc(), device));
        _println(CalculatablePrinter.getTextResult(result.iqCalc(), device));
        _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation(), device));

        device.setPrecision(0); // NO decimal places
        device.clear();

        _println("");
        _println("The peak pixel signal + background is "
                + device.toString(result.peakPixelCount())
                + ". This is "
                + device.toString(result.peakPixelCount()
                / instrument.getWellDepth() * 100)
                + "% of the full well depth of "
                + device.toString(instrument.getWellDepth()) + ".");

        if (result.peakPixelCount() > (.8 * instrument.getWellDepth()))
            _println("Warning: peak pixel exceeds 80% of the well depth and may be saturated");

        _println("");
        device.setPrecision(2); // TWO decimal places
        device.clear();

        printConfiguration((Flamingos2) result.instrument(), result.parameters());
    }

    private void printConfiguration(final Flamingos2 instrument, final Parameters p) {
        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: Flamingos 2\n");
        _println(HtmlPrinter.printParameterSummary(p.source()));
        _println(flamingos2ToString(instrument));
        _println(HtmlPrinter.printParameterSummary(p.telescope()));
        _println(HtmlPrinter.printParameterSummary(p.conditions()));
        _println(HtmlPrinter.printParameterSummary(p.observation()));
    }

    private String flamingos2ToString(final Flamingos2 instrument) {
        String s = "Instrument configuration: \n";
        s += "Optical Components: <BR>";
        for (final TransmissionElement te : instrument.getComponents()) {
            s += "<LI>" + te.toString() + "<BR>";
        }
        s += "<LI>Read Noise: " + instrument.getReadNoiseString() + "\n";

        if (instrument.getFocalPlaneMask() != FPUnit.FPU_NONE)
            s += "<LI>Focal Plane Mask: " + instrument.getFocalPlaneMask().getSlitWidth() + " pix slit\n";

        s += "<BR>Pixel Size: " + instrument.getPixelSize() + "<BR>";

        return s;
    }


}
