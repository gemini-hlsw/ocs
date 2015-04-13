package edu.gemini.itc.web.html;

import edu.gemini.itc.flamingos2.Flamingos2;
import edu.gemini.itc.flamingos2.Flamingos2Parameters;
import edu.gemini.itc.flamingos2.Flamingos2Recipe;
import edu.gemini.itc.flamingos2.GrismOptics;
import edu.gemini.itc.shared.*;

import java.io.PrintWriter;
import java.util.Calendar;

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
            final SpectroscopyResult result = recipe.calculateSpectroscopy();
            writeSpectroscopyOutput(result);
            validatePlottingDetails(pdp, result.instrument());
        }
    }

    private void writeSpectroscopyOutput(final SpectroscopyResult result) {

        // we know this is Flamingos
        final Flamingos2 instrument = (Flamingos2) result.instrument();

        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();
        _println("");

        _print(CalculatablePrinter.getTextResult(result.sfCalc(), device));
        _println(CalculatablePrinter.getTextResult(result.iqCalc(), device));

        if (!result.parameters().observation().isAutoAperture()) {
            _println("software aperture extent along slit = " + device.toString(result.parameters().observation().getApertureDiameter()) + " arcsec");
        } else {
            switch (result.parameters().source().getProfileType()) {
                case UNIFORM:
                    _println("software aperture extent along slit = "
                            + device.toString(1 / instrument.getSlitSize() * result.instrument().getPixelSize()) + " arcsec");
                    break;
                case POINT:
                    _println("software aperture extent along slit = "
                            + device.toString(1.4 * result.iqCalc().getImageQuality()) + " arcsec");
                    break;
            }
        }

        if (!result.parameters().source().isUniform()) {
            _println("fraction of source flux in aperture = " + device.toString(result.st().getSlitThroughput()));
        }

        _println("derived image size(FWHM) for a point source = " + device.toString(result.iqCalc().getImageQuality()) + " arcsec");

        _println("");
        _println("Requested total integration time = "
                + device.toString(result.parameters().observation().getExposureTime() * result.parameters().observation().getNumExposures())
                + " secs, of which "
                + device.toString(result.parameters().observation().getExposureTime() * result.parameters().observation().getNumExposures()
                * result.specS2N()[0].getSpecFracWithSource()) + " secs is on source.");

        _print("<HR align=left SIZE=3>");

        final ITCChart chart1 = new ITCChart(
                "Signal and SQRT(Background) in software aperture of " + result.specS2N()[0].getSpecNpix() + " pixels",
                "Wavelength (nm)", "e- per exposure per spectral pixel", pdp);
        final ITCChart chart2 = new ITCChart(
                "Intermediate Single Exp and Final S/N",
                "Wavelength (nm)", "Signal / Noise per spectral pixel", pdp);

        _println("<p style=\"page-break-inside: never\">");
        chart1.addArray(result.specS2N()[0].getSignalSpectrum().getData(), "Signal ");
        chart1.addArray(result.specS2N()[0].getBackgroundSpectrum().getData(), "SQRT(Background)  ");

        _println(chart1.getBufferedImage(), "SigAndBack");
        _println("");

        final String sigSpec = _printSpecTag("ASCII signal spectrum");
        final String backSpec = _printSpecTag("ASCII background spectrum");

        chart2.addArray(result.specS2N()[0].getExpS2NSpectrum().getData(), "Single Exp S/N");
        chart2.addArray(result.specS2N()[0].getFinalS2NSpectrum().getData(), "Final S/N  ");

        _println(chart2.getBufferedImage(), "Sig2N");
        _println("");

        final String singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
        final String finalS2N = _printSpecTag("Final S/N ASCII data");

        final String header = "# Flamingos-2 ITC: " + Calendar.getInstance().getTime() + "\n";
        _println(result.specS2N()[0].getSignalSpectrum(), header, sigSpec);
        _println(result.specS2N()[0].getBackgroundSpectrum(), header, backSpec);
        _println(result.specS2N()[0].getExpS2NSpectrum(), header, singleS2N);
        _println(result.specS2N()[0].getFinalS2NSpectrum(), header, finalS2N);

        printConfiguration((Flamingos2) result.instrument(), result.parameters());

        _println(HtmlPrinter.printParameterSummary(pdp));

    }


    private void writeImagingOutput(final ImagingResult result) {

        // we know this is Flamingos
        final Flamingos2 instrument = (Flamingos2) result.instrument();

        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();
        _println("");

        _print(CalculatablePrinter.getTextResult(result.sfCalc(), device));
        _println(CalculatablePrinter.getTextResult(result.iqCalc(), device));
        _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation(), device));

        device.setPrecision(0); // NO decimal places
        device.clear();

        _println("");
        _println("The peak pixel signal + background is "
                + device.toString(result.peakPixelCount())
                + ". This is "
                + device.toString(result.peakPixelCount()
                / instrument.getWellDepth() * 100)
                + "% of the full well depth of "
                + device.toString(instrument.getWellDepth()) + ".");

        if (result.peakPixelCount() > (.8 * instrument.getWellDepth()))
            _println("Warning: peak pixel exceeds 80% of the well depth and may be saturated");

        _println("");
        device.setPrecision(2); // TWO decimal places
        device.clear();

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

        if (instrument.getFocalPlaneMask() != edu.gemini.spModel.gemini.flamingos2.Flamingos2.FPUnit.FPU_NONE)
            s += "<LI>Focal Plane Mask: " + instrument.getFocalPlaneMask().getSlitWidth() + " pix slit\n";

        s += "<BR>Pixel Size: " + instrument.getPixelSize() + "<BR>";

        return s;
    }


}
