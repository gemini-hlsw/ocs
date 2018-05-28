package edu.gemini.itc.web.html;

import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.Result;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.gmos.Gmos;
import edu.gemini.itc.gmos.GmosRecipe;
import edu.gemini.itc.gmos.GmosSaturLimitRule;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.gmos.*;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator;

import java.io.PrintWriter;
import java.util.*;

/**
 * Helper class for printing GMOS calculation results to an output stream.
 */
public final class GmosPrinter extends PrinterBase implements OverheadTablePrinter.PrinterWithOverhead {

    private final GmosRecipe recipe;
    private final PlottingDetails pdp;
    private final boolean isImaging;
    private final ItcParameters p;
    private final GmosParameters instr;

    public GmosPrinter(final ItcParameters p, final GmosParameters instr, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe         = new GmosRecipe(p, instr);
        this.pdp            = pdp;
        this.isImaging      = p.observation().calculationMethod() instanceof Imaging;
        this.p              = p;
        this.instr          = instr;
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
        _println("");

        scala.Option<ItcCcd> ccdWithMaxPeak = scala.Option.empty();
        Optional<Gmos> instrumentWithMaxPeak = Optional.empty();
        // Printing one peak pixel value, maximum across all CCDs and spectra
        for (final Gmos instrument : ccdArray) {
            final int ccdIndex = instrument.getDetectorCcdIndex();
            if (s.ccd(ccdIndex).isDefined()) {
                if ((int)s.ccd(ccdIndex).get().peakPixelFlux() == s.maxPeakPixelFlux()) {
                    ccdWithMaxPeak = s.ccd(ccdIndex);
                    instrumentWithMaxPeak = Optional.of(instrument);
                }
            }
        }

        if (ccdWithMaxPeak.isDefined()) {
            if (instrumentWithMaxPeak.isPresent()) {
                _printPeakPixelInfo(ccdWithMaxPeak, instrumentWithMaxPeak.get().getGmosSaturLimitWarning());
            }
        }

        OverheadTablePrinter overheadTablePrinter = new OverheadTablePrinter(this, p, results[0], s);
        _println(overheadTablePrinter.printOverheadTable());

        _print("<HR align=left SIZE=3>");

        // For IFUs we can have more than one S2N result.
        // Print links for all data files and the charts for each IFU.
        // For the non IFU case specS2N will have only one entry.
        for (int i = 0; i < result.specS2N().length; i++) {
            _println("<p style=\"page-break-inside: never\">");
            if (mainInstrument.isIfu2()) {
                List<Integer> indicesIfu2R = new ArrayList<>();
                List<Integer> indicesIfu2B = new ArrayList<>();
                final int numberOfSeries = 2; // of each type

                for (int n = 0; n < ccdArray.length; n++) {
                    // Indices for chart series
                    // Note: sigIndicesXXX works both for signal and S2N, bkIndicesXXX works for background and final S2N
                    indicesIfu2B.add(n * numberOfSeries);
                    indicesIfu2R.add(n * numberOfSeries + 1);
                }
                _printFileLink(id, SignalData.instance(),     i, indicesIfu2R, " (red slit)");
                _printFileLink(id, SignalData.instance(),     i, indicesIfu2B, " (blue slit)");
                _printFileLink(id, BackgroundData.instance(), i, indicesIfu2R, " (red slit)");
                _printFileLink(id, BackgroundData.instance(), i, indicesIfu2B, " (blue slit)");
                _printFileLink(id, SingleS2NData.instance(),  i, indicesIfu2R, " (red slit)");
                _printFileLink(id, SingleS2NData.instance(),  i, indicesIfu2B, " (blue slit)");
                _printFileLink(id, FinalS2NData.instance(),   i, indicesIfu2R, " (red slit)");
                _printFileLink(id, FinalS2NData.instance(),   i, indicesIfu2B, " (blue slit)");
                _printFileLink(id, PixSigData.instance(),     i, indicesIfu2R, " (red slit)");
                _printFileLink(id, PixSigData.instance(),     i, indicesIfu2B, " (blue slit)");
                _printFileLink(id, PixBackData.instance(),    i, indicesIfu2R, " (red slit)");
                _printFileLink(id, PixBackData.instance(),    i, indicesIfu2B, " (blue slit)");

            }
            else {
                _printFileLinkAllSeries(id, SignalData.instance(),     i);
                _printFileLinkAllSeries(id, BackgroundData.instance(), i);
                _printFileLinkAllSeries(id, SingleS2NData.instance(),  i);
                _printFileLinkAllSeries(id, FinalS2NData.instance(),   i);
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
            if (s.ccd(ccdIndex).isDefined()) {
                _printPeakPixelInfo(s.ccd(ccdIndex), instrument.getGmosSaturLimitWarning());
                _printWarnings(s.ccd(ccdIndex).get().warnings());
            }
        }

        OverheadTablePrinter overheadTablePrinter = new OverheadTablePrinter(this, p, results[0]);
        _println(overheadTablePrinter.printOverheadTable());


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

    protected void _printPeakPixelInfo(final scala.Option<ItcCcd> ccd, final GmosSaturLimitRule gmosLimit) {
        if (ccd.isDefined()) {
            _println(
                    String.format("The peak pixel signal + background is %.0f e- (%d ADU). This is %.0f%% of the saturation limit of %.0f e-.",
                            ccd.get().peakPixelFlux(), ccd.get().adu(), gmosLimit.percentOfLimit(ccd.get().peakPixelFlux()), gmosLimit.limit()));
        }
    }

    public ConfigCreator.ConfigCreatorResult createInstConfig(int numberExposures) {
        ConfigCreator cc = new ConfigCreator(p);
        return cc.createGmosConfig(instr, numberExposures);
    }

    public PlannedTime.ItcOverheadProvider getInst() {
        if (instr.site().displayName.equals("Gemini North")) {
            return new InstGmosNorth();
        } else if (instr.site().displayName.equals("Gemini South")) {
            return new InstGmosSouth();
        } else {
            throw new Error("invalid site");
        }
    }

    public double getReadoutTimePerCoadd() {
        return 0;
    }
}
