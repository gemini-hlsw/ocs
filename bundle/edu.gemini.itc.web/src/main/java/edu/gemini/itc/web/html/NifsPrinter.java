package edu.gemini.itc.web.html;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.Result;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.nifs.IFUComponent;
import edu.gemini.itc.nifs.Nifs;
import edu.gemini.itc.nifs.NifsRecipe;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.nifs.NIFSParams;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator;
import edu.gemini.spModel.obscomp.ItcOverheadProvider;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing NIFS calculation results to an output stream.
 */
public final class NifsPrinter extends PrinterBase implements OverheadTablePrinter.PrinterWithOverhead {

    private final NifsRecipe recipe;
    private final PlottingDetails pdp;
    private final NifsParameters instr;
    private final ItcParameters p;

    public NifsPrinter(final ItcParameters p, final NifsParameters instr, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe = new NifsRecipe(p, instr);
        this.pdp    = pdp;
        this.instr  = instr;
        this.p      = p;
    }

    public void writeOutput() {
        final SpectroscopyResult r = recipe.calculateSpectroscopy();
        final ItcSpectroscopyResult s = recipe.serviceResult(r, false);
        final UUID id = cache(s);
        writeSpectroscopyOutput(id, r, s);
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult result, final ItcSpectroscopyResult s) {

        final Nifs instrument = (Nifs) result.instrument();
        final double iqAtSource = result.iqCalc().getImageQuality();

        _println("");

        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printSummary((Altair) result.aoSystem().get()));
        }

        _println(String.format("derived image halo size (FWHM) for a point source = %.2f arcsec\n", iqAtSource));

        _printRequestedIntegrationTime(result);

        _println("");

        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

        _print(OverheadTablePrinter.print(this, p, getReadoutTimePerCoadd(), result, s));

        _print("<HR align=left SIZE=3>");

        for (int i = 0; i < result.specS2N().length; i++) {
            _println("<p style=\"page-break-inside: never\">");
            _printImageLink(id, SignalChart.instance(),   i, pdp);
            _println("");
            _printFileLink(id, SignalData.instance(),     i);
            _printFileLink(id, BackgroundData.instance(), i);
            _printImageLink(id, S2NChart.instance(),      i, pdp);
            _println("");
            _printFileLink(id, SingleS2NData.instance(),  i);
            _printFileLink(id, FinalS2NData.instance(),   i);
        }

        _println("");

        _print("<HR align=left SIZE=3>");

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.source()));
        _println(nifsToString(instrument));
        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printParameterSummary((Altair) result.aoSystem().get()));
        }
        _println(HtmlPrinter.printParameterSummary(result.telescope()));
        _println(HtmlPrinter.printParameterSummary(result.conditions(), instrument.getEffectiveWavelength(), iqAtSource));
        _println(HtmlPrinter.printParameterSummary(result.observation()));
        _println(HtmlPrinter.printParameterSummary(pdp));

    }

    private String nifsToString(final Nifs instrument) {
        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);
        s += "<LI>Focal Plane Mask: ifu\n";
        s += "<LI>Read Noise: " + instrument.getReadNoise() + "\n";
        s += "<LI>Well Depth: 90000.0\n";
        s += "\n";

        s += String.format("<L1> Central Wavelength: %.1f nm\n", instrument.getCentralWavelength());
        s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + " arcsec\n";

        s += String.format("Pixel Size in Spectral Direction: %.3f nm\n", instrument.getGratingDispersion());

        s += "IFU is selected,";
        if (instrument.getIFUMethod() instanceof IfuSingle)
            s += "with a single IFU element at " + instrument.getIFUOffset() + " arcsecs.";
        else if (instrument.getIFUMethod() instanceof IfuSummed)
            s += String.format("with multiple summed IFU elements arranged in a " + instrument.getIFUNumX() + "x" + instrument.getIFUNumY() +
                    " (%.3f\"x%.3f\") grid.", instrument.getIFUNumX() * IFUComponent.IFU_LEN_X, instrument.getIFUNumY() * IFUComponent.IFU_LEN_Y);
        else
            s += "with mulitple IFU elements arranged from " + instrument.getIFUMinOffset() + " to " + instrument.getIFUMaxOffset() + " arcsecs.";
        s += "\n";

        return s;
    }


    public ConfigCreator.ConfigCreatorResult createInstConfig(int numberExposures) {
        ConfigCreator cc = new ConfigCreator(p);
        return cc.createNifsConfig(instr, numberExposures);
    }

    public ItcOverheadProvider getInst() {
            return new InstNIFS();

    }
    public double getReadoutTimePerCoadd() {
        return instr.readMode().getMinExp() + InstNIFS.COADD_CONSTANT;
    }

    @Override
    public double getVisitTime() {
        return this.getVisit_time();
    }

    @Override
    public double getRecenterInterval() {
        return this.getRecentInterval();
    }

    public int getNumberExposures() { throw new Error("Not implemented"); }
}
