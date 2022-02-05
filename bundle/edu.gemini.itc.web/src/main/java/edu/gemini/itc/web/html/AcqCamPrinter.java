package edu.gemini.itc.web.html;

import edu.gemini.itc.acqcam.AcqCamRecipe;
import edu.gemini.itc.acqcam.AcquisitionCamera;
import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.itc.shared.AcquisitionCamParameters;
import edu.gemini.itc.shared.ItcImagingResult;
import edu.gemini.itc.shared.ItcParameters;
import edu.gemini.itc.shared.ItcWarning;
import scala.collection.JavaConversions;

import java.io.PrintWriter;

/**
 * Helper class for printing Acquisition Camera calculation results to an output stream.
 */
public final class AcqCamPrinter extends PrinterBase {

    private final AcqCamRecipe  recipe;

    public AcqCamPrinter(final ItcParameters p, final AcquisitionCamParameters instr, final PrintWriter out) {
        super(out);
        recipe = new AcqCamRecipe(p, instr);
    }

    public void writeOutput() {
        final ImagingResult result = recipe.calculateImaging();
        final ItcImagingResult s = recipe.serviceResult(result);
        writeImagingOutput(result, s);
    }


    private void writeImagingOutput(final ImagingResult result, final ItcImagingResult s) {

        // we know this is the acq cam
        final AcquisitionCamera instrument = (AcquisitionCamera) result.instrument();
        final double iqAtSource = result.iqCalc().getImageQuality();

        _println("");

        _print(CalculatablePrinter.getTextResult(result.sfCalc()));
        _println(CalculatablePrinter.getTextResult(result.iqCalc()));
        _println(CalculatablePrinter.getTextResult(result.is2nCalc(), result.observation()));

        _println("");
        _printPeakPixelInfo(s.ccd(0));
        _printWarnings(s.warnings());

        printConfiguration(result, instrument, iqAtSource);

    }


    private void printConfiguration(final ImagingResult result, final AcquisitionCamera instrument, double iqAtSource){

        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + result.instrument().getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.parameters().source()));
        _println(acqCamToString(result.instrument()));
        _println(HtmlPrinter.printParameterSummary(result.parameters().telescope()));
        _println(HtmlPrinter.printParameterSummary(result.parameters().conditions(), instrument.getEffectiveWavelength(), iqAtSource));
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
