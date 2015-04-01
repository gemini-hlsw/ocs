package edu.gemini.itc.web.html;

import edu.gemini.itc.gmos.Gmos;
import edu.gemini.itc.gmos.GmosRecipe;
import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.HtmlPrinter;

import java.awt.*;
import java.io.PrintWriter;
import java.util.Calendar;

/**
 * Helper class for printing GMOS calculation results to an output stream.
 */
public final class GmosPrinter extends PrinterBase {

    private final GmosRecipe recipe;
    private final PlottingDetails pdp;
    private final boolean isImaging;

    public GmosPrinter(final Parameters p, final GmosParameters ip, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe    = new GmosRecipe(p.source(), p.observation(), p.conditions(), ip, p.telescope());
        this.pdp       = pdp;
        this.isImaging = p.observation().getMethod().isImaging();
    }

     /**
     * Performes recipe calculation and writes results to a cached PrintWriter or to System.out.
     */
    public void writeOutput() {
        if (isImaging) {
            final ImagingResult[] results = recipe.calculateImaging();
            writeImagingOutput(results);
        } else {
            final SpectroscopyResult[] results = recipe.calculateSpectroscopy();
            writeSpectroscopyOutput(results);
        }
    }

    private void writeSpectroscopyOutput(final SpectroscopyResult[] results) {

        final Gmos mainInstrument = (Gmos) results[0].instrument(); // main instrument

        _println("");

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        // Create one chart to use for all 3 CCDS (one for Signal and Background and one for Intermediate Single Exp and Final S/N)
        final ITCChart gmosChart1;
        final ITCChart gmosChart2;
        final boolean ifuAndNotUniform = mainInstrument.isIfuUsed() && !(results[0].source().isUniform());
        final double ifu_offset = ifuAndNotUniform ? mainInstrument.getIFU().getApertureOffsetList().iterator().next() : 0.0;
        final String chart1Title = ifuAndNotUniform ? "Signal and Background (IFU element offset: " + device.toString(ifu_offset) + " arcsec)" : "Signal and Background ";
        final String chart2Title = ifuAndNotUniform ? "Intermediate Single Exp and Final S/N (IFU element offset: " + device.toString(ifu_offset) + " arcsec)" : "Intermediate Single Exp and Final S/N";
        gmosChart1 = new ITCChart(chart1Title, "Wavelength (nm)", "e- per exposure per spectral pixel", pdp);
        gmosChart2 = new ITCChart(chart2Title, "Wavelength (nm)", "Signal / Noise per spectral pixel", pdp);

        String sigSpec = null, backSpec = null, singleS2N = null, finalS2N = null;
        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();
        final DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();
        final int detectorCount = ccdArray.length;

        for (final Gmos instrument : ccdArray) {

            final int ccdIndex = instrument.getDetectorCcdIndex();
            final String ccdName = instrument.getDetectorCcdName();
            final Color ccdColor = instrument.getDetectorCcdColor();
            final Color ccdColorDarker = ccdColor == null ? null : ccdColor.darker().darker();
            final int firstCcdIndex = tv.getDetectorCcdStartIndex(ccdIndex);
            final int lastCcdIndex = tv.getDetectorCcdEndIndex(ccdIndex, detectorCount);
            // REL-478: include the gaps in the text data output
            final int lastCcdIndexWithGap = (ccdIndex < 2 && detectorCount > 1)
                    ? tv.getDetectorCcdStartIndex(ccdIndex + 1)
                    : lastCcdIndex;

            final SpectroscopyResult calcGmos = results[ccdIndex];

            final int number_exposures = results[0].observation().getNumExposures();
            final double frac_with_source = results[0].observation().getSourceFraction();
            final double exposure_time = results[0].observation().getExposureTime();

            if (ccdIndex == 0) {
                _println("Read noise: " + instrument.getReadNoise());
                if (!instrument.isIfuUsed()) {
                    if (!results[0].observation().isAutoAperture()) {
                        _println("software aperture extent along slit = " + device.toString(results[0].observation().getApertureDiameter()) + " arcsec");
                    } else {
                        switch (results[0].source().getProfileType()) {
                            case UNIFORM:
                                _println("software aperture extent along slit = " + device.toString(1 / instrument.getSlitWidth()) + " arcsec");
                                break;
                            case POINT:
                                _println("software aperture extent along slit = " + device.toString(1.4 * calcGmos.iqCalc().getImageQuality()) + " arcsec");
                                break;
                        }
                    }

                    if (!results[0].source().isUniform()) {
                        _println("fraction of source flux in aperture = " + device.toString(calcGmos.st().getSlitThroughput()));
                    }
                }
                _println("derived image size(FWHM) for a point source = " + device.toString(calcGmos.iqCalc().getImageQuality()) + "arcsec\n");
                _println("Sky subtraction aperture = " + results[0].observation().getSkyApertureDiameter() + " times the software aperture.");
                _println("");
                _println("Requested total integration time = " + device.toString(exposure_time * number_exposures) + " secs, of which " + device.toString(exposure_time * number_exposures * frac_with_source) + " secs is on source.");
                _print("<HR align=left SIZE=3>");
            }

            // For IFUs we can have more than one S2N result.
            final String header = "# GMOS ITC: " + Calendar.getInstance().getTime() + "\n";
            for (int i = 0; i < calcGmos.specS2N().length; i++) {

                gmosChart1.addArray(calcGmos.specS2N()[i].getSignalSpectrum().getData(firstCcdIndex, lastCcdIndex), "Signal " + ccdName, ccdColor);
                gmosChart1.addArray(calcGmos.specS2N()[i].getBackgroundSpectrum().getData(firstCcdIndex, lastCcdIndex), "SQRT(Background) " + ccdName, ccdColorDarker);

                gmosChart2.addArray(calcGmos.specS2N()[i].getExpS2NSpectrum().getData(firstCcdIndex, lastCcdIndex), "Single Exp S/N " + ccdName, ccdColor);
                gmosChart2.addArray(calcGmos.specS2N()[i].getFinalS2NSpectrum().getData(firstCcdIndex, lastCcdIndex), "Final S/N " + ccdName, ccdColorDarker);

                if (ccdIndex == 0) {
                    _println("<p style=\"page-break-inside: never\">");
                    sigSpec = _printSpecTag("ASCII signal spectrum");
                    backSpec = _printSpecTag("ASCII background spectrum");
                    singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
                    finalS2N = _printSpecTag("Final S/N ASCII data");
                }
                _println("");
            }

            _println(calcGmos.specS2N()[calcGmos.specS2N().length-1].getSignalSpectrum(), header, sigSpec, firstCcdIndex, lastCcdIndexWithGap);
            _println(calcGmos.specS2N()[calcGmos.specS2N().length-1].getBackgroundSpectrum(), header, backSpec, firstCcdIndex, lastCcdIndexWithGap);
            _println(calcGmos.specS2N()[calcGmos.specS2N().length-1].getExpS2NSpectrum(), header, singleS2N, firstCcdIndex, lastCcdIndexWithGap);
            _println(calcGmos.specS2N()[calcGmos.specS2N().length-1].getFinalS2NSpectrum(), header, finalS2N, firstCcdIndex, lastCcdIndexWithGap);

        }

        _println(gmosChart1.getBufferedImage(), "SigAndBack");
        _println("");
        _println(gmosChart2.getBufferedImage(), "Sig2N");
        _println("");

        printConfiguration(results[0].parameters(), mainInstrument);
    }


