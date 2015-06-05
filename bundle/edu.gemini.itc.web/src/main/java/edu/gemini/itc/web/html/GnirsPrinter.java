package edu.gemini.itc.web.html;

import edu.gemini.itc.base.GnirsSpectroscopyResult;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.gnirs.Gnirs;
import edu.gemini.itc.gnirs.GnirsParameters;
import edu.gemini.itc.gnirs.GnirsRecipe;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
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

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        if (!result.observation().isAutoAperture()) {
            _println("software aperture extent along slit = "
                    + device.toString(result.observation()
                    .getApertureDiameter()) + " arcsec");
        } else {
            switch (result.source().getProfileType()) {
                case UNIFORM:
                    _println("software aperture extent along slit = "
                            + device.toString(1 / instrument
                            .getFPMask()) + " arcsec");
                    break;
                case POINT:
                    _println("software aperture extent along slit = "
                            + device.toString(1.4 * result.iqCalc().getImageQuality()) + " arcsec");
                    break;
            }
        }

        if (!result.source().isUniform()) {
            _println("fraction of source flux in aperture = "
                    + device.toString(result.st().getSlitThroughput()));
        }

        _println("derived image size(FWHM) for a point source = "
                + device.toString(result.iqCalc().getImageQuality()) + "arcsec\n");

        _println("Sky subtraction aperture = "
                + result.observation().getSkyApertureDiameter()
                + " times the software aperture.");

        _println("");
        _println("Requested total integration time = "
                + device.toString(result.observation().getExposureTime() * result.observation().getNumExposures())
                + " secs, of which "
                + device.toString(result.observation().getExposureTime() * result.observation().getNumExposures()
                * result.observation().getSourceFraction()) + " secs is on source.");

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
        device.setPrecision(2); // TWO decimal places
        device.clear();

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
        //Used to format the strings
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(3);  // Two decimal places
        device.clear();


        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);

        s += "<LI>Focal Plane Mask: " + instrument.getFocalPlaneMask().displayValue() + "\n";

        s += "<LI>Grating: " + instrument.getGrating().displayValue() + "\n"; // REL-469

        s += "<LI>Read Noise: " + instrument.getReadNoise() + "\n";
        s += "<LI>Well Depth: " + instrument.getWellDepth() + "\n";
        s += "\n";

        s += "<L1> Central Wavelength: " + instrument.getCentralWavelength() + " nm" + " \n";
        s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + "arcsec\n";
        if (p.observation().getMethod().isSpectroscopy()) {
            if (instrument.XDisp_IsUsed()) {
                s += "Pixel Size in Spectral Direction(Order 3): " + device.toString(instrument.getGratingDispersion_nmppix() / 3) + "nm\n";
                s += "Pixel Size in Spectral Direction(Order 4): " + device.toString(instrument.getGratingDispersion_nmppix() / 4) + "nm\n";
                s += "Pixel Size in Spectral Direction(Order 5): " + device.toString(instrument.getGratingDispersion_nmppix() / 5) + "nm\n";
                s += "Pixel Size in Spectral Direction(Order 6): " + device.toString(instrument.getGratingDispersion_nmppix() / 6) + "nm\n";
                s += "Pixel Size in Spectral Direction(Order 7): " + device.toString(instrument.getGratingDispersion_nmppix() / 7) + "nm\n";
                s += "Pixel Size in Spectral Direction(Order 8): " + device.toString(instrument.getGratingDispersion_nmppix() / 8) + "nm\n";
            } else {
                s += "Pixel Size in Spectral Direction: " + device.toString(instrument.getGratingDispersion_nmppix()) + "nm\n";
            }
        }
        return s;
    }

}
