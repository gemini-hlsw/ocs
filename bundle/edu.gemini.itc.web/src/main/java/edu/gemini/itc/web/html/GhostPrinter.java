package edu.gemini.itc.web.html;

import edu.gemini.itc.ghost.GHostRecipe;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.ghost.GhostSaturLimitRule;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.obscomp.ItcOverheadProvider;
import edu.gemini.itc.ghost.Ghost;

import java.io.PrintWriter;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Helper class for printing GHOST calculation results to an output stream.
 */
public final class GhostPrinter extends PrinterBase implements OverheadTablePrinter.PrinterWithOverhead {

    private static final Logger Log = Logger.getLogger(GhostPrinter.class.getName());
    private final GHostRecipe recipe;
    private final PlottingDetails pdp;
    private final ItcParameters p;
    private final GhostParameters instr;

    public GhostPrinter(final ItcParameters p, final GhostParameters instr, final PlottingDetails pdp, final PrintWriter out) {
        //
        //super(out, 18000, 9000);
        super(out, 7200, 3600);
        this.recipe         = new GHostRecipe(p, instr);
        this.pdp            = pdp;
        this.p              = p;
        this.instr          = instr;
    }

    /**
     * Performs recipe calculation and writes results to a cached PrintWriter or to System.out.
     */
    public void writeOutput() {
        final SpectroscopyResult[] r = recipe.calculateSpectroscopy();
        final ItcSpectroscopyResult s = recipe.serviceResult(r, false);
        //final ItcSpectroscopyResult s[] = recipe.serviceResult(r, false);
        final UUID id = cache(s);
        writeSpectroscopyOutput(id, r, s);
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult[] results, final ItcSpectroscopyResult s) {

        final Ghost mainInstrument = (Ghost) results[0].instrument(); // main instrument

        _println("");

        final SpectroscopyResult result = results[0];
        final double iqAtSource = result.iqCalc().getImageQuality();

        Log.info("***** 222 calculateSpectroscopy getImageQuality[0]: " + results[0].iqCalc().getImageQuality() + " [1]: "+ results[1].iqCalc().getImageQuality() + "  " + iqAtSource);

        _println("Read noise: " + mainInstrument.getReadNoise());


        _println(String.format("derived image size(FWHM) for a point source = %.2f arcsec\n", iqAtSource));
        _printSkyAperture(result);
        _println("");

        _printRequestedIntegrationTime(result);
        _println("");

        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

        _print(OverheadTablePrinter.print(this, p, results[0], s));

        Log.info("CCD lengh: " + s.ccds().length() );

        _print("<HR align=left SIZE=3>");

        //for (int i = 0; i < results.length; i++) {
        for (int i = 0; i < 1; i++) { // If the user want to plot the SNR for each detector,
                                      // it would have to use the previous instruction. This has been commented
                                      // because the QE of the CCD is the same for both. Therefore, the SNR are similar.
            _println("<p style=\"page-break-inside: never\">");
            Log.info("Plotting results, index: "+i);
            _printImageLink(id, SignalChart.instance(), i, pdp);
            _println("");
            _printFileLink(id, SignalData.instance(), i);
            _printFileLink(id, BackgroundData.instance(), i);
            _printImageLink(id, S2NChart.instance(), i, pdp);
            _println("");
            _printFileLink(id, SingleS2NData.instance(), i);
            _printFileLink(id, FinalS2NData.instance(), i);
            _println("");
            _printImageLink(id, S2NChartPerRes.instance(), i, pdp);
            _printFileLink(id, SingleS2NPerResEle.instance(), i);
            _printFileLink(id, FinalS2NPerResEle.instance(), i);


        }

        printConfiguration(results[0].parameters(), mainInstrument, iqAtSource);
        _println(HtmlPrinter.printParameterSummary(pdp));
    }

    private void printCcdTitle(final Ghost ccd) {
        final String ccdName = ccd.getDetectorCcdName();
        final String forCcdName = ccdName.length() == 0 ? "" : " for " + ccdName;
        _println("");
        _println("<b>S/N" + forCcdName + ":</b>");
        _println("");
    }

    private void printConfiguration(final ItcParameters p, final Ghost mainInstrument, final double iqAtSource) {
        _println("");

        _print("<HR align=left SIZE=3>");

        _println(HtmlPrinter.printParameterSummary(pdp));

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + mainInstrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(p.source()));
        _println(ghostToString(mainInstrument, p, (GhostParameters) p.instrument()));
        _println(HtmlPrinter.printParameterSummary(p.telescope()));
        _println(HtmlPrinter.printParameterSummary(p.conditions(), mainInstrument.getEffectiveWavelength(), iqAtSource));
        _println(HtmlPrinter.printParameterSummary(p.observation()));
    }

    private String ghostToString(final Ghost instrument, final ItcParameters p, final GhostParameters config) {

        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);
        s += "\n";
        s += "Spatial Binning (y-binning): " + instrument.getSpatialBinning() + "\n";
        s += "Spectral Binning (x-binning): " + instrument.getSpectralBinning() + "\n";
        s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + " arcsec\n";
        s += "Pixel Size in Spectral Direction: " + instrument.getGratingDispersion() + " nm\n";

        return s;
    }

    protected void _printPeakPixelInfo(final scala.Option<ItcCcd> ccd, final GhostSaturLimitRule ghostLimit) {
        if (ccd.isDefined()) {
            _println(
                    String.format("The peak pixel signal + background is %.0f e- (%d ADU). This is %.0f%% of the saturation limit of %.0f e-.",
                            ccd.get().peakPixelFlux(), ccd.get().adu(), ghostLimit.percentOfLimit(ccd.get().peakPixelFlux()), ghostLimit.limit()));
        }
    }

    public ConfigCreator.ConfigCreatorResult createInstConfig(int numberExposures) {
        ConfigCreator cc = new ConfigCreator(p);
        p.instrument();
        return cc.createGhostConfig(instr, numberExposures);
    }


    public ItcOverheadProvider getInst() {
        return new edu.gemini.spModel.gemini.ghost.Ghost();
    }


    public double getReadoutTimePerCoadd() {
        return 0;
    }

    @Override
    public double getVisitTime() {
        return this.getVisit_time();
    }

    @Override
    public double getRecenterInterval() {
        return this.getRecentInterval();
    }

}
