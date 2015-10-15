package edu.gemini.itc.web.html;

import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.michelle.Michelle;
import edu.gemini.itc.michelle.MichelleRecipe;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.michelle.MichelleParams;
import edu.gemini.spModel.target.PointSource$;
import edu.gemini.spModel.target.UniformSource$;
import scala.Tuple2;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing Michelle calculation results to an output stream.
 */
public final class MichellePrinter extends PrinterBase {

    private final MichelleRecipe recipe;
    private final PlottingDetails pdp;
    private final boolean isImaging;

    public MichellePrinter(final Parameters p, final MichelleParameters ip, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe    = new MichelleRecipe(p.source(), p.observation(), p.conditions(), ip, p.telescope());
        this.pdp       = pdp;
        this.isImaging = p.observation().getMethod().isImaging();
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

        final Michelle instrument = (Michelle) result.instrument();

        _println("");

        if (!result.observation().isAutoAperture()) {
            _println(String.format("software aperture extent along slit = %.2f arcsec", result.observation().getApertureDiameter()));
        } else {
            if (result.source().profile() == UniformSource$.MODULE$) {
                _println(String.format("software aperture extent along slit = %.2f arcsec", 1 / instrument.getFPMask()));
            } else if (result.source().profile() == PointSource$.MODULE$) {
                _println(String.format("software aperture extent along slit = %.2f arcsec", 1.4 * result.iqCalc().getImageQuality()));
            }
        }

        if (!result.source().isUniform()) {
            _println(String.format("fraction of source flux in aperture = %.2f", result.st().getSlitThroughput()));
        }

        _println(String.format("derived image size(FWHM) for a point source = %.2farcsec\n", result.iqCalc().getImageQuality()));

        _println("Sky subtraction aperture = " + result.observation().getSkyApertureDiameter() + " times the software aperture.");

        _println("");

        final int number_exposures = result.observation().getNumExposures();
        final double frac_with_source = result.observation().getSourceFraction();
        final double exposure_time = result.observation().getExposureTime();
        if (instrument.polarimetryIsUsed()) {
            //Michelle polarimetry uses 4 waveplate positions so a single observation takes 4 times as long.
            //To the user it should appear as though the time used by the ITC matches thier requested time.
            //hence the x4 factor
            _println(String.format("Requested total integration time = %.2f secs, of which %.2f secs is on source.",
                    exposure_time * 4 * number_exposures, exposure_time * 4 * number_exposures * frac_with_source));
        } else {
            _println(String.format("Requested total integration time = %.2f secs, of which %.2f secs is on source.",
                    exposure_time * number_exposures, exposure_time * number_exposures * frac_with_source));
        }

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

        _println("");

        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.source()));
        _println(michelleToString(instrument, result.parameters()));
        _println(HtmlPrinter.printParameterSummary(result.telescope()));
        _println(HtmlPrinter.printParameterSummary(result.conditions()));
        _println(HtmlPrinter.printParameterSummary(result.observation()));
        _println(HtmlPrinter.printParameterSummary(pdp));

    }

    private void writeImagingOutput(final ImagingResult result) {

        final Michelle instrument = (Michelle) result.instrument();

        _println("");

        _print(CalculatablePrinter.getTextResult(result.sfCalc()));
        _println(CalculatablePrinter.getTextResult(result.iqCalc()));
        _println("Sky subtraction aperture = " + result.observation().getSkyApertureDiameter() + " times the software aperture.\n");

        if (!instrument.polarimetryIsUsed()) {
            _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation()));
        } else {
            _println("Polarimetry mode enabled.\n");
            final String result2 = CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation());
            final String delims = "[ ]+";
            final String[] tokens = result2.split(delims);
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].contains("Derived")) {
                    tokens[i + 5] = String.format("%.2f", new Double(tokens[i + 5]) * 4);
                    tokens[i + 9] = String.format("%.2f", new Double(tokens[i + 9]) * 4);
                }
                if (tokens[i].contains("Taking")) {
                    tokens[i + 1] = String.format("%.2f", new Double(tokens[i + 1]) * 4);
                }
                if (tokens[i].contains("Requested") || tokens[i].contains("Required")) {
                    tokens[i + 5] = String.format("%.2f", new Double(tokens[i + 5]) * 4);
                    tokens[i + 9] = String.format("%.2f", new Double(tokens[i + 9]) * 4);
                }
                _print(tokens[i] + " ");
            }
        }

        _println("");
        _println(String.format("The peak pixel signal + background is %.0f. ", result.peakPixelCount()));

        if (result.peakPixelCount() > (instrument.getWellDepth()))
            _println("Warning: peak pixel may be saturating the imaging deep well setting of " +
                    instrument.getWellDepth());


        _println("");

        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.source()));
        _println(michelleToString(instrument, result.parameters()));
        _println(HtmlPrinter.printParameterSummary(result.telescope()));
        _println(HtmlPrinter.printParameterSummary(result.conditions()));

        // Michelle polarimetry calculations include a x4 overhead of observing into the calculation
        // the following code applies this factor to all the needed values
        if (!instrument.polarimetryIsUsed()) {
            _println(HtmlPrinter.printParameterSummary(result.observation()));
        } else {
            final String result2 = HtmlPrinter.printParameterSummary(result.observation());
            final String delims = "[ ]+";
            final String[] tokens = result2.split(delims);
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].contains("<LI>Calculation") && tokens[i + 2].contains("S/N")) {
                    tokens[i + 5] = String.format("%.2f", new Double(tokens[i + 5]) * 4);
                }
                _print(tokens[i] + " ");

            }
        }

    }

    private String michelleToString(final Michelle instrument, final Parameters p) {

        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);

        if (instrument.getFocalPlaneMask() != MichelleParams.Mask.MASK_IMAGING) {
            s += "<LI> Focal Plane Mask: " + instrument.getFocalPlaneMask().displayValue();
        }
        s += "\n";
        s += "\n";
        if (p.observation().getMethod().isSpectroscopy())
            s += String.format("<L1> Central Wavelength: %.1f nm\n", instrument.getCentralWavelength());
        s += "Spatial Binning: 1\n";
        if (p.observation().getMethod().isSpectroscopy())
            s += "Spectral Binning: 1\n";
        s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + "arcsec\n";
        if (p.observation().getMethod().isSpectroscopy())
            s += "Pixel Size in Spectral Direction: " + instrument.getGratingDispersion_nmppix() + "nm\n";
        return s;
    }

}
