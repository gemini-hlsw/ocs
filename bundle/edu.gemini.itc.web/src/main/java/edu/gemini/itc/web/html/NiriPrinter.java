package edu.gemini.itc.web.html;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.AOSystem;
import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.itc.niri.Niri;
import edu.gemini.itc.niri.NiriRecipe;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.niri.Niri.Mask;
import scala.Option;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing NIRI calculation results to an output stream.
 */
public final class NiriPrinter extends PrinterBase {

    private final NiriParameters instr;
    private final PlottingDetails pdp;
    private final NiriRecipe recipe;
    private final boolean isImaging;

    /**
     * Constructs a NiriRecipe given the parameters. Useful for testing.
     */
    public NiriPrinter(final ItcParameters p, final NiriParameters instr, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.instr     = instr;
        this.recipe    = new NiriRecipe(p, instr);
        this.isImaging = p.observation().calculationMethod() instanceof Imaging;
        this.pdp       = pdp;
    }

    public void writeOutput() {
        if (isImaging) {
            final ImagingResult result = recipe.calculateImaging();
            final ItcImagingResult s = recipe.serviceResult(result);
            writeImagingOutput(result, s);
        } else {
            final SpectroscopyResult r = recipe.calculateSpectroscopy();
            final ItcSpectroscopyResult s = recipe.serviceResult(r);
            final UUID id = cache(s);
            writeSpectroscopyOutput(id, r, s);
        }
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult result, final ItcSpectroscopyResult s) {

        final Niri instrument = (Niri) result.instrument();

        _println("");

        // Altair specific section
        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printSummary((Altair) result.aoSystem().get()));
        }

        _printSoftwareAperture(result, 1 / instrument.getFPMask());

        _println(String.format("derived image size(FWHM) for a point source = %.2f arcsec", result.specS2N()[0].getImageQuality()));

        _println("");

        _printRequestedIntegrationTime(result);

        _println("");
        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

        _print("<HR align=left SIZE=3>");

        _println("<p style=\"page-break-inside: never\">");

        _printImageLink(id, SignalChart.instance(), pdp);
        _println("");

        _printFileLink(id,  SignalData.instance());
        _printFileLink(id,  BackgroundData.instance());

        _printImageLink(id, S2NChart.instance(), pdp);
        _println("");

        _printFileLink(id,  SingleS2NData.instance());
        _printFileLink(id,  FinalS2NData.instance());

        printConfiguration(result.parameters(), instrument, result.aoSystem());

        _println(HtmlPrinter.printParameterSummary(pdp));

    }

    private void writeImagingOutput(final ImagingResult result, final ItcImagingResult s) {

        final Niri instrument = (Niri) result.instrument();

        _println("");

        // Altair specific section
        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printSummary((Altair) result.aoSystem().get()));
            _print(CalculatablePrinter.getTextResult(result.sfCalc(), false));
            _println(String.format("derived image halo size (FWHM) for a point source = %.2f arcsec.\n", result.iqCalc().getImageQuality()));
        } else {
            _print(CalculatablePrinter.getTextResult(result.sfCalc()));
            _println(CalculatablePrinter.getTextResult(result.iqCalc()));
        }

        _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation()));
        _println(CalculatablePrinter.getBackgroundLimitResult(result.is2nCalc()));

        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

        printConfiguration(result.parameters(), instrument, result.aoSystem());

    }

    private void printConfiguration(final ItcParameters p, final Niri instrument, final Option<AOSystem> ao) {
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
        if (instr.mask() != Mask.MASK_IMAGING)
            s += "<LI>Focal Plane Mask: " + instr.mask().displayValue() + "\n";
        s += "<LI>Read Mode: " + instr.readMode().displayValue() + "\n";
        s += "<LI>Detector Bias: " + instr.wellDepth().displayValue() + "\n";

        s += "<BR>Pixel Size: " + instrument.getPixelSize() + "<BR>";

        return s;
    }


}
