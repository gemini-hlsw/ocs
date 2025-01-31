package edu.gemini.itc.web.html;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.*;
import edu.gemini.itc.igrins2.Igrins2;
import edu.gemini.itc.igrins2.Igrins2Recipe;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.obscomp.ItcOverheadProvider;

import scala.Option;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.UUID;

/**
 * Helper class for printing IGRINS-2 calculation results to an output stream.
 */
public final class Igrins2Printer extends PrinterBase implements OverheadTablePrinter.PrinterWithOverhead {

    private static final Logger Log = Logger.getLogger(Igrins2Printer.class.getName());
    private final Igrins2Parameters instr;
    private final PlottingDetails pdp;
    private final Igrins2Recipe recipe;
    private final ItcParameters p;
    private int numberExposures;

    public Igrins2Printer(final ItcParameters p,
                          final Igrins2Parameters instr,
                          final PlottingDetails pdp,
                          final PrintWriter out) {
        super(out);
        this.instr = instr;
        recipe = new Igrins2Recipe(p, instr);
        this.pdp = pdp;
        this.p = p;
    }

    public void writeOutput() {
        final SpectroscopyResult[] r = recipe.calculateSpectroscopy();
        final ItcSpectroscopyResult s = recipe.serviceResult(r, false);
        final UUID id = cache(s);
        writeSpectroscopyOutput(id, r, s);
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult[] results, final ItcSpectroscopyResult s) {

        final Igrins2 mainInstrument = (Igrins2) results[0].instrument();
        final CalculationMethod calcMethod = p.observation().calculationMethod();
        final SpectroscopyResult result = results[0];
        final double iqAtSource = result.iqCalc().getImageQuality();
        double exposureTime = recipe.getExposureTime();
        numberExposures = recipe.getNumberExposures();
        String str;

        _println("");

        _println(String.format("Read noise: %.2f e- in %s, and %.2f e- in %s, using %d Fowler samples.",
                ((Igrins2) results[0].instrument()).getReadNoise(exposureTime), ((Igrins2) results[0].instrument()).getWaveBand(),
                ((Igrins2) results[1].instrument()).getReadNoise(exposureTime), ((Igrins2) results[1].instrument()).getWaveBand(),
                ((Igrins2) results[0].instrument()).getFowlerSamples(exposureTime)));

        _println(String.format("Dark current: %.4f e-/s/pix in %s and %.4f e-/s/pix in %s.",
                results[0].instrument().getDarkCurrent(), ((Igrins2) results[0].instrument()).getWaveBand(),
                results[1].instrument().getDarkCurrent(), ((Igrins2) results[1].instrument()).getWaveBand()));

        if (result.aoSystem().isDefined()) { _println(HtmlPrinter.printSummary((Altair) result.aoSystem().get())); }

        _printSoftwareAperture(result, 1 / mainInstrument.getSlitWidth());
        _println(String.format("Derived image size (FWHM) for a point source = %.2f arcsec.", iqAtSource));
        _println("");

        if (calcMethod instanceof SpectroscopyInt) {
            if (exposureTime < 10) {
                str = "Total integration time = %.1f seconds (%d x %.2f s), of which %.1f seconds is on source.";
            } else {
                str = "Total integration time = %.0f seconds (%d x %.0f s), of which %.0f seconds is on source.";
            }
            _println(String.format(str, numberExposures * exposureTime, numberExposures, exposureTime,
                    (exposureTime * numberExposures * result.observation().sourceFraction())));
        } else {
            _printRequestedIntegrationTime(result);
        }

        _println("");
        _println("The peak pixel signal + background on each detector is:");
        for (int i = 0; i <= 1; i++) {
            _println(String.format("%s: %.0f e- (%d ADU) which is %.0f%% of full well (%.0f e-) and %.0f%% of the linearity limit (%d e-).",
                    ((Igrins2) results[i].instrument()).getArm().getName(),
                    s.ccd(i).get().peakPixelFlux(), s.ccd(i).get().adu(),
                    s.ccd(i).get().percentFullWell(), s.ccd(i).get().wellDepth(),
                    (100. * s.ccd(i).get().peakPixelFlux() / ((Igrins2) results[i].instrument()).getArm().getLinearityLimit()),
                    ((Igrins2) results[i].instrument()).getArm().getLinearityLimit()));
        }

        _println("");
        _printWarnings(s.warnings());

        _print(OverheadTablePrinter.print(this, p, results[0], s));

        Log.info("Plotting results...");
        _print("<HR align=left SIZE=3>");
        _println("<p style=\"page-break-inside: never\">");

        // H signal
        _printImageLink(id, SignalChart.instance(), 0, pdp);
        _printFileLink(id, SignalData.instance(), 0, 0, " in H");
        _printFileLink(id, BackgroundData.instance(), 0, 0, " in H");
        _println("");

        // K signal
        _printImageLink(id, SignalChart.instance(), 1, pdp);
        _printFileLink(id, SignalData.instance(), 1, 0, " in K");
        _printFileLink(id, BackgroundData.instance(), 1, 0, " in K");
        _println("");

        // H S/N
        _printImageLink(id, S2NChart.instance(), 0, pdp);
        _printFileLink(id, SingleS2NData.instance(), 0, 0, " in H");
        _printFileLink(id, FinalS2NData.instance(), 0, 0, " in H");
        _println("");

        // K S/N
        _printImageLink(id, S2NChart.instance(), 1, pdp);
        _printFileLink(id, SingleS2NData.instance(), 1, 0, " in K");
        _printFileLink(id, FinalS2NData.instance(), 1, 0, " in K");
        _println("");

        printConfiguration(result.parameters(), mainInstrument, result.aoSystem(), iqAtSource);
        _println(HtmlPrinter.printParameterSummary(pdp));
    }

    private void printConfiguration(final ItcParameters p, final Igrins2 instrument, final Option<AOSystem> ao, final double iqAtSource) {
        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(p.source()));
        _println(igrins2ToString(instrument));
        if (ao.isDefined()) {
            _println(HtmlPrinter.printParameterSummary(p.telescope(), "altair"));
            _println(HtmlPrinter.printParameterSummary((Altair) ao.get()));
        } else {
            _println(HtmlPrinter.printParameterSummary(p.telescope()));
        }
        _println(HtmlPrinter.printParameterSummary(p.conditions(), instrument.getEffectiveWavelength(), iqAtSource));
        _println(HtmlPrinter.printParameterSummary(p.observation()));
    }

    private String igrins2ToString(final Igrins2 instrument) {
        String s = "Instrument configuration: \n";
        s += "Optical Components: <BR>";
        for (final TransmissionElement te : instrument.getComponents()) {
            s += "<LI>" + te.toString() + "<BR>";
        }
        s += "<LI>Read Mode: \n";
        s += "<LI>Detector Bias: \n";
        s += "<BR>Pixel Size: " + instrument.getPixelSize() + "<BR>";
        return s;
    }

    public ConfigCreator.ConfigCreatorResult createInstConfig(int numberExposures) {
        ConfigCreator cc = new ConfigCreator(p);
        return cc.createIgrins2Config(instr, numberExposures, recipe.getExposureTime());
    }

    public ItcOverheadProvider getInst() { return new edu.gemini.spModel.gemini.igrins2.Igrins2(); }

    @Override
    public double getReadoutTimePerCoadd() { return 0; }

    @Override
    public double getVisitTime() { return this.getVisit_time(); }

    @Override
    public double getRecenterInterval() { return this.getRecentInterval(); }

    @Override
    public int getNumberExposures() { return numberExposures; }

}
