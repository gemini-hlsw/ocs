package edu.gemini.itc.web.html;

import edu.gemini.itc.michelle.Michelle;
import edu.gemini.itc.michelle.MichelleParameters;
import edu.gemini.itc.michelle.MichelleRecipe;
import edu.gemini.itc.shared.*;

import java.io.PrintWriter;
import java.util.Calendar;

/**
 * Helper class for printing Michelle calculation results to an output stream.
 */
public final class MichellePrinter extends PrinterBase {

    private final MichelleRecipe recipe;
    private final PlottingDetails pdp;
    private final boolean isImaging;

    public MichellePrinter(final Parameters p, final MichelleParameters ip, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe    = new MichelleRecipe(p.source(), p.observation(), p.conditions(), ip, p.telescope());
        this.pdp       = pdp;
        this.isImaging = p.observation().getMethod().isImaging();
    }


    public void writeOutput() {
        if (isImaging) {
            final ImagingResult result = recipe.calculateImaging();
            writeImagingOutput(result);
        } else {
            final SpectroscopyResult result = recipe.calculateSpectroscopy();
            writeSpectroscopyOutput(result);
        }
    }

    private void writeSpectroscopyOutput(final SpectroscopyResult result) {

        final Michelle instrument = (Michelle) result.instrument();

        _println("");

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();

        if (!result.observation().isAutoAperture()) {
            _println("software aperture extent along slit = " + device.toString(result.observation().getApertureDiameter()) + " arcsec");
        } else {
            switch (result.source().getProfileType()) {
                case UNIFORM:
                    _println("software aperture extent along slit = " + device.toString(1 / instrument.getFPMask()) + " arcsec");
                    break;
                case POINT:
                    _println("software aperture extent along slit = " + device.toString(1.4 * result.iqCalc().getImageQuality()) + " arcsec");
                    break;
            }
        }

        if (!result.source().isUniform()) {
            _println("fraction of source flux in aperture = " + device.toString(result.st().getSlitThroughput()));
        }

        _println("derived image size(FWHM) for a point source = " + device.toString(result.iqCalc().getImageQuality()) + "arcsec\n");

        _println("Sky subtraction aperture = " + result.observation().getSkyApertureDiameter() + " times the software aperture.");

        _println("");

        final int number_exposures = result.observation().getNumExposures();
        final double frac_with_source = result.observation().getSourceFraction();
        final double exposure_time = result.observation().getExposureTime();
        if (instrument.polarimetryIsUsed()) {
            //Michelle polarimetry uses 4 waveplate positions so a single observation takes 4 times as long.
            //To the user it should appear as though the time used by the ITC matches thier requested time.
            //hence the x4 factor
            _println("Requested total integration time = " +
                    device.toString(exposure_time * 4 * number_exposures) +
                    " secs, of which " + device.toString(exposure_time * 4 *
                    number_exposures *
                    frac_with_source) +
                    " secs is on source.");
        } else {
            _println("Requested total integration time = " +
                    device.toString(exposure_time * number_exposures) +
                    " secs, of which " + device.toString(exposure_time *
                    number_exposures *
                    frac_with_source) +
                    " secs is on source.");
        }

        _print("<HR align=left SIZE=3>");

        _println("<p style=\"page-break-inside: never\">");

        final ITCChart chart1 = new ITCChart("Signal and Background ", "Wavelength (nm)", "e- per exposure per spectral pixel", pdp);
        chart1.addArray(result.specS2N()[0].getSignalSpectrum().getData(), "Signal ");
        chart1.addArray(result.specS2N()[0].getBackgroundSpectrum().getData(), "SQRT(Background)  ");
        _println(chart1.getBufferedImage(), "SigAndBack");
        _println("");

        final String sigSpec = _printSpecTag("ASCII signal spectrum");
        final String backSpec = _printSpecTag("ASCII background spectrum");

        final ITCChart chart2 = new ITCChart("Intermediate Single Exp and Final S/N", "Wavelength (nm)", "Signal / Noise per spectral pixel", pdp);
        chart2.addArray(result.specS2N()[0].getExpS2NSpectrum().getData(), "Single Exp S/N");
        chart2.addArray(result.specS2N()[0].getFinalS2NSpectrum().getData(), "Final S/N  ");
        _println(chart2.getBufferedImage(), "Sig2N");
        _println("");

        final String singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
        final String finalS2N = _printSpecTag("Final S/N ASCII data");

        _println("");
        device.setPrecision(2);  // TWO decimal places
        device.clear();

        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.source()));
        _println(michelleToString(instrument, result.parameters()));
        _println(HtmlPrinter.printParameterSummary(result.telescope()));
        _println(HtmlPrinter.printParameterSummary(result.conditions()));
        _println(HtmlPrinter.printParameterSummary(result.observation()));
        _println(HtmlPrinter.printParameterSummary(pdp));

        final String header = "# Michelle ITC: " + Calendar.getInstance().getTime() + "\n";
        _println(result.specS2N()[0].getSignalSpectrum(), header, sigSpec);
        _println(result.specS2N()[0].getBackgroundSpectrum(), header, backSpec);
        _println(result.specS2N()[0].getExpS2NSpectrum(), header, singleS2N);
        _println(result.specS2N()[0].getFinalS2NSpectrum(), header, finalS2N);

    }

    private void writeImagingOutput(final ImagingResult result) {

        final Michelle instrument = (Michelle) result.instrument();

        _println("");

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();

        _print(result.sfCalc().getTextResult(device));
        _println(result.iqCalc().getTextResult(device));
        _println("Sky subtraction aperture = " + result.observation().getSkyApertureDiameter() + " times the software aperture.\n");

        if (!instrument.polarimetryIsUsed()) {
            _println(result.is2nCalc().getTextResult(device));
        } else {
            _println("Polarimetry mode enabled.\n");
            final String result2 = result.is2nCalc().getTextResult(device);
            final String delims = "[ ]+";
            final String[] tokens = result2.split(delims);
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].contains("Derived")) {
                    tokens[i + 5] = device.toString((new Double(tokens[i + 5]) * 4));
                    tokens[i + 9] = device.toString((new Double(tokens[i + 9]) * 4));
                }
                if (tokens[i].contains("Taking")) {
                    tokens[i + 1] = device.toString((new Double(tokens[i + 1]) * 4));
                }
                if (tokens[i].contains("Requested") || tokens[i].contains("Required")) {
                    tokens[i + 5] = device.toString((new Double(tokens[i + 5]) * 4));
                    tokens[i + 9] = device.toString((new Double(tokens[i + 9]) * 4));
                }
                _print(tokens[i] + " ");
            }
        }

        device.setPrecision(0);  // NO decimal places
        device.clear();
        _println("");
        _println("The peak pixel signal + background is " + device.toString(result.peakPixelCount()) + ". ");

        if (result.peakPixelCount() > (instrument.getWellDepth()))
            _println("Warning: peak pixel may be saturating the imaging deep well setting of " +
                    instrument.getWellDepth());


        _println("");
        device.setPrecision(2);  // TWO decimal places
        device.clear();

        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.source()));
        _println(michelleToString(instrument, result.parameters()));
        _println(HtmlPrinter.printParameterSummary(result.telescope()));
        _println(HtmlPrinter.printParameterSummary(result.conditions()));

        // Michelle polarimetry calculations include a x4 overhead of observing into the calculation
        // the following code applies this factor to all the needed values
        if (!instrument.polarimetryIsUsed()) {
            _println(HtmlPrinter.printParameterSummary(result.observation()));
        } else {
            final String result2 = HtmlPrinter.printParameterSummary(result.observation());
            final String delims = "[ ]+";
            final String[] tokens = result2.split(delims);
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].contains("<LI>Calculation") && tokens[i + 2].contains("S/N")) {
                    tokens[i + 5] = device.toString((new Double(tokens[i + 5]) * 4));
                }
                _print(tokens[i] + " ");

            }
        }

    }

    private String michelleToString(final Michelle instrument, final Parameters p) {

        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);

        if (!instrument.getFocalPlaneMask().equals(MichelleParameters.NO_SLIT))
            s += "<LI> Focal Plane Mask: " + instrument.getFocalPlaneMask();
        s += "\n";
        s += "\n";
        if (p.observation().getMethod().isSpectroscopy())
            s += "<L1> Central Wavelength: " + instrument.getCentralWavelength() + " nm" + "\n";
        s += "Spatial Binning: 1\n";
        if (p.observation().getMethod().isSpectroscopy())
            s += "Spectral Binning: 1\n";
        s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + "arcsec\n";
        if (p.observation().getMethod().isSpectroscopy())
            s += "Pixel Size in Spectral Direction: " + instrument.getGratingDispersion_nmppix() + "nm\n";
        return s;
    }


}
