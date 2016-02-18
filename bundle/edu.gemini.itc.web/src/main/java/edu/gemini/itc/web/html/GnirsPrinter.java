package edu.gemini.itc.web.html;

import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.gnirs.Gnirs;
import edu.gemini.itc.gnirs.GnirsRecipe;
import edu.gemini.itc.shared.*;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing GNIRS calculation results to an output stream.
 */
public final class GnirsPrinter extends PrinterBase {

    private final PlottingDetails pdp;
    private final GnirsRecipe recipe;

    public GnirsPrinter(final ItcParameters p, final GnirsParameters instr, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.pdp        = pdp;
        this.recipe     = new GnirsRecipe(p, instr);
    }

    public void writeOutput() {
        final SpectroscopyResult r = recipe.calculateSpectroscopy();
        final ItcSpectroscopyResult s = recipe.serviceResult(r);
        final UUID id = cache(s);
        writeSpectroscopyOutput(id, r, s);
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult result, final ItcSpectroscopyResult s) {
        _println("");

        final Gnirs instrument = (Gnirs) result.instrument();

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

        if (instrument.XDisp_IsUsed()) {

            _printImageLink(id, SignalChart.instance(), pdp);
            _println("");

            for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
                _printFileLink(id, SignalData.instance(), 0, i, "Order " + (i+3));
            }
            for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
                _printFileLink(id, BackgroundData.instance(), 0, i, "Order " + (i+3));
            }

            _printImageLink(id, S2NChart.instance(), pdp);
            _println("");


            for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
                _printFileLink(id, FinalS2NData.instance(), 0, i, "Order " + (i+3));
            }

        } else {

            _printImageLink(id, SignalChart.instance(), pdp);
            _println("");

            _printFileLink(id, SignalData.instance());
            _printFileLink(id, BackgroundData.instance());
            _printImageLink(id, S2NChart.instance(), pdp);
            _println("");

            _printFileLink(id, SingleS2NData.instance());
            _printFileLink(id, FinalS2NData.instance());
        }

        _println("");

        _print("<HR align=left SIZE=3>");

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.source()));
        _println(gnirsToString(instrument, result.parameters()));
        _println(HtmlPrinter.printParameterSummary(result.telescope()));
        _println(HtmlPrinter.printParameterSummary(result.conditions()));
        _println(HtmlPrinter.printParameterSummary(result.observation()));
        _println(HtmlPrinter.printParameterSummary(pdp));

    }

    private String gnirsToString(final Gnirs instrument, final ItcParameters p) {

        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);

        s += "<LI>Focal Plane Mask: " + instrument.getFocalPlaneMask().displayValue() + "\n";

        s += "<LI>Grating: " + instrument.getGrating().displayValue() + "\n"; // REL-469

        s += "<LI>Read Noise: " + instrument.getReadNoise() + "\n";
        s += "<LI>Well Depth: " + instrument.getWellDepth() + "\n";
        s += "\n";

        s += String.format("<L1> Central Wavelength: %.1f nm\n", instrument.getCentralWavelength());
        s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + " arcsec\n";
        if (p.observation().calculationMethod() instanceof Spectroscopy) {
            if (instrument.XDisp_IsUsed()) {
                s += String.format("Pixel Size in Spectral Direction(Order 3): %.3f nm\n", instrument.getGratingDispersion() / 3);
                s += String.format("Pixel Size in Spectral Direction(Order 4): %.3f nm\n", instrument.getGratingDispersion() / 4);
                s += String.format("Pixel Size in Spectral Direction(Order 5): %.3f nm\n", instrument.getGratingDispersion() / 5);
                s += String.format("Pixel Size in Spectral Direction(Order 6): %.3f nm\n", instrument.getGratingDispersion() / 6);
                s += String.format("Pixel Size in Spectral Direction(Order 7): %.3f nm\n", instrument.getGratingDispersion() / 7);
                s += String.format("Pixel Size in Spectral Direction(Order 8): %.3f nm\n", instrument.getGratingDispersion() / 8);
            } else {
                s += String.format("Pixel Size in Spectral Direction: %.3f nm\n", instrument.getGratingDispersion() / instrument.getOrder());
            }
        }
        return s;
    }

}
