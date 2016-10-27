package edu.gemini.itc.web.html;

import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.gmos.Gmos;
import edu.gemini.itc.gmos.GmosRecipe;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;

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
        this.isImaging      = p.observation().calculationMethod() instanceof Imaging;
    }

    /**
     * Performs recipe calculation and writes results to a cached PrintWriter or to System.out.
     */
    public void writeOutput() {
        if (isImaging) {
            final ImagingResult[] results = recipe.calculateImaging();
            final ItcImagingResult s = recipe.serviceResult(results);
            writeImagingOutput(results, s);
        } else {
            final SpectroscopyResult[] r = recipe.calculateSpectroscopy();
            final ItcSpectroscopyResult s = recipe.serviceResult(r);
            final UUID id = cache(s);
            writeSpectroscopyOutput(id, r, s);
        }
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult[] results, final ItcSpectroscopyResult s) {

        final Gmos mainInstrument = (Gmos) results[0].instrument(); // main instrument

        _println("");

        final Gmos[] ccdArray           = mainInstrument.getDetectorCcdInstruments();
        final SpectroscopyResult result = results[0];

        _println("Read noise: " + mainInstrument.getReadNoise());

        if (!mainInstrument.isIfuUsed()) {
            _printSoftwareAperture(results[0], 1 / mainInstrument.getSlitWidth());
        }
        _println(String.format("derived image size(FWHM) for a point source = %.2f arcsec\n", result.iqCalc().getImageQuality()));
        _printSkyAperture(result);
        _println("");

        _printRequestedIntegrationTime(result);

        for (final Gmos instrument : ccdArray) {
            if (ccdArray.length > 1) {
                printCcdTitle(instrument);
            }
            final int ccdIndex = instrument.getDetectorCcdIndex();
            _printPeakPixelInfo(s.ccd(ccdIndex));
            _printWarnings(s.ccd(ccdIndex).warnings());
        }

        _print("<HR align=left SIZE=3>");

        // For IFUs we can have more than one S2N result.
        // Print links for all data files and the charts for each IFU.
        // For the non IFU case specS2N will have only one entry.
        for (int i = 0; i < result.specS2N().length; i++) {
            _println("<p style=\"page-break-inside: never\">");
            _printFileLinkAllSeries(id, SignalData.instance(),     i);
            _printFileLinkAllSeries(id, BackgroundData.instance(), i);
            _printFileLinkAllSeries(id, SingleS2NData.instance(),  i);
            _printFileLinkAllSeries(id, FinalS2NData.instance(),  i);
            if (mainInstrument.isIfu2()) {
                _printFileLink(id, PixSigData.instance(), i, 0, " (red)");
                _printFileLink(id, PixSigData.instance(), i, 1, " (blue)");
                _printFileLink(id, PixBackData.instance(), i, 0, " (red)");
                _printFileLink(id, PixBackData.instance(), i, 1, " (blue)");
            }
            _printImageLink(id, SignalChart.instance(), i, pdp);
            _println("");
            _printImageLink(id, S2NChart.instance(),    i, pdp);
            _println("");
            if (mainInstrument.isIfu2()) {
                _printImageLink(id, SignalPixelChart.instance(), i, pdp);
                _println("");
            }
        }

        printConfiguration(results[0].parameters(), mainInstrument);
    }


    private void writeImagingOutput(final ImagingResult[] results, final ItcImagingResult s) {
        // use instrument of ccd 0 to represent GMOS (this is a design flaw: instead of using one instrument
        // with 3 ccds the current implementation uses three instruments to represent the different ccds).
        final Gmos instrument = (Gmos) results[0].instrument();

        _println("");
        _print(CalculatablePrinter.getTextResult(results[0].sfCalc()));
        _println(CalculatablePrinter.getTextResult(results[0].iqCalc()));
        _printSkyAperture(results[0]);
        _println("Read noise: " + instrument.getReadNoise());

        final Gmos[] ccdArray = instrument.getDetectorCcdInstruments();

        for (final Gmos ccd : ccdArray) {

            if (ccdArray.length > 1) {
                printCcdTitle(ccd);
            }

            final int ccdIndex = ccd.getDetectorCcdIndex();
            _println(CalculatablePrinter.getTextResult(results[ccdIndex].is2nCalc(), results[ccdIndex].observation()));
            _printPeakPixelInfo(s.ccd(ccdIndex));
            _printWarnings(s.ccd(ccdIndex).warnings());

        }

        printConfiguration(results[0].parameters(), instrument);
    }

    private void printCcdTitle(final Gmos ccd) {
        final String ccdName = ccd.getDetectorCcdName();
        final String forCcdName = ccdName.length() == 0 ? "" : " for " + ccdName;
        _println("");
        _println("<b>S/N" + forCcdName + ":</b>");
        _println("");
    }

    private void printConfiguration(final ItcParameters p, final Gmos mainInstrument) {
        _println("");

        _print("<HR align=left SIZE=3>");

        _println(HtmlPrinter.printParameterSummary(pdp));

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + mainInstrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(p.source()));
        _println(gmosToString(mainInstrument, p, (GmosParameters) p.instrument()));
        _println(HtmlPrinter.printParameterSummary(p.telescope()));
        _println(HtmlPrinter.printParameterSummary(p.conditions()));
        _println(HtmlPrinter.printParameterSummary(p.observation()));
    }

    private String gmosToString(final Gmos instrument, final ItcParameters p, final GmosParameters config) {

        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);

        s += String.format("Amp gain: %s, Amp read mode: %s\n",config.ampGain().displayValue() ,config.ampReadMode().displayValue());

        if (!instrument.getFpMask().equals(GmosNorthType.FPUnitNorth.FPU_NONE) && !instrument.getFpMask().equals(GmosSouthType.FPUnitSouth.FPU_NONE))
            s += "<LI> Focal Plane Mask: " + instrument.getFpMask().displayValue() + "\n";
        s += "\n";
        if (p.observation().calculationMethod() instanceof Spectroscopy)
            s += String.format("<L1> Central Wavelength: %.1f nm\n", instrument.getCentralWavelength());
        s += "Spatial Binning: " + instrument.getSpatialBinning() + "\n";
        if (p.observation().calculationMethod() instanceof Spectroscopy)
            s += "Spectral Binning: " + instrument.getSpectralBinning() + "\n";
        s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + "arcsec\n";
        if (p.observation().calculationMethod() instanceof Spectroscopy)
            s += "Pixel Size in Spectral Direction: " + instrument.getGratingDispersion() + "nm\n";
        if (instrument.isIfuUsed()) {
            s += "IFU is selected,";
            if (instrument.getIfuMethod().get() instanceof IfuSingle) {
                final IfuSingle ifu = (IfuSingle) instrument.getIfuMethod().get();
                s += "with a single IFU element at " + ifu.offset() + "arcsecs.";
            } else if (instrument.getIfuMethod().get() instanceof IfuRadial) {
                final IfuRadial ifu = (IfuRadial) instrument.getIfuMethod().get();
                s += "with mulitple IFU elements arranged from " + ifu.minOffset() + " to " + ifu.maxOffset() + "arcsecs.";
            } else if (instrument.getIfuMethod().get() instanceof IfuSum) {
                final IfuSum ifu = (IfuSum) instrument.getIfuMethod().get();
                s += " with IFU elements summed within " + ifu.num() + "arcsecs of center.";
            } else {
                throw new Error("invalid IFU type");
            }
            s += "\n";
        }
        return s;
    }

}
