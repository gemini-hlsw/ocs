package edu.gemini.itc.web.html;

import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.Result;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.itc.flamingos2.Flamingos2;
import edu.gemini.itc.flamingos2.Flamingos2Recipe;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.FPUnit;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing F2 calculation results to an output stream.
 */
public final class Flamingos2Printer extends PrinterBase {

    private final PlottingDetails pdp;
    private final Flamingos2Recipe recipe;
    private final boolean isImaging;
    private final ItcParameters p;
    private final Flamingos2Parameters instr;

    private int step;
    private PlannedTime pta;
    private Config[] config;


    public Flamingos2Printer(final ItcParameters p, final Flamingos2Parameters instr, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.pdp       = pdp;
        this.recipe    = new Flamingos2Recipe(p, instr);
        this.isImaging = p.observation().calculationMethod() instanceof Imaging;
        this.p          = p;
        this.instr          = instr;
    }

    /**
     * Performs recipe calculation and writes results to a cached PrintWriter or to System.out.
     */
    public void writeOutput() {
        if (isImaging) {
            final ImagingResult result = recipe.calculateImaging();
            final ItcImagingResult s = recipe.serviceResult(result);
            writeImagingOutput(result, s);
        } else {
            final SpectroscopyResult r = recipe.calculateSpectroscopy();
            final ItcSpectroscopyResult s = recipe.serviceResult(r);
            final UUID id = cache(s);
            writeSpectroscopyOutput(id, r, s);
            validatePlottingDetails(pdp, r.instrument());
        }
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult result, final ItcSpectroscopyResult s) {

        // we know this is Flamingos
        final Flamingos2 instrument = (Flamingos2) result.instrument();

        _println("");

        _println(CalculatablePrinter.getTextResult(result.iqCalc()));

        _printSoftwareAperture(result, 1 / instrument.getSlitWidth());

        _println("");

        _printRequestedIntegrationTime(result);

        _println("");

        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

        getOverheadTableParams(result, result.observation());
        _println(_printOverheadTable(p, config[step], pta, step));

        _print("<HR align=left SIZE=3>");

        _println("<p style=\"page-break-inside: never\">");

        _printImageLink(id, SignalChart.instance(), pdp);
        _println("");

        _printFileLink(id, SignalData.instance());
        _printFileLink(id, BackgroundData.instance());

        _printImageLink(id, S2NChart.instance(), pdp);
        _println("");

        _printFileLink(id, SingleS2NData.instance());
        _printFileLink(id, FinalS2NData.instance());

        printConfiguration((Flamingos2) result.instrument(), result.parameters());

        _println(HtmlPrinter.printParameterSummary(pdp));

    }


    private void writeImagingOutput(final ImagingResult result, final ItcImagingResult s) {

        // we know this is Flamingos
        final Flamingos2 instrument = (Flamingos2) result.instrument();

        _println("");

        _print(CalculatablePrinter.getTextResult(result.sfCalc()));
        _println(CalculatablePrinter.getTextResult(result.iqCalc()));
        _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation()));

        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

        getOverheadTableParams(result, result.observation());
        _println(_printOverheadTable(p, config[step], pta, step));

        printConfiguration((Flamingos2) result.instrument(), result.parameters());
    }

    private void printConfiguration(final Flamingos2 instrument, final ItcParameters p) {
        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: Flamingos 2\n");
        _println(HtmlPrinter.printParameterSummary(p.source()));
        _println(flamingos2ToString(instrument));
        _println(HtmlPrinter.printParameterSummary(p.telescope()));
        _println(HtmlPrinter.printParameterSummary(p.conditions()));
        _println(HtmlPrinter.printParameterSummary(p.observation()));
    }

    private String flamingos2ToString(final Flamingos2 instrument) {
        String s = "Instrument configuration: \n";
        s += "Optical Components: <BR>";
        for (final TransmissionElement te : instrument.getComponents()) {
            s += "<LI>" + te.toString() + "<BR>";
        }
        s += "<LI>Read Noise: " + instrument.getReadNoiseString() + "\n";

        if (instrument.getFocalPlaneMask() != FPUnit.FPU_NONE)
            s += "<LI>Focal Plane Mask: " + instrument.getFocalPlaneMask().getSlitWidth() + " pix slit\n";

        s += "<BR>Pixel Size: " + instrument.getPixelSize() + "<BR>";

        return s;
    }

    public void getOverheadTableParams(Result result, ObservationDetails obs) {
        int numberExposures = 1;
        final CalculationMethod calcMethod = obs.calculationMethod();

        if (calcMethod instanceof ImagingInt) {
            numberExposures = (int)(((ImagingResult) result).is2nCalc().numberSourceExposures() * obs.sourceFraction());
        } else if (calcMethod instanceof ImagingS2N) {
            numberExposures = ((ImagingS2N) calcMethod).exposures();
        } else if (calcMethod instanceof SpectroscopyS2N) {
            numberExposures = ((SpectroscopyS2N) calcMethod).exposures();
        }

        ConfigCreator cc    = new ConfigCreator(p);
        config              = cc.createF2Config(instr, numberExposures);
        pta                 = PlannedTimeCalculator.instance.calc(config, new edu.gemini.spModel.gemini.flamingos2.Flamingos2());

        if (numberExposures < 2) step = 0; else step = 1;
    }


}
