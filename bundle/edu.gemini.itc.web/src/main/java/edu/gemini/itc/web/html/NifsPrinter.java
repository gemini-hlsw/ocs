package edu.gemini.itc.web.html;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.nifs.IFUComponent;
import edu.gemini.itc.nifs.Nifs;
import edu.gemini.itc.nifs.NifsRecipe;
import edu.gemini.itc.shared.*;
import scala.Tuple2;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing NIFS calculation results to an output stream.
 */
public final class NifsPrinter extends PrinterBase {

    private final NifsRecipe recipe;
    private final PlottingDetails pdp;

    public NifsPrinter(final Parameters p, final NifsParameters ip, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe = new NifsRecipe(p.source(), p.observation(), p.conditions(), ip, p.telescope());
        this.pdp    = pdp;
    }

    public void writeOutput() {
        final Tuple2<ItcSpectroscopyResult, SpectroscopyResult> r = recipe.calculateSpectroscopy();
        final UUID id = cache(r._1());
        writeSpectroscopyOutput(id, r._2());
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult result) {

        final Nifs instrument = (Nifs) result.instrument();

        _println("");

        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printSummary((Altair) result.aoSystem().get()));
        }

        final int number_exposures = result.observation().getNumExposures();
        final double frac_with_source = result.observation().getSourceFraction();
        final double exposure_time = result.observation().getExposureTime();

        _println(String.format("derived image halo size (FWHM) for a point source = %.2f arcsec\n", result.iqCalc().getImageQuality()));
        _println(String.format("Requested total integration time = %.2f secs, of which %.2f secs is on source.", exposure_time * number_exposures, exposure_time * number_exposures * frac_with_source));

        _print("<HR align=left SIZE=3>");

        for (int i = 0; i < result.specS2N().length; i++) {
            _println("<p style=\"page-break-inside: never\">");
            _printImageLink(id, SignalChart.instance(),   i, pdp);
            _println("");
            _printFileLink(id, SignalData.instance(),     i);
            _printFileLink(id, BackgroundData.instance(), i);
            _printImageLink(id, S2NChart.instance(),      i, pdp);
            _println("");
            _printFileLink(id, SingleS2NData.instance(),  i);
            _printFileLink(id, FinalS2NData.instance(),   i);
        }

        _println("");

        _print("<HR align=left SIZE=3>");

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.source()));
        _println(nifsToString(instrument));
        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printParameterSummary((Altair) result.aoSystem().get()));
        }
        _println(HtmlPrinter.printParameterSummary(result.telescope()));
        _println(HtmlPrinter.printParameterSummary(result.conditions()));
        _println(HtmlPrinter.printParameterSummary(result.observation()));
        _println(HtmlPrinter.printParameterSummary(pdp));

    }

    private String nifsToString(final Nifs instrument) {
        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);
        s += "<LI>Focal Plane Mask: ifu\n";
        s += "<LI>Read Noise: " + instrument.getReadNoise() + "\n";
        s += "<LI>Well Depth: 90000.0\n";
        s += "\n";

        s += String.format("<L1> Central Wavelength: %.1f nm\n", instrument.getCentralWavelength());
        s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + " arcsec\n";

        s += String.format("Pixel Size in Spectral Direction: %.3f nm\n", instrument.getGratingDispersion_nmppix());

        s += "IFU is selected,";
        if (instrument.getIFUMethod() instanceof IfuSingle)
            s += "with a single IFU element at " + instrument.getIFUOffset() + " arcsecs.";
        else if (instrument.getIFUMethod() instanceof IfuSummed)
            s += String.format("with multiple summed IFU elements arranged in a " + instrument.getIFUNumX() + "x" + instrument.getIFUNumY() +
                    " (%.3f\"x%.3f\") grid.", instrument.getIFUNumX() * IFUComponent.IFU_LEN_X, instrument.getIFUNumY() * IFUComponent.IFU_LEN_Y);
        else
            s += "with mulitple IFU elements arranged from " + instrument.getIFUMinOffset() + " to " + instrument.getIFUMaxOffset() + " arcsecs.";
        s += "\n";

        return s;
    }

}
