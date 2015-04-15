package edu.gemini.itc.web.html;

import edu.gemini.itc.acqcam.AcqCamRecipe;
import edu.gemini.itc.acqcam.AcquisitionCamera;
import edu.gemini.itc.shared.*;

import java.io.PrintWriter;

/**
 * Helper class for printing Acquisition Camera calculation results to an output stream.
 */
public final class AcqCamPrinter extends PrinterBase {

    private final AcqCamRecipe  recipe;

    public AcqCamPrinter(final Parameters p, final AcquisitionCamParameters ip, final PrintWriter out) {
        super(out);
        recipe = new AcqCamRecipe(p.source(), p.observation(), p.conditions(), p.telescope(), ip);
    }

    public void writeOutput() {
        final ImagingResult result = recipe.calculateImaging();
        writeImagingOutput(result);
    }


    private void writeImagingOutput(final ImagingResult result) {

        // we know this is the acq cam
        final AcquisitionCamera instrument = (AcquisitionCamera) result.instrument();

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();
        _println("");

        _print(CalculatablePrinter.getTextResult(result.sfCalc(), device));
        _println(CalculatablePrinter.getTextResult(result.iqCalc(), device));
        _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation(), device));

        device.setPrecision(0);  // NO decimal places
        device.clear();

        _println("");
        _println("The peak pixel signal + background is " + device.toString(result.peakPixelCount()) + ". This is " +
                device.toString(result.peakPixelCount() / instrument.getWellDepth() * 100) +
                "% of the full well depth of " + device.toString(instrument.getWellDepth()) + ".");

        if (result.peakPixelCount() > (.8 * instrument.getWellDepth()))
            _println("Warning: peak pixel exceeds 80% of the well depth and may be saturated");

        _println("");
        device.setPrecision(2);  // TWO decimal places
        device.clear();

        printConfiguration(result);

    }


    private void printConfiguration(final ImagingResult result) {
        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + result.instrument().getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.parameters().source()));
        _println(acqCamToString(result.instrument()));
        _println(HtmlPrinter.printParameterSummary(result.parameters().telescope()));
        _println(HtmlPrinter.printParameterSummary(result.parameters().conditions()));
        _println(HtmlPrinter.printParameterSummary(result.parameters().observation()));
    }

    private String acqCamToString(final Instrument instrument) {
        String s = "Instrument configuration: \n";
        s += "Optical Components: <BR>";
        for (final TransmissionElement te : instrument.getComponents()) {
            s += "<LI>" + te.toString() + "<BR>";
        }
        s += "<BR>";
        s += "Pixel Size: " + instrument.getPixelSize() + "<BR>" + "<BR>";

        return s;
    }
}