    private void writeImagingOutput(final ImagingResult[] results) {

        final Gmos mainInstrument = (Gmos) results[0].instrument(); // main instrument

        _println("");

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();


        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();
        for (final Gmos instrument : ccdArray) {
            final int ccdIndex = instrument.getDetectorCcdIndex();
            final String ccdName = instrument.getDetectorCcdName();
            final String forCcdName = ccdName.length() == 0 ? "" : " for " + ccdName;

            final ImagingResult calcGmos = results[ccdIndex];

            if (ccdIndex == 0) {
                _print(calcGmos.sfCalc().getTextResult(device));
                _println(calcGmos.iqCalc().getTextResult(device));
                _println("Sky subtraction aperture = "
                        + results[0].observation().getSkyApertureDiameter()
                        + " times the software aperture.\n");
                _println("Read noise: " + instrument.getReadNoise());
            }
            _println("");
            _println("<b>S/N" + forCcdName + ":</b>");
            _println("");
            _println(calcGmos.is2nCalc().getTextResult(device));

            device.setPrecision(0); // NO decimal places
            device.clear();
            final int binFactor = instrument.getSpatialBinning() * instrument.getSpatialBinning();

            _println("");
            _println("The peak pixel signal + background is " + device.toString(calcGmos.peakPixelCount()) + ". ");

            if (calcGmos.peakPixelCount() > (.95 * instrument.getWellDepth() * binFactor))
                _println("Warning: peak pixel may be saturating the (binned) CCD full well of "
                        + .95 * instrument.getWellDepth() * binFactor);

            if (calcGmos.peakPixelCount() > (.95 * instrument.getADSaturation() * instrument.getLowGain()))
                _println("Warning: peak pixel may be saturating the low gain setting of "
                        + .95
                        * instrument.getADSaturation()
                        * instrument.getLowGain());

            if (calcGmos.peakPixelCount() > (.95 * instrument.getADSaturation() * instrument.getHighGain()))
                _println("Warning: peak pixel may be saturating the high gain setting "
                        + .95
                        * instrument.getADSaturation()
                        * instrument.getHighGain());

        }

        printConfiguration(results[0].parameters(), mainInstrument);
    }

    private void printConfiguration(final Parameters p, final Gmos mainInstrument) {
        _println("");

        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // TWO decimal places
        device.clear();

        _print("<HR align=left SIZE=3>");

        _println(HtmlPrinter.printParameterSummary(pdp));

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + mainInstrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(p.source()));
        _println(mainInstrument.toString());
        _println(HtmlPrinter.printParameterSummary(p.telescope()));
        _println(HtmlPrinter.printParameterSummary(p.conditions()));
        _println(HtmlPrinter.printParameterSummary(p.observation()));
    }


}
