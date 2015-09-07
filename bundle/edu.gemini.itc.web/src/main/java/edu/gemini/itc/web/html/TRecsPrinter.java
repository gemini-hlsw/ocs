package edu.gemini.itc.web.html;

import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.trecs.TRecs;
import edu.gemini.itc.trecs.TRecsRecipe;
import edu.gemini.spModel.gemini.trecs.TReCSParams;
import scala.Tuple2;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing TRecs calculation results to an output stream.
 */
public final class TRecsPrinter extends PrinterBase {

    private final TRecsRecipe recipe;
    private final PlottingDetails pdp;
    private final boolean isImaging;

    public TRecsPrinter(final Parameters p, final TRecsParameters ip, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe    = new TRecsRecipe(p.source(), p.observation(), p.conditions(), ip, p.telescope());
        this.pdp       = pdp;
        this.isImaging = p.observation().getMethod().isImaging();
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

        final TRecs instrument = (TRecs) result.instrument();

        _println("");

        if (!result.observation().isAutoAperture()) {
            _println(String.format("software aperture extent along slit = %.2f arcsec", result.observation().getApertureDiameter()));
        } else {
            switch (result.source().getProfileType()) {
                case UNIFORM:
                    _println(String.format("software aperture extent along slit = %.2f arcsec", 1 / instrument.getFPMask()));
                    break;
                case POINT:
                    _println(String.format("software aperture extent along slit = %.2f arcsec", 1.4 * result.iqCalc().getImageQuality()));
                    break;
            }
        }

        if (!result.source().isUniform()) {
            _println(String.format("fraction of source flux in aperture = %.2f", result.st().getSlitThroughput()));
        }

        _println(String.format("derived image size(FWHM) for a point source = %.2f arcsec\n", result.iqCalc().getImageQuality()));

        _println("Sky subtraction aperture = " + result.observation().getSkyApertureDiameter() + " times the software aperture.");

        _println("");

        final double exp_time = result.observation().getExposureTime();
        final int number_exposures = result.observation().getNumExposures();
        final double frac_with_source = result.observation().getSourceFraction();

        _println(String.format("Requested total integration time = %.2f secs, of which %.2f secs is on source.", exp_time * number_exposures, exp_time * number_exposures * frac_with_source));

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

    private void writeImagingOutput(final ImagingResult result) {

        final TRecs instrument = (TRecs) result.instrument();

        _println("");

        _print(CalculatablePrinter.getTextResult(result.sfCalc()));
        _println(CalculatablePrinter.getTextResult(result.iqCalc()));
        _println("Sky subtraction aperture = "
                + result.observation().getSkyApertureDiameter()
                + " times the software aperture.\n");

        _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation()));

        _println("");
        _println(String.format("The peak pixel signal + background is %.0f.", result.peakPixelCount()));

        if (result.peakPixelCount() > (instrument.getWellDepth()))
            _println("Warning: peak pixel may be saturating the imaging deep well setting of "
                    + instrument.getWellDepth());

        _println("");

        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.source()));
        _println(trecsToString(instrument, result.parameters()));
        _println(HtmlPrinter.printParameterSummary(result.telescope()));
        _println(HtmlPrinter.printParameterSummary(result.conditions()));
        _println(HtmlPrinter.printParameterSummary(result.observation()));
    }

    private String trecsToString(final TRecs instrument, final Parameters p) {

        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);

        final TReCSParams.Mask mask = instrument.getFocalPlaneMask();
        if (!mask.equals(TReCSParams.Mask.MASK_IMAGING) && !mask.equals(TReCSParams.Mask.MASK_IMAGING_W))
            s += "<LI> Focal Plane Mask: " + instrument.getFocalPlaneMask().displayValue() + "\n";
        s += "\n";
        if (p.observation().getMethod().isSpectroscopy())
            s += String.format("<L1> Central Wavelength: %.1f nm\n", instrument.getCentralWavelength());
        s += "Spatial Binning: 1\n";
        if (p.observation().getMethod().isSpectroscopy())
            s += "Spectral Binning: 1\n";
        s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + " arcsec\n";
        if (p.observation().getMethod().isSpectroscopy())
            s += "Pixel Size in Spectral Direction: " + instrument.getGratingDispersion_nmppix() + " nm\n";
        return s;
    }

}
