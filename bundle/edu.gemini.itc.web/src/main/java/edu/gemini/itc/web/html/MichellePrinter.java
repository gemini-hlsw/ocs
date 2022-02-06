package edu.gemini.itc.web.html;

import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.michelle.Michelle;
import edu.gemini.itc.michelle.MichelleRecipe;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.michelle.MichelleParams;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Helper class for printing Michelle calculation results to an output stream.
 */
public final class MichellePrinter extends PrinterBase {

    private final MichelleRecipe recipe;
    private final PlottingDetails pdp;
    private final boolean isImaging;

    public MichellePrinter(final ItcParameters p, final MichelleParameters instr, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe    = new MichelleRecipe(p, instr);
        this.pdp       = pdp;
        this.isImaging = p.observation().calculationMethod() instanceof Imaging;
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

        final Michelle instrument = (Michelle) result.instrument();
        final double iqAtSource = result.iqCalc().getImageQuality();

        _println("");

        _printSoftwareAperture(result, 1 / instrument.getSlitWidth());

        _println(String.format("derived image size(FWHM) for a point source = %.2farcsec\n", iqAtSource));

        _printSkyAperture(result);

        _println("");

        if (instrument.polarimetryIsUsed()) {
            //Michelle polarimetry uses 4 waveplate positions so a single observation takes 4 times as long.
            //To the user it should appear as though the time used by the ITC matches thier requested time.
            //hence the x4 factor
            _printRequestedIntegrationTime(result, 4);
        } else {
            _printRequestedIntegrationTime(result);
        }

        _println("");

        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

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
        _println(HtmlPrinter.printParameterSummary(result.conditions(), instrument.getEffectiveWavelength(), iqAtSource));
        _println(HtmlPrinter.printParameterSummary(result.observation()));
        _println(HtmlPrinter.printParameterSummary(pdp));

    }

    private void writeImagingOutput(final ImagingResult result, final ItcImagingResult s) {

        final Michelle instrument = (Michelle) result.instrument();
        final double iqAtSource = result.iqCalc().getImageQuality();

        _println("");

        _print(CalculatablePrinter.getTextResult(result.sfCalc()));
        _println(CalculatablePrinter.getTextResult(result.iqCalc()));
        _printSkyAperture(result);

        if (!instrument.polarimetryIsUsed()) {
            _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation()));
        } else {
            _println("Polarimetry mode enabled.\n");
            final String result2 = CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation());
            final String delims = "[ ,]+";
            final String[] tokens = result2.split(delims);
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].contains("Derived")) {
                    tokens[i + 5] = String.format("%.2f", Double.valueOf(tokens[i + 5]) * 4);
                    if (tokens[i + 6].contains("each"))
                        tokens[i + 12] = String.format("%.2f", Double.valueOf(tokens[i + 12]) * 4);
                    else
                        tokens[i + 8] = String.format("%.2f", Double.valueOf(tokens[i + 8]) * 4);
                }
                if (tokens[i].contains("Taking")) {
                    tokens[i + 1] = String.format("%.2f", Double.valueOf(tokens[i + 1]) * 4);
                }
                if (tokens[i].contains("Requested") || tokens[i].contains("Required")) {
                    tokens[i + 5] = String.format("%.2f", Double.valueOf(tokens[i + 5]) * 4);
                    tokens[i + 9] = String.format("%.2f", Double.valueOf(tokens[i + 9]) * 4);
                }
                _print(tokens[i] + " ");
            }
        }

        _println("");
        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.source()));
        _println(michelleToString(instrument, result.parameters()));
        _println(HtmlPrinter.printParameterSummary(result.telescope()));
        _println(HtmlPrinter.printParameterSummary(result.conditions(), instrument.getEffectiveWavelength(), iqAtSource));

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

    private String michelleToString(final Michelle instrument, final ItcParameters p) {

        String s = "Instrument configuration: \n";
        s += HtmlPrinter.opticalComponentsToString(instrument);

        if (instrument.getFocalPlaneMask() != MichelleParams.Mask.MASK_IMAGING) {
            s += "<LI> Focal Plane Mask: " + instrument.getFocalPlaneMask().displayValue();
        }
        s += "\n";
        s += "\n";
        if (p.observation().calculationMethod() instanceof Spectroscopy)
            s += String.format("<L1> Central Wavelength: %.1f nm\n", instrument.getCentralWavelength());
        s += "Spatial Binning: 1\n";
        if (p.observation().calculationMethod() instanceof Spectroscopy)
            s += "Spectral Binning: 1\n";
        s += "Pixel Size in Spatial Direction: " + instrument.getPixelSize() + "arcsec\n";
        if (p.observation().calculationMethod() instanceof Spectroscopy)
            s += "Pixel Size in Spectral Direction: " + instrument.getGratingDispersion() + "nm\n";
        return s;
    }

}
