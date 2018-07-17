package edu.gemini.itc.web.html;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.AOSystem;
import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.Result;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.gnirs.Gnirs;
import edu.gemini.itc.gnirs.GnirsRecipe;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.gnirs.*;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator;
import scala.Option;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing GNIRS calculation results to an output stream.
 */
public final class GnirsPrinter extends PrinterBase implements OverheadTablePrinter.PrinterWithOverhead {

    private final GnirsParameters instr;
    private final PlottingDetails pdp;
    private final GnirsRecipe recipe;
    private final boolean isImaging;
    private final ItcParameters p;

    public GnirsPrinter(final ItcParameters p, final GnirsParameters instr, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.instr      = instr;
        this.recipe     = new GnirsRecipe(p, instr);
        this.isImaging  = p.observation().calculationMethod() instanceof Imaging;
        this.pdp        = pdp;
        this.p          = p;
    }

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
        }
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult result, final ItcSpectroscopyResult s) {

        final Gnirs instrument = (Gnirs) result.instrument();

        _println("");

        _printSoftwareAperture(result, 1 / instrument.getSlitWidth());

        // Altair specific section
        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printSummary((Altair) result.aoSystem().get()));
            _println(String.format("derived image halo size (FWHM) for a point source = %.2f arcsec.\n", result.iqCalc().getImageQuality()));
        } else {
            _println(String.format("derived image size(FWHM) for a point source = %.2f arcsec\n", result.iqCalc().getImageQuality()));
        }

        _printSkyAperture(result);

        _println("");

        _printRequestedIntegrationTime(result);

        _println("");

        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

        _print(OverheadTablePrinter.print(this, p, getReadoutTimePerCoadd(),result));

        _print("<HR align=left SIZE=3>");

        _println("<p style=\"page-break-inside: never\">");

        if (instrument.XDisp_IsUsed()) {

            _printImageLink(id, SignalChart.instance(), pdp);
            _println("");

            for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
                _printFileLink(id, SignalData.instance(), 0, i, "Order " + (i+3));
            }
            for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
                _printFileLink(id, BackgroundData.instance(), 0, i, "Order " + (i+3));
            }

            _printImageLink(id, S2NChart.instance(), pdp);
            _println("");


            for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
                _printFileLink(id, FinalS2NData.instance(), 0, i, "Order " + (i+3));

            }


        } else {

            _printImageLink(id, SignalChart.instance(), pdp);
            _println("");

            _printFileLink(id, SignalData.instance());
            _printFileLink(id, BackgroundData.instance());
            _printImageLink(id, S2NChart.instance(), pdp);
            _println("");

            _printFileLink(id, SingleS2NData.instance());
            _printFileLink(id, FinalS2NData.instance());

           // printConfiguration(result.parameters(), instrument, result.aoSystem());

           // _println(HtmlPrinter.printParameterSummary(pdp));
        }
// in separate method now
        /*_println("");

        _print("<HR align=left SIZE=3>");

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.source()));
        _println(gnirsToString(instrument, result.parameters()));
        _println(HtmlPrinter.printParameterSummary(result.telescope()));
        _println(HtmlPrinter.printParameterSummary(result.conditions()));
        _println(HtmlPrinter.printParameterSummary(result.observation()));
        _println(HtmlPrinter.printParameterSummary(pdp)); */
        printConfiguration(result.parameters(), instrument, result.aoSystem());

        _println(HtmlPrinter.printParameterSummary(pdp));

    }


    private void writeImagingOutput(final ImagingResult result, final ItcImagingResult s) {

        final Gnirs instrument = (Gnirs) result.instrument();

        _println("");

        // Altair specific section
        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printSummary((Altair) result.aoSystem().get()));
            _print(CalculatablePrinter.getTextResult(result.sfCalc(), false));
            _println(String.format("derived image halo size (FWHM) for a point source = %.2f arcsec.\n", result.iqCalc().getImageQuality()));
        } else {
            _print(CalculatablePrinter.getTextResult(result.sfCalc()));
            _println(CalculatablePrinter.getTextResult(result.iqCalc()));
        }

        _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation()));
        _println(CalculatablePrinter.getBackgroundLimitResult(result.is2nCalc()));

        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());


        _print(OverheadTablePrinter.print(this, p, getReadoutTimePerCoadd(),result));


        printConfiguration(result.parameters(), instrument, result.aoSystem());

    }

    private void printConfiguration(final ItcParameters p, final Gnirs instrument, final Option<AOSystem> ao) {
        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(p.source()));
        _println(gnirsToString(instrument, p));
        if (ao.isDefined()) {
            _println(HtmlPrinter.printParameterSummary(p.telescope(), "altair"));
            _println(HtmlPrinter.printParameterSummary((Altair) ao.get()));
        } else {
            _println(HtmlPrinter.printParameterSummary(p.telescope()));
        }

        _println(HtmlPrinter.printParameterSummary(p.conditions()));
        _println(HtmlPrinter.printParameterSummary(p.observation()));
    }

    private String gnirsToString(final Gnirs instrument, final ItcParameters p) {

        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);

        if (p.observation().calculationMethod() instanceof Spectroscopy) {
            s += "<LI>Focal Plane Mask: " + instrument.getFocalPlaneMask().displayValue() + "\n";
            s += "<LI>Grating: " + instrument.getGrating().displayValue() + "\n"; // REL-469
        }

        s += "<LI>Read Noise: " + instrument.getReadNoise() + "\n";
        s += "<LI>Well Depth: " + instrument.getWellDepth() + "\n";
        s += "\n";

        if (p.observation().calculationMethod() instanceof Spectroscopy) {
            if (instrument.XDisp_IsUsed())
                s += String.format("<L1> Central Wavelength: %.1f nm\n", instrument.getCentralWavelengthXD());
            else
                s += String.format("<L1> Central Wavelength: %.1f nm\n", instrument.getCentralWavelength());
            s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + " arcsec\n";
            if (instrument.XDisp_IsUsed()) {
                s += String.format("Pixel Size in Spectral Direction(Order 3): %.3f nm\n", instrument.getGratingDispersion() / 3);
                s += String.format("Pixel Size in Spectral Direction(Order 4): %.3f nm\n", instrument.getGratingDispersion() / 4);
                s += String.format("Pixel Size in Spectral Direction(Order 5): %.3f nm\n", instrument.getGratingDispersion() / 5);
                s += String.format("Pixel Size in Spectral Direction(Order 6): %.3f nm\n", instrument.getGratingDispersion() / 6);
                s += String.format("Pixel Size in Spectral Direction(Order 7): %.3f nm\n", instrument.getGratingDispersion() / 7);
                s += String.format("Pixel Size in Spectral Direction(Order 8): %.3f nm\n", instrument.getGratingDispersion() / 8);
            } else {
                s += String.format("Pixel Size in Spectral Direction: %.3f nm\n", instrument.getGratingDispersion() / instrument.getOrder());
            }
        } else {
            s += "Pixel Size: " + instrument.getPixelSize() + " arcsec\n";
        }
        return s;
    }

    public ConfigCreator.ConfigCreatorResult createInstConfig(int numberExposures) {
        ConfigCreator cc = new ConfigCreator(p);
        return cc.createGnirsConfig(instr, numberExposures);
    }

    public PlannedTime.ItcOverheadProvider getInst() {
        return new InstGNIRS();
    }

    public double getReadoutTimePerCoadd() {
        return GnirsReadoutTime.getReadoutOverheadPerCoadd(instr.readMode());
    }

}
