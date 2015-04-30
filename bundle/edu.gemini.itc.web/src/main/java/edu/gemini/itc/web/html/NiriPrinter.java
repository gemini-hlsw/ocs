package edu.gemini.itc.web.html;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.niri.Niri;
import edu.gemini.itc.niri.NiriRecipe;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.servlets.ImageServlet;
import edu.gemini.spModel.gemini.niri.Niri.Mask;
import scala.Option;
import scala.Tuple2;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing NIRI calculation results to an output stream.
 */
public final class NiriPrinter extends PrinterBase {

    private final PlottingDetails pdp;
    private final NiriRecipe recipe;
    private final boolean isImaging;

    /**
     * Constructs a NiriRecipe given the parameters. Useful for testing.
     */
    public NiriPrinter(final Parameters p, final NiriParameters ip, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe    = new NiriRecipe(p.source(), p.observation(), p.conditions(), ip, p.telescope());
        this.isImaging = p.observation().getMethod().isImaging();
        this.pdp       = pdp;
    }

    public void writeOutput() {
        if (isImaging) {
            final ImagingResult result = recipe.calculateImaging();
            writeImagingOutput(result);
        } else {
            final Tuple2<ItcSpectroscopyResult, SpectroscopyResult> r = recipe.calculateSpectroscopy();
            final UUID id = cache(r._1());
            writeSpectroscopyOutput(id, r._2());
        }
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult result) {

        final Niri instrument = (Niri) result.instrument();

        _println("");

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        // Altair specific section
        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printSummary((Altair) result.aoSystem().get()));
        }

        if (!result.observation().isAutoAperture()) {
            _println("software aperture extent along slit = "
                    + device.toString(result.observation().getApertureDiameter()) + " arcsec");
        } else {
            switch (result.source().getProfileType()) {
                case UNIFORM:
                    _println("software aperture extent along slit = " + device.toString(1 / instrument.getFPMask()) + " arcsec");
                    break;
                case POINT:
                    _println("software aperture extent along slit = " + device.toString(1.4 * result.specS2N()[0].getImageQuality()) + " arcsec");
                    break;
            }
        }

        if (!result.source().isUniform()) {
            _println("fraction of source flux in aperture = "
                    + device.toString(result.st().getSlitThroughput()));
        }

        _println("derived image size(FWHM) for a point source = "
                + device.toString(result.specS2N()[0].getImageQuality()) + " arcsec");

        _println("");
        _println("Requested total integration time = "
                + device.toString(result.observation().getExposureTime() * result.observation().getNumExposures())
                + " secs, of which "
                + device.toString(result.observation().getExposureTime() * result.observation().getNumExposures()
                * result.observation().getSourceFraction()) + " secs is on source.");

        _print("<HR align=left SIZE=3>");

        _println("<p style=\"page-break-inside: never\">");

        _printImageLink(id, ImageServlet.SigSwApChart);
        _println("");

        _printFileLink(id, ImageServlet.SigSpec, 0, "ASCII signal spectrum");
        _printFileLink(id, ImageServlet.BackSpec, 1, "ASCII background spectrum");

        _printImageLink(id, ImageServlet.S2NChart);
        _println("");

        _printFileLink(id, ImageServlet.SingleS2N, 2, "Single Exposure S/N ASCII data");
        _printFileLink(id, ImageServlet.FinalS2N, 3, "Final S/N ASCII data");

        printConfiguration(result.parameters(), instrument, result.aoSystem());

        _println(HtmlPrinter.printParameterSummary(pdp));

    }

    private void writeImagingOutput(final ImagingResult result) {

        final Niri instrument = (Niri) result.instrument();

        _println("");

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        // Altair specific section
        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printSummary((Altair) result.aoSystem().get()));
            _print(CalculatablePrinter.getTextResult(result.sfCalc(), device, false));
            _println("derived image halo size (FWHM) for a point source = "
                    + device.toString(result.iqCalc().getImageQuality()) + " arcsec.\n");
        } else {
            _print(CalculatablePrinter.getTextResult(result.sfCalc(), device));
            _println(CalculatablePrinter.getTextResult(result.iqCalc(), device));
        }

        _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation(), device));
        _println(CalculatablePrinter.getBackgroundLimitResult(result.is2nCalc()));
        device.setPrecision(0); // NO decimal places
        device.clear();

        _println("");
        _println("The peak pixel signal + background is "
                + device.toString(result.peakPixelCount())
                + ". This is "
                + device.toString(result.peakPixelCount()
                / instrument.getWellDepthValue() * 100)
                + "% of the full well depth of "
                + device.toString(instrument.getWellDepthValue()) + ".");

        if (result.peakPixelCount() > (.8 * instrument.getWellDepthValue()))
            _println("Warning: peak pixel exceeds 80% of the well depth and may be saturated");

        _println("");

        printConfiguration(result.parameters(), instrument, result.aoSystem());

    }

    private void printConfiguration(final Parameters p, final Niri instrument, final Option<AOSystem> ao) {
        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(p.source()));
        _println(niriToString(instrument));
        if (ao.isDefined()) {
            _println(HtmlPrinter.printParameterSummary(p.telescope(), "altair"));
            _println(HtmlPrinter.printParameterSummary((Altair) ao.get()));
        } else {
            _println(HtmlPrinter.printParameterSummary(p.telescope()));
        }

        _println(HtmlPrinter.printParameterSummary(p.conditions()));
        _println(HtmlPrinter.printParameterSummary(p.observation()));
    }

    private String niriToString(final Niri instrument) {
        String s = "Instrument configuration: \n";
        s += "Optical Components: <BR>";
        for (final TransmissionElement te : instrument.getComponents()) {
            s += "<LI>" + te.toString() + "<BR>";
        }
        if (instrument.getFocalPlaneMask() != Mask.MASK_IMAGING)
            s += "<LI>Focal Plane Mask: " + instrument.getFocalPlaneMask().displayValue() + "\n";
        s += "<LI>Read Mode: " + instrument.getReadMode().displayValue() + "\n";
        s += "<LI>Detector Bias: " + instrument.getWellDepth().displayValue() + "\n";

        s += "<BR>Pixel Size: " + instrument.getPixelSize() + "<BR>";

        return s;
    }


}
