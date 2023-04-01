package edu.gemini.itc.web.html;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.AOSystem;
import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.Result;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.gnirs.Gnirs;
import edu.gemini.itc.gnirs.GnirsRecipe;
import edu.gemini.itc.gnirs.IFUComponent;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.gnirs.*;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator;
import edu.gemini.spModel.obscomp.ItcOverheadProvider;
import scala.Option;

import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.UUID;

/**
 * Helper class for printing GNIRS calculation results to an output stream.
 */
public final class GnirsPrinter extends PrinterBase implements OverheadTablePrinter.PrinterWithOverhead {

    private static final Logger Log = Logger.getLogger(GnirsPrinter.class.getName());
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
            final ItcSpectroscopyResult s = recipe.serviceResult(r, false);
            final UUID id = cache(s);
            writeSpectroscopyOutput(id, r, s);
        }
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult result, final ItcSpectroscopyResult s) {

        final Gnirs instrument = (Gnirs) result.instrument();
        final double iqAtSource = result.iqCalc().getImageQuality();

        _println("");

        if (! instrument.isIfuUsed()) {
            _printSoftwareAperture(result, 1 / instrument.getSlitWidth());
        }

        // Altair specific section
        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printSummary((Altair) result.aoSystem().get()));
            _println(String.format("derived image halo size (FWHM) for a point source = %.2f arcsec.\n", iqAtSource));
        } else {
            _println(String.format("derived image size(FWHM) for a point source = %.2f arcsec\n", iqAtSource));
        }

        if (! instrument.isIfuUsed()) {
            _printSkyAperture(result);
            _println("");
        }

        _printRequestedIntegrationTime(result);

        _println("");

        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

        _print(OverheadTablePrinter.print(this, p, getReadoutTimePerCoadd(),result));

        _print("<HR align=left SIZE=3>");

        _println("<p style=\"page-break-inside: never\">");

        if (instrument.XDisp_IsUsed()) {
            Log.fine("Generating XD output...");
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
            Log.fine("Generating non-XD (long-slit and IFU) output...");
            for (int i = 0; i < result.specS2N().length; i++) {
                // _println("<p style=\"page-break-inside: never\">");  // causes tests to fail
                _printImageLink(id, SignalChart.instance(),   i, pdp);
                _println("");
                _printFileLink(id, SignalData.instance(),     i);
                _printFileLink(id, BackgroundData.instance(), i);
                _printImageLink(id, S2NChart.instance(),      i, pdp);
                _println("");
                _printFileLink(id, SingleS2NData.instance(),  i);
                _printFileLink(id, FinalS2NData.instance(),   i);
            }

        }

        printConfiguration(result.parameters(), instrument, result.aoSystem(), iqAtSource);

        _println(HtmlPrinter.printParameterSummary(pdp));

    }


    private void writeImagingOutput(final ImagingResult result, final ItcImagingResult s) {

        final Gnirs instrument = (Gnirs) result.instrument();
        final double iqAtSource = result.iqCalc().getImageQuality();

        _println("");

        // Altair specific section
        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printSummary((Altair) result.aoSystem().get()));
            _print(CalculatablePrinter.getTextResult(result.sfCalc(), false));
            _println(String.format("derived image halo size (FWHM) for a point source = %.2f arcsec.\n", iqAtSource));
        } else {
            _print(CalculatablePrinter.getTextResult(result.sfCalc()));
            _println(CalculatablePrinter.getTextResult(result.iqCalc()));
        }

        _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation()));

        //_println(CalculatablePrinter.getBackgroundLimitResult(result.is2nCalc()));
        CalculatablePrinter.ReadNoiseLimitedWarning(result.is2nCalc()).foreach(str -> _println(str));

        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

        _print(OverheadTablePrinter.print(this, p, getReadoutTimePerCoadd(),result));

        printConfiguration(result.parameters(), instrument, result.aoSystem(), iqAtSource);

    }

    private void printConfiguration(final ItcParameters p, final Gnirs instrument, final Option<AOSystem> ao, final double iqAtSource) {
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

        _println(HtmlPrinter.printParameterSummary(p.conditions(), instrument.getEffectiveWavelength(), iqAtSource));
        _println(printParameterSummary(p.observation(), instrument));
    }

    public static String printParameterSummary(final ObservationDetails odp, final Gnirs instrument) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Calculation and analysis methods:\n");
        sb.append("<LI>Mode: ");
        sb.append((odp.calculationMethod() instanceof Imaging ? "imaging" : "spectroscopy"));
        sb.append("\n");
        sb.append("<LI>Calculation of ");
        if (odp.calculationMethod() instanceof S2NMethod) {
            sb.append(String.format("S/N ratio with %d", ((S2NMethod) odp.calculationMethod()).exposures()));
        } else {
            sb.append(String.format("integration time from a S/N ratio of %.2f for", ((ImagingInt) odp.calculationMethod()).sigma()));
        }
        sb.append(String.format(" exposures of %.2f secs", odp.exposureTime()));
        if (odp.calculationMethod().coaddsOrElse(1) > 1) {
            sb.append(String.format(" and %d coadds", odp.calculationMethod().coaddsOrElse(1)));
        }
        sb.append(String.format(", and %.2f%% of them on source.\n", odp.sourceFraction() * 100));

        sb.append("<LI>Analysis performed ");
        if (odp.analysisMethod() instanceof AutoAperture) {
            sb.append("for aperture that gives 'optimum' S/N ");
            sb.append(String.format("and a sky aperture that is %.2f times the target aperture.\n", ((AutoAperture) odp.analysisMethod()).skyAperture()));

        } else if (odp.analysisMethod() instanceof UserAperture) {
            sb.append(String.format("for aperture of diameter %.2f ", ((UserAperture) odp.analysisMethod()).diameter()));
            sb.append(String.format("and a sky aperture that is %.2f times the target aperture.\n", ((UserAperture) odp.analysisMethod()).skyAperture()));

        } else if (odp.analysisMethod() instanceof IfuMethod) {

            if (instrument.getIFUMethod() instanceof IfuSingle) {
                sb.append("on a single IFU element at " + instrument.getIFUOffset() + " arcseconds from the center.\n");

            } else if (instrument.getIFUMethod() instanceof IfuSummed) {
                sb.append(String.format("on %d x %d (%.3f\" x %.3f\") summed IFU elements.\n",
                        instrument.getIFUNumX(),
                        instrument.getIFUNumY(),
                        instrument.getIFUNumX() * IFUComponent.ifuElementSize,
                        instrument.getIFUNumY() * IFUComponent.ifuElementSize));

            } else {
                sb.append("on multiple IFU elements arranged from " +
                        instrument.getIFUMinOffset() + " to " + instrument.getIFUMaxOffset() + " arcsecs.\n");
            }

        } else {
            throw new Error("Unsupported analysis method");
        }

        return sb.toString();
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
                s += String.format("Pixel Size in Spectral Direction: %.3f nm\n",
                        instrument.getGratingDispersion() / instrument.getOrder());
            }

            if (instrument.isIfuUsed()) {
                if (instrument.getIFUMethod() instanceof IfuSingle) {
                    s += "IFU: a single element at " + instrument.getIFUOffset() + " arcsecs.\n";
                } else if (instrument.getIFUMethod() instanceof IfuSummed) {
                    s += String.format("IFU: multiple elements summed in a " +
                                    instrument.getIFUNumX() + "x" + instrument.getIFUNumY() +
                                    " (%.3f\"x%.3f\") grid.\n",
                            instrument.getIFUNumX() * IFUComponent.ifuElementSize,
                            instrument.getIFUNumY() * IFUComponent.ifuElementSize);
                } else {
                    s += "IFU: multiple elements arranged from " +
                            instrument.getIFUMinOffset() + " to " + instrument.getIFUMaxOffset() + " arcsecs.\n";
                }
            }

        } else {  // Imaging
            s += "Pixel Size: " + instrument.getPixelSize() + " arcsec\n";
        }
        return s;
    }

    public ConfigCreator.ConfigCreatorResult createInstConfig(int numberExposures) {
        ConfigCreator cc = new ConfigCreator(p);
        return cc.createGnirsConfig(instr, numberExposures);
    }

    public ItcOverheadProvider getInst() {
        return new InstGNIRS();
    }

    public double getReadoutTimePerCoadd() {
        return GnirsReadoutTime.getReadoutOverheadPerCoadd(instr.readMode());
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
