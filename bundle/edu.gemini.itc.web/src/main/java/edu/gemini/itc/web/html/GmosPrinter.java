package edu.gemini.itc.web.html;

import edu.gemini.itc.gmos.Gmos;
import edu.gemini.itc.gmos.GmosRecipe;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.servlets.ImageServlet;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;
import scala.Tuple2;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing GMOS calculation results to an output stream.
 */
public final class GmosPrinter extends PrinterBase {

    private final GmosRecipe recipe;
    private final PlottingDetails pdp;
    private final boolean isImaging;

    public GmosPrinter(final Parameters p, final GmosParameters ip, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe = new GmosRecipe(p.source(), p.observation(), p.conditions(), ip, p.telescope());
        this.pdp = pdp;
        this.isImaging = p.observation().getMethod().isImaging();
    }

    /**
     * Performs recipe calculation and writes results to a cached PrintWriter or to System.out.
     */
    public void writeOutput() {
        if (isImaging) {
            final ImagingResult[] results = recipe.calculateImaging();
            writeImagingOutput(results);
        } else {
            final Tuple2<ItcSpectroscopyResult, SpectroscopyResult[]> r = recipe.calculateSpectroscopy();
            final UUID id = cache(r._1());
            writeSpectroscopyOutput(id, r._2());
        }
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult[] results) {

        final Gmos mainInstrument = (Gmos) results[0].instrument(); // main instrument

        _println("");

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();

        for (final Gmos instrument : ccdArray) {

            final int ccdIndex = instrument.getDetectorCcdIndex();
            final SpectroscopyResult result = results[ccdIndex];

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
                                _println("software aperture extent along slit = " + device.toString(1.4 * result.iqCalc().getImageQuality()) + " arcsec");
                                break;
                        }
                    }

                    if (!results[0].source().isUniform()) {
                        _println("fraction of source flux in aperture = " + device.toString(result.st().getSlitThroughput()));
                    }
                }
                _println("derived image size(FWHM) for a point source = " + device.toString(result.iqCalc().getImageQuality()) + "arcsec\n");
                _println("Sky subtraction aperture = " + results[0].observation().getSkyApertureDiameter() + " times the software aperture.");
                _println("");
                _println("Requested total integration time = " + device.toString(exposure_time * number_exposures) + " secs, of which " + device.toString(exposure_time * number_exposures * frac_with_source) + " secs is on source.");
                _print("<HR align=left SIZE=3>");
            }

            // For IFUs we can have more than one S2N result.
            for (int i = 0; i < result.specS2N().length; i++) {

                if (ccdIndex == 0) {
                    _println("<p style=\"page-break-inside: never\">");
                    _printFileLink(id, SignalData.instance());
                    _printFileLink(id, BackgroundData.instance());
                    _printFileLink(id, SingleS2NData.instance());
                    _printFileLink(id, FinalS2NData.instance());
                }
                _println("");
            }

        }

        _printImageLink(id, SignalChart.instance(), pdp);
        _println("");
        _printImageLink(id, S2NChart.instance(), pdp);
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

            final ImagingResult result = results[ccdIndex];

            if (ccdIndex == 0) {
                _print(CalculatablePrinter.getTextResult(result.sfCalc(), device));
                _println(CalculatablePrinter.getTextResult(result.iqCalc(), device));
                _println("Sky subtraction aperture = "
                        + results[0].observation().getSkyApertureDiameter()
                        + " times the software aperture.\n");
                _println("Read noise: " + instrument.getReadNoise());
            }
            _println("");
            _println("<b>S/N" + forCcdName + ":</b>");
            _println("");
            _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation(), device));

            device.setPrecision(0); // NO decimal places
            device.clear();
            final int binFactor = instrument.getSpatialBinning() * instrument.getSpatialBinning();

            _println("");
            _println("The peak pixel signal + background is " + device.toString(result.peakPixelCount()) + ". ");

            if (result.peakPixelCount() > (.95 * instrument.getWellDepth() * binFactor))
                _println("Warning: peak pixel may be saturating the (binned) CCD full well of "
                        + .95 * instrument.getWellDepth() * binFactor);

            if (result.peakPixelCount() > (.95 * instrument.getADSaturation() * instrument.getLowGain()))
                _println("Warning: peak pixel may be saturating the low gain setting of "
                        + .95
                        * instrument.getADSaturation()
                        * instrument.getLowGain());

            if (result.peakPixelCount() > (.95 * instrument.getADSaturation() * instrument.getHighGain()))
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
        _println(gmosToString(mainInstrument, p));
        _println(HtmlPrinter.printParameterSummary(p.telescope()));
        _println(HtmlPrinter.printParameterSummary(p.conditions()));
        _println(HtmlPrinter.printParameterSummary(p.observation()));
    }

    private String gmosToString(final Gmos instrument, final Parameters p) {

        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);

        if (!instrument.getFpMask().equals(GmosNorthType.FPUnitNorth.FPU_NONE) && !instrument.getFpMask().equals(GmosSouthType.FPUnitSouth.FPU_NONE))
            s += "<LI> Focal Plane Mask: " + instrument.getFpMask().displayValue() + "\n";
        s += "\n";
        if (p.observation().getMethod().isSpectroscopy())
            s += "<L1> Central Wavelength: " + instrument.getCentralWavelength() + " nm" + "\n";
        s += "Spatial Binning: " + instrument.getSpatialBinning() + "\n";
        if (p.observation().getMethod().isSpectroscopy())
            s += "Spectral Binning: " + instrument.getSpectralBinning() + "\n";
        s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + "arcsec\n";
        if (p.observation().getMethod().isSpectroscopy())
            s += "Pixel Size in Spectral Direction: " + instrument.getGratingDispersion_nmppix() + "nm\n";
        if (instrument.isIfuUsed()) {
            s += "IFU is selected,";
            if (instrument.getIfuMethod().get() instanceof IfuSingle) {
                final IfuSingle ifu = (IfuSingle) instrument.getIfuMethod().get();
                s += "with a single IFU element at " + ifu.offset() + "arcsecs.";
            } else if (instrument.getIfuMethod().get() instanceof IfuRadial) {
                final IfuRadial ifu = (IfuRadial) instrument.getIfuMethod().get();
                s += "with mulitple IFU elements arranged from " + ifu.minOffset() + " to " + ifu.maxOffset() + "arcsecs.";
            } else {
                throw new Error("invalid IFU type");
            }
            s += "\n";
        }
        return s;
    }

}
