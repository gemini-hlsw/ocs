package edu.gemini.itc.web.html;

import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.gmos.Gmos;
import edu.gemini.itc.gmos.GmosRecipe;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.PointSource$;
import edu.gemini.spModel.core.UniformSource$;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;
import scala.collection.JavaConversions;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing GMOS calculation results to an output stream.
 */
public final class GmosPrinter extends PrinterBase {

    private final GmosRecipe recipe;
    private final PlottingDetails pdp;
    private final boolean isImaging;

    public GmosPrinter(final ItcParameters p, final GmosParameters instr, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe         = new GmosRecipe(p, instr);
        this.pdp            = pdp;
        this.isImaging      = p.observation().getMethod().isImaging();
    }

    /**
     * Performs recipe calculation and writes results to a cached PrintWriter or to System.out.
     */
    public void writeOutput() {
        if (isImaging) {
            final ImagingResult[] results = recipe.calculateImaging();
            writeImagingOutput(results);
        } else {
            final SpectroscopyResult[] r = recipe.calculateSpectroscopy();
            final ItcSpectroscopyResult s = recipe.serviceResult(r);
            final UUID id = cache(s);
            writeSpectroscopyOutput(id, r);
        }
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult[] results) {

        final Gmos mainInstrument = (Gmos) results[0].instrument(); // main instrument

        _println("");

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
                        _println(String.format("software aperture extent along slit = %.2f arcsec", results[0].observation().getApertureDiameter()));
                    } else {
                        if (result.source().profile() == UniformSource$.MODULE$) {
                            _println(String.format("software aperture extent along slit = %.2f arcsec", 1 / instrument.getSlitWidth()));
                        } else if (result.source().profile() == PointSource$.MODULE$) {
                            _println(String.format("software aperture extent along slit = %.2f arcsec", (1.4 * result.iqCalc().getImageQuality())));
                        }
                    }

                    if (!results[0].source().isUniform()) {
                        _println(String.format("fraction of source flux in aperture = %.2f", result.st().getSlitThroughput()));
                    }
                }
                _println(String.format("derived image size(FWHM) for a point source = %.2f arcsec\n", result.iqCalc().getImageQuality()));
                _println("Sky subtraction aperture = " + results[0].observation().getSkyApertureDiameter() + " times the software aperture.");
                _println("");
                _println(String.format("Requested total integration time = %.2f secs, of which %.2f secs is on source.", exposure_time * number_exposures, exposure_time * number_exposures * frac_with_source));
                _print("<HR align=left SIZE=3>");
            }

            // For IFUs we can have more than one S2N result.
            for (int i = 0; i < result.specS2N().length; i++) {

                if (ccdIndex == 0) {
                    _println("<p style=\"page-break-inside: never\">");
                    _printFileLinkAllSeries(id, SignalData.instance());
                    _printFileLinkAllSeries(id, BackgroundData.instance());
                    _printFileLinkAllSeries(id, SingleS2NData.instance());
                    _printFileLinkAllSeries(id, FinalS2NData.instance());
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

        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();
        for (final Gmos instrument : ccdArray) {
            final int ccdIndex = instrument.getDetectorCcdIndex();
            final String ccdName = instrument.getDetectorCcdName();
            final String forCcdName = ccdName.length() == 0 ? "" : " for " + ccdName;

            final ImagingResult result = results[ccdIndex];

            if (ccdIndex == 0) {
                _print(CalculatablePrinter.getTextResult(result.sfCalc()));
                _println(CalculatablePrinter.getTextResult(result.iqCalc()));
                _println("Sky subtraction aperture = "
                        + results[0].observation().getSkyApertureDiameter()
                        + " times the software aperture.\n");
                _println("Read noise: " + instrument.getReadNoise());
            }
            _println("");
            _println("<b>S/N" + forCcdName + ":</b>");
            _println("");
            _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation()));

            _println("");
            _println(String.format("The peak pixel signal + background is %.0f. ", result.peakPixelCount()));

            for (final ItcWarning warning : JavaConversions.asJavaList(result.warnings())) {
                _println(warning.msg());
            }

        }

        printConfiguration(results[0].parameters(), mainInstrument);
    }

    private void printConfiguration(final ItcParameters p, final Gmos mainInstrument) {
        _println("");

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

    private String gmosToString(final Gmos instrument, final ItcParameters p) {

        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);

        if (!instrument.getFpMask().equals(GmosNorthType.FPUnitNorth.FPU_NONE) && !instrument.getFpMask().equals(GmosSouthType.FPUnitSouth.FPU_NONE))
            s += "<LI> Focal Plane Mask: " + instrument.getFpMask().displayValue() + "\n";
        s += "\n";
        if (p.observation().getMethod().isSpectroscopy())
            s += String.format("<L1> Central Wavelength: %.1f nm\n", instrument.getCentralWavelength());
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
