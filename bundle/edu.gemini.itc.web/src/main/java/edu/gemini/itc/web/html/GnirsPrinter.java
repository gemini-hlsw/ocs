package edu.gemini.itc.web.html;

import edu.gemini.itc.base.GnirsSpectroscopyResult;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.gnirs.Gnirs;
import edu.gemini.itc.gnirs.GnirsRecipe;
import edu.gemini.itc.shared.*;
import scala.Tuple2;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing GNIRS calculation results to an output stream.
 */
public final class GnirsPrinter extends PrinterBase {

    private final PlottingDetails pdp;
    private final GnirsRecipe recipe;

    public GnirsPrinter(final Parameters p, final GnirsParameters ip, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.pdp        = pdp;
        this.recipe     = new GnirsRecipe(p.source(), p.observation(), p.conditions(), ip, p.telescope());
    }

    public void writeOutput() {
        final Tuple2<ItcSpectroscopyResult, SpectroscopyResult> r = recipe.calculateSpectroscopy();
        final UUID id = cache(r._1());
        writeSpectroscopyOutput(id, (GnirsSpectroscopyResult) r._2());
    }

    private void writeSpectroscopyOutput(final UUID id, final GnirsSpectroscopyResult result) {
        _println("");

        final Gnirs instrument = (Gnirs) result.instrument();

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

        _println("Sky subtraction aperture = "
                + result.observation().getSkyApertureDiameter()
                + " times the software aperture.");

        _println("");
        _println(String.format("Requested total integration time = %.2f secs, of which %.2f secs is on source.",
                result.observation().getExposureTime() * result.observation().getNumExposures(),
                result.observation().getExposureTime() * result.observation().getNumExposures() * result.observation().getSourceFraction()));

        _print("<HR align=left SIZE=3>");

        _println("<p style=\"page-break-inside: never\">");

        if (instrument.XDisp_IsUsed()) {

            _printImageLink(id, SignalChart.instance(), pdp);
            _println("");

            _printFileLink(id,  SignalData.instance());
            _printFileLink(id,  BackgroundData.instance());
            _printImageLink(id, S2NChart.instance(), pdp);
            _println("");

            _printFileLink(id,  FinalS2NData.instance());

        } else {

            _printImageLink(id, SignalChart.instance(), pdp);
            _println("");

            _printFileLink(id,  SignalData.instance());
            _printFileLink(id,  BackgroundData.instance());
            _printImageLink(id, S2NChart.instance(), pdp);
            _println("");

            _printFileLink(id,  SingleS2NData.instance());
            _printFileLink(id,  FinalS2NData.instance());
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

    private String gnirsToString(final Gnirs instrument, final Parameters p) {

        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);

        s += "<LI>Focal Plane Mask: " + instrument.getFocalPlaneMask().displayValue() + "\n";

        s += "<LI>Grating: " + instrument.getGrating().displayValue() + "\n"; // REL-469

        s += "<LI>Read Noise: " + instrument.getReadNoise() + "\n";
        s += "<LI>Well Depth: " + instrument.getWellDepth() + "\n";
        s += "\n";

        s += "<L1> Central Wavelength: " + instrument.getCentralWavelength() + " nm" + " \n";
        s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + " arcsec\n";
        if (p.observation().getMethod().isSpectroscopy()) {
            if (instrument.XDisp_IsUsed()) {
                s += String.format("Pixel Size in Spectral Direction(Order 3): %.3f nm\n", instrument.getGratingDispersion_nmppix() / 3);
                s += String.format("Pixel Size in Spectral Direction(Order 4): %.3f nm\n", instrument.getGratingDispersion_nmppix() / 4);
                s += String.format("Pixel Size in Spectral Direction(Order 5): %.3f nm\n", instrument.getGratingDispersion_nmppix() / 5);
                s += String.format("Pixel Size in Spectral Direction(Order 6): %.3f nm\n", instrument.getGratingDispersion_nmppix() / 6);
                s += String.format("Pixel Size in Spectral Direction(Order 7): %.3f nm\n", instrument.getGratingDispersion_nmppix() / 7);
                s += String.format("Pixel Size in Spectral Direction(Order 8): %.3f nm\n", instrument.getGratingDispersion_nmppix() / 8);
            } else {
                s += String.format("Pixel Size in Spectral Direction: %.3f nm\n", instrument.getGratingDispersion_nmppix());
            }
        }
        return s;
    }

}
