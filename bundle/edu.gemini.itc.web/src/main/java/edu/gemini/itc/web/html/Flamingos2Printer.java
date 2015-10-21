package edu.gemini.itc.web.html;

import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.itc.flamingos2.Flamingos2;
import edu.gemini.itc.flamingos2.Flamingos2Recipe;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.FPUnit;
import edu.gemini.spModel.core.PointSource$;
import edu.gemini.spModel.core.UniformSource$;
import scala.Tuple2;
import scala.collection.JavaConversions;

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

        _println("");

        _print(CalculatablePrinter.getTextResult(result.sfCalc()));
        _println(CalculatablePrinter.getTextResult(result.iqCalc()));

        if (!result.parameters().observation().isAutoAperture()) {
            _println(String.format("software aperture extent along slit = %.2f arcsec", result.parameters().observation().getApertureDiameter()));
        } else {
            if (result.source().profile() == UniformSource$.MODULE$) {
                _println(String.format("software aperture extent along slit = %.2f arcsec", 1 / instrument.getSlitSize() * result.instrument().getPixelSize()));
            } else if (result.source().profile() == PointSource$.MODULE$) {
                _println(String.format("software aperture extent along slit = %.2f arcsec", 1.4 * result.iqCalc().getImageQuality()));
            }
        }

        if (!result.parameters().source().isUniform()) {
            _println(String.format("fraction of source flux in aperture = %.2f", result.st().getSlitThroughput()));
        }

        _println(String.format("derived image size(FWHM) for a point source = %.2f arcsec", result.iqCalc().getImageQuality()));

        _println("");
        final double totExpTime = result.parameters().observation().getExposureTime() * result.parameters().observation().getNumExposures();
        _println(String.format(
                "Requested total integration time = %.2f secs, of which %.2f secs is on source.",
                totExpTime, totExpTime * result.specS2N()[0].getSpecFracWithSource()));

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

        _println("");

        _print(CalculatablePrinter.getTextResult(result.sfCalc()));
        _println(CalculatablePrinter.getTextResult(result.iqCalc()));
        _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation()));

        _println("");
        _println(String.format(
                "The peak pixel signal + background is %.0f. This is %.0f%% of the full well depth of %.0f.",
                result.peakPixelCount(), result.peakPixelCount() / instrument.getWellDepth() * 100, instrument.getWellDepth()));

        for (final ItcWarning warning : JavaConversions.asJavaList(result.warnings())) {
            _println(warning.msg());
        }

        _println("");

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
