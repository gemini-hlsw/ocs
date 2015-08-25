package edu.gemini.itc.web.html;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.AOSystem;
import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.itc.niri.Niri;
import edu.gemini.itc.niri.NiriRecipe;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.shared.SourceDefinition.Profile;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.niri.Niri.Mask;
import scala.Option;
import scala.Tuple2;
import scala.collection.JavaConversions;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing NIRI calculation results to an output stream.
 */
public final class NiriPrinter extends PrinterBase {

    private final PlottingDetails pdp;
    private final NiriRecipe recipe;
    private final boolean isImaging;

    /**
     * Constructs a NiriRecipe given the parameters. Useful for testing.
     */
    public NiriPrinter(final Parameters p, final NiriParameters ip, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe    = new NiriRecipe(p.source(), p.observation(), p.conditions(), ip, p.telescope());
        this.isImaging = p.observation().getMethod().isImaging();
        this.pdp       = pdp;
    }

    /**
     * Then name of the instrument this recipe belongs to.
     * @return
     */
    public String getInstrumentName() {
        return SPComponentType.INSTRUMENT_NIRI.readableStr;
    }

    public void writeOutput() {
        if (isImaging) {
            final ImagingResult result = recipe.calculateImaging();
            writeImagingOutput(result);
        } else {
            final Tuple2<ItcSpectroscopyResult, SpectroscopyResult> r = recipe.calculateSpectroscopy();
            final UUID id = cache(r._1());
            writeSpectroscopyOutput(id, r._2());
        }
    }

    private void writeSpectroscopyOutput(final UUID id, final SpectroscopyResult result) {

        final Niri instrument = (Niri) result.instrument();

        _println("");

        // Altair specific section
        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printSummary((Altair) result.aoSystem().get()));
        }

        if (!result.observation().isAutoAperture()) {
            _println(String.format("software aperture extent along slit = %.2f arcsec", result.observation().getApertureDiameter()));
        } else {
            if (result.source().getProfileType() == Profile.UNIFORM) {
                _println(String.format("software aperture extent along slit = %.2f arcsec", 1 / instrument.getFPMask()));
            } else if (result.source().getProfileType() == Profile.POINT) {
                _println(String.format("software aperture extent along slit = %.2f arcsec", 1.4 * result.specS2N()[0].getImageQuality()));
            }
        }

        if (!result.source().isUniform()) {
            _println(String.format("fraction of source flux in aperture = %.2f", result.st().getSlitThroughput()));
        }

        _println(String.format("derived image size(FWHM) for a point source = %.2f arcsec", result.specS2N()[0].getImageQuality()));

        _println("");
        _println(String.format(
                "Requested total integration time = %.2f secs, of which %.2f secs is on source.",
                result.observation().getExposureTime() * result.observation().getNumExposures(),
                result.observation().getExposureTime() * result.observation().getNumExposures() * result.observation().getSourceFraction()));

        _print("<HR align=left SIZE=3>");

        _println("<p style=\"page-break-inside: never\">");

        _printImageLink(id, SignalChart.instance(), pdp);
        _println("");

        _printFileLink(id,  SignalData.instance());
        _printFileLink(id,  BackgroundData.instance());

        _printImageLink(id, S2NChart.instance(), pdp);
        _println("");

        _printFileLink(id,  SingleS2NData.instance());
        _printFileLink(id,  FinalS2NData.instance());

        printConfiguration(result.parameters(), instrument, result.aoSystem());

        _println(HtmlPrinter.printParameterSummary(pdp));

    }

    private void writeImagingOutput(final ImagingResult result) {

        final Niri instrument = (Niri) result.instrument();

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

        _println("");
        _println(String.format("The peak pixel signal + background is %.0f. This is %.0f%% of the full well depth of %.0f.",
                result.peakPixelCount(), result.peakPixelCount() / instrument.getWellDepthValue() * 100, instrument.getWellDepthValue()));

        for (final ItcWarning warning : JavaConversions.asJavaList(result.warnings())) {
            _println(warning.msg());
        }

        _println("");

        printConfiguration(result.parameters(), instrument, result.aoSystem());

    }

    private void printConfiguration(final Parameters p, final Niri instrument, final Option<AOSystem> ao) {
        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(p.source()));
        _println(niriToString(instrument));
        if (ao.isDefined()) {
            _println(HtmlPrinter.printParameterSummary(p.telescope(), "altair"));
            _println(HtmlPrinter.printParameterSummary((Altair) ao.get()));
        } else {
            _println(HtmlPrinter.printParameterSummary(p.telescope()));
        }

        _println(HtmlPrinter.printParameterSummary(p.conditions()));
        _println(HtmlPrinter.printParameterSummary(p.observation()));
    }

    private String niriToString(final Niri instrument) {
        String s = "Instrument configuration: \n";
        s += "Optical Components: <BR>";
        for (final TransmissionElement te : instrument.getComponents()) {
            s += "<LI>" + te.toString() + "<BR>";
        }
        if (instrument.getFocalPlaneMask() != Mask.MASK_IMAGING)
            s += "<LI>Focal Plane Mask: " + instrument.getFocalPlaneMask().displayValue() + "\n";
        s += "<LI>Read Mode: " + instrument.getReadMode().displayValue() + "\n";
        s += "<LI>Detector Bias: " + instrument.getWellDepth().displayValue() + "\n";

        s += "<BR>Pixel Size: " + instrument.getPixelSize() + "<BR>";

        return s;
    }


}
