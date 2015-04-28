package edu.gemini.itc.web.html;

import edu.gemini.itc.gnirs.Gnirs;
import edu.gemini.itc.gnirs.GnirsParameters;
import edu.gemini.itc.gnirs.GnirsRecipe;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.servlets.ImageServlet;
import org.jfree.chart.ChartColor;
import scala.Tuple2;

import java.awt.*;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.UUID;

/**
 * Helper class for printing GNIRS calculation results to an output stream.
 */
public final class GnirsPrinter extends PrinterBase {

    private static final Color[] ORDER_COLORS =
            new Color[] {ChartColor.DARK_RED,           ChartColor.DARK_BLUE,       ChartColor.DARK_GREEN,
                         ChartColor.DARK_MAGENTA,       ChartColor.black,           ChartColor.DARK_CYAN};
    private static final Color[] ORDER_BG_COLORS =
            new Color[] {ChartColor.VERY_LIGHT_RED,     ChartColor.VERY_LIGHT_BLUE, ChartColor.VERY_LIGHT_GREEN,
                         ChartColor.VERY_LIGHT_MAGENTA, ChartColor.lightGray,       ChartColor.VERY_LIGHT_CYAN};

    private final PlottingDetails pdp;
    private final GnirsRecipe recipe;

    public GnirsPrinter(final Parameters p, final GnirsParameters ip, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.pdp        = pdp;
        this.recipe     = new GnirsRecipe(p.source(), p.observation(), p.conditions(), ip, p.telescope());
    }

    public void writeOutput() {
        final Tuple2<UUID, SpectroscopyResult> result = cache(recipe.calculateSpectroscopy());
        writeSpectroscopyOutput(result._1(), (GnirsSpectroscopyResult) result._2());
        validatePlottingDetails(pdp, result._2().instrument());
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

            final ITCChart chart1 = new ITCChart("Signal and Background in software aperture of " + result.specS2N()[0].getSpecNpix() + " pixels", "Wavelength (nm)", "e- per exposure per spectral pixel", pdp);
            for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
                chart1.addArray(result.signalOrder()[i].getData(), "Signal Order "+(i+3), ORDER_COLORS[i]);
                chart1.addArray(result.backGroundOrder()[i].getData(), "SQRT(Background) Order "+(i+3), ORDER_BG_COLORS[i]);
            }
            _println(chart1.getBufferedImage(), "SigAndBack");
            _println("");

            _printFileLink(id, ImageServlet.GnirsSigOrder, "ASCII signal spectrum");
            _printFileLink(id, ImageServlet.GnirsBgOrder, "ASCII background spectrum");

            final ITCChart chart2 = new ITCChart("Final S/N", "Wavelength (nm)", "Signal / Noise per spectral pixel", pdp);
            for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
                chart2.addArray(result.finalS2NOrder()[i].getData(), "Final S/N Order "+(i+3), ORDER_COLORS[i]);
            }
            _println(chart2.getBufferedImage(), "Sig2N");
            _println("");

            _printFileLink(id, ImageServlet.GnirsFinalS2NOrder, "Final S/N ASCII data");

        } else {

            final ITCChart chart1 = new ITCChart("Signal and Background in software aperture of " + result.specS2N()[0].getSpecNpix() + " pixels", "Wavelength (nm)", "e- per exposure per spectral pixel", pdp);
            chart1.addArray(result.specS2N()[0].getSignalSpectrum().getData(), "Signal ");
            chart1.addArray(result.specS2N()[0].getBackgroundSpectrum().getData(), "SQRT(Background)  ");
            _println(chart1.getBufferedImage(), "SigAndBack");
            _println("");

            _printFileLink(id, ImageServlet.SigSpec, "ASCII signal spectrum");
            _printFileLink(id, ImageServlet.BackSpec, "ASCII background spectrum");

            final ITCChart chart2 = new ITCChart("Intermediate Single Exp and Final S/N", "Wavelength (nm)", "Signal / Noise per spectral pixel", pdp);
            chart2.addArray(result.specS2N()[0].getExpS2NSpectrum().getData(), "Single Exp S/N");
            chart2.addArray(result.specS2N()[0].getFinalS2NSpectrum().getData(), "Final S/N  ");
            _println(chart2.getBufferedImage(), "Sig2N");
            _println("");

            _printFileLink(id, ImageServlet.SingleS2N, "Single Exposure S/N ASCII data");
            _printFileLink(id, ImageServlet.FinalS2N, "Final S/N ASCII data");
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

        if (!instrument.getFocalPlaneMask().equals(GnirsParameters.NO_SLIT))
            s += "<LI>Focal Plane Mask: " + instrument.getFocalPlaneMask() + "\n";

        s += "<LI>Grating: " + instrument.getGrating() + "\n"; // REL-469

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
