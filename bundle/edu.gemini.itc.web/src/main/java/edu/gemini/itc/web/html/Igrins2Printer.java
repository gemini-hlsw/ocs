package edu.gemini.itc.web.html;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.*;
import edu.gemini.itc.igrins2.Igrins2;
import edu.gemini.itc.igrins2.Igrins2Recipe;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator;
import edu.gemini.spModel.obscomp.ItcOverheadProvider;
import scala.Option;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Helper class for printing IGRINS-2 calculation results to an output stream.
 */
public final class Igrins2Printer extends PrinterBase implements OverheadTablePrinter.PrinterWithOverhead {

    private static final Logger Log = Logger.getLogger(Igrins2Printer.class.getName());
    private final Igrins2Parameters instr;
    private final PlottingDetails pdp;
    private final Igrins2Recipe recipe;
    private final ItcParameters p;

    public Igrins2Printer(final ItcParameters p, final Igrins2Parameters instr, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.instr     = instr;
        this.recipe    = new Igrins2Recipe(p, instr);
        this.pdp       = pdp;
        this.p          = p;
    }

    public void writeOutput() {
        final SpectroscopyResult[] r = recipe.calculateSpectroscopy();
        final ItcSpectroscopyResult s = recipe.serviceResult(r, false);
        final UUID id = cache(s);
        writeSpectroscopyOutput(id, r, s);
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult[] results, final ItcSpectroscopyResult s) {

        final Igrins2 mainInstrument = (Igrins2) results[0].instrument();

         final SpectroscopyResult result = results[0];

         final double iqAtSource = result.iqCalc().getImageQuality();
        _println("");

        for (int i = 0; i < results.length; i++) {
            _println("Read noise (" + ((Igrins2) results[i].instrument()).getWaveBand() + "): " + results[i].instrument().getReadNoise() + " electrons");
        }

        if (result.aoSystem().isDefined()) { _println(HtmlPrinter.printSummary((Altair) result.aoSystem().get())); }

        _printSoftwareAperture(result, 1 / mainInstrument.getSlitWidth());
        _println(String.format("derived image size(FWHM) for a point source = %.2f arcsec", iqAtSource));
        _println("");
        _printRequestedIntegrationTime(result);
        _println("");
        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

        _print(OverheadTablePrinter.print(this, p, results[0], s));

        Log.info("Plotting results...");
        _print("<HR align=left SIZE=3>");
        _println("<p style=\"page-break-inside: never\">");

        // H signal
        _printImageLink(id, SignalChart.instance(), 0, pdp);
        _printFileLink(id, SignalData.instance(), 0);
        _printFileLink(id, BackgroundData.instance(), 0);
        _println("");

        // K signal
        _printImageLink(id, SignalChart.instance(), 1, pdp);
        _printFileLink(id, SignalData.instance(), 1);
        _printFileLink(id, BackgroundData.instance(), 1);
        _println("");

        // H S/N
        _printImageLink(id, S2NChart.instance(), 0, pdp);
        _printFileLink(id, SingleS2NData.instance(), 0);
        _printFileLink(id, FinalS2NData.instance(), 0);
        _println("");

        // K S/N
        _printImageLink(id, S2NChart.instance(), 1, pdp);
        _printFileLink(id, SingleS2NData.instance(), 1);
        _printFileLink(id, FinalS2NData.instance(), 1);
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
        return cc.createIgrins2Config(instr, numberExposures);
    }

    public ItcOverheadProvider getInst() { return new edu.gemini.spModel.gemini.igrins2.Igrins2(); }

    public double getReadoutTimePerCoadd() {
        // Igrins2ReadoutTime rt = Igrins2ReadoutTime.lookup(instr.builtinROI(), instr.readMode()).getValue();
        //return rt.getReadout(1);
        return 1;
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
