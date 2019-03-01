package edu.gemini.itc.web.html;

import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.trecs.TRecs;
import edu.gemini.itc.trecs.TRecsRecipe;
import edu.gemini.spModel.gemini.trecs.TReCSParams;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing TRecs calculation results to an output stream.
 */
public final class TRecsPrinter extends PrinterBase {

    private final TRecsRecipe recipe;
    private final PlottingDetails pdp;
    private final boolean isImaging;

    public TRecsPrinter(final ItcParameters p, final TRecsParameters instr, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe    = new TRecsRecipe(p, instr);
        this.pdp       = pdp;
        this.isImaging = p.observation().calculationMethod() instanceof Imaging;
    }

    public void writeOutput() {
        if (isImaging) {
            final ImagingResult r = recipe.calculateImaging();
            final ItcImagingResult s = recipe.serviceResult(r);
            writeImagingOutput(r, s);
        } else {
            final SpectroscopyResult r = recipe.calculateSpectroscopy();
            final ItcSpectroscopyResult s = recipe.serviceResult(r, false);
            final UUID id = cache(s);
            writeSpectroscopyOutput(id, r, s);
        }
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult result, final ItcSpectroscopyResult s) {

        final TRecs instrument = (TRecs) result.instrument();

        _println("");

        _printSoftwareAperture(result, 1 / instrument.getSlitWidth());

        _println(String.format("derived image size(FWHM) for a point source = %.2f arcsec\n", result.iqCalc().getImageQuality()));

        _printSkyAperture(result);

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

        _println("");

        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.source()));
        _println(trecsToString(instrument, result.parameters()));
        _println(HtmlPrinter.printParameterSummary(result.telescope()));
        _println(HtmlPrinter.printParameterSummary(result.conditions()));
        _println(HtmlPrinter.printParameterSummary(result.observation()));
        _println(HtmlPrinter.printParameterSummary(pdp));

    }

    private void writeImagingOutput(final ImagingResult result, final ItcImagingResult s) {

        final TRecs instrument = (TRecs) result.instrument();

        _println("");

        _print(CalculatablePrinter.getTextResult(result.sfCalc()));
        _println(CalculatablePrinter.getTextResult(result.iqCalc()));

        _printSkyAperture(result);

        _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation()));

        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.source()));
        _println(trecsToString(instrument, result.parameters()));
        _println(HtmlPrinter.printParameterSummary(result.telescope()));
        _println(HtmlPrinter.printParameterSummary(result.conditions()));
        _println(HtmlPrinter.printParameterSummary(result.observation()));
    }

    private String trecsToString(final TRecs instrument, final ItcParameters p) {

        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);

        final TReCSParams.Mask mask = instrument.getFocalPlaneMask();
        if (!mask.equals(TReCSParams.Mask.MASK_IMAGING) && !mask.equals(TReCSParams.Mask.MASK_IMAGING_W))
            s += "<LI> Focal Plane Mask: " + instrument.getFocalPlaneMask().displayValue() + "\n";
        s += "\n";
        if (p.observation().calculationMethod() instanceof Spectroscopy)
            s += String.format("<L1> Central Wavelength: %.1f nm\n", instrument.getCentralWavelength());
        s += "Spatial Binning: 1\n";
        if (p.observation().calculationMethod() instanceof Spectroscopy)
            s += "Spectral Binning: 1\n";
        s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + " arcsec\n";
        if (p.observation().calculationMethod() instanceof Spectroscopy)
            s += "Pixel Size in Spectral Direction: " + instrument.getGratingDispersion() + " nm\n";
        return s;
    }

}
