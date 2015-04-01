package edu.gemini.itc.web.html;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.altair.AltairParameters;
import edu.gemini.itc.nifs.Nifs;
import edu.gemini.itc.nifs.NifsNorth;
import edu.gemini.itc.nifs.NifsParameters;
import edu.gemini.itc.nifs.NifsRecipe;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.HtmlPrinter;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.spModel.core.Site;
import org.jfree.chart.plot.Plot;
import scala.Option;

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class for printing NIFS calculation results to an output stream.
 */
public final class NifsPrinter extends PrinterBase {

    private final NifsRecipe recipe;
    private final PlottingDetails pdp;

    public NifsPrinter(final Parameters p, final NifsParameters ip, final AltairParameters altair, final PlottingDetails pdp, final PrintWriter out) {
        super(out);
        this.recipe = new NifsRecipe(p.source(), p.observation(), p.conditions(), ip, p.telescope(), altair);
        this.pdp    = pdp;
    }

    public void writeOutput() {
        final SpectroscopyResult result = recipe.calculateSpectroscopy();
        writeSpectroscopyOutput(result);
    }

    private void writeSpectroscopyOutput(final SpectroscopyResult result) {

        final NifsNorth instrument = (NifsNorth) result.instrument();

        _println("");
        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();

        // TODO : THIS IS PURELY FOR REGRESSION TEST ONLY, REMOVE ASAP
        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource0 = SEDFactory.calculate(instrument, Site.GN, ITCConstants.NEAR_IR, result.source(), result.conditions(), result.telescope());
        final VisitableSampledSpectrum sed0 = calcSource0.sed;
        final VisitableSampledSpectrum sky0 = calcSource0.sky;
        final double sed_integral0 = sed0.getIntegral();
        final double sky_integral0 = sky0.getIntegral();
        // Update this in (or remove from) regression test baseline:
        _println("SED Int: " + sed_integral0 + " Sky Int: " + sky_integral0);
        // TODO : THIS IS PURELY FOR REGRESSION TEST ONLY, REMOVE ASAP

        if (result.aoSystem().isDefined()) {
            _println(((Altair) result.aoSystem().get()).printSummary());
        }

        final int number_exposures = result.observation().getNumExposures();
        final double frac_with_source = result.observation().getSourceFraction();
        final double exposure_time = result.observation().getExposureTime();

        _println("derived image halo size (FWHM) for a point source = " + device.toString(result.iqCalc().getImageQuality()) + "arcsec\n");
        _println("Requested total integration time = " +
                device.toString(exposure_time * number_exposures) +
                " secs, of which " + device.toString(exposure_time *
                number_exposures *
                frac_with_source) +
                " secs is on source.");

        _print("<HR align=left SIZE=3>");

        String sigSpec = null, backSpec = null, singleS2N = null, finalS2N = null;

        final List<Double> ap_offset_list = instrument.getIFU().getApertureOffsetList();
        final Iterator<Double> ifu_offset_it = ap_offset_list.iterator();
        for (int i = 0; i < result.specS2N().length; i++) {
            _println("<p style=\"page-break-inside: never\">");
            device.setPrecision(3);  // NO decimal places
            device.clear();

            final double ifu_offset = ifu_offset_it.next();

            final String chart1Title =
                    instrument.getIFUMethod().equals(NifsParameters.SUMMED_APERTURE_IFU) ?
                            "Signal and Background (IFU summed apertures: " +
                                    device.toString(instrument.getIFUNumX()) + "x" + device.toString(instrument.getIFUNumY()) +
                                    ", " + device.toString(instrument.getIFUNumX() * instrument.getIFU().IFU_LEN_X) + "\"x" +
                                    device.toString(instrument.getIFUNumY() * instrument.getIFU().IFU_LEN_Y) + "\")" :
                            "Signal and Background (IFU element offset: " + device.toString(ifu_offset) + " arcsec)";

            final ITCChart chart1 = new ITCChart(chart1Title, "Wavelength (nm)", "e- per exposure per spectral pixel", pdp);
            chart1.addArray(result.specS2N()[i].getSignalSpectrum().getData(), "Signal ");
            chart1.addArray(result.specS2N()[i].getBackgroundSpectrum().getData(), "SQRT(Background)  ");
            _println(chart1.getBufferedImage(), "SigAndBack");
            _println("");


            sigSpec = _printSpecTag("ASCII signal spectrum");
            backSpec = _printSpecTag("ASCII background spectrum");

            final String chart2Title =
                    instrument.getIFUMethod().equals(NifsParameters.SUMMED_APERTURE_IFU) ?
                            "Intermediate Single Exp and Final S/N \n(IFU apertures:" +
                                    device.toString(instrument.getIFUNumX()) + "x" + device.toString(instrument.getIFUNumY()) +
                                    ", " + device.toString(instrument.getIFUNumX() * instrument.getIFU().IFU_LEN_X) + "\"x" +
                                    device.toString(instrument.getIFUNumY() * instrument.getIFU().IFU_LEN_Y) + "\")" :
                            "Intermediate Single Exp and Final S/N (IFU element offset: " + device.toString(ifu_offset) + " arcsec)";

            final ITCChart chart2 = new ITCChart(chart2Title, "Wavelength (nm)", "Signal / Noise per spectral pixel", pdp);
            chart2.addArray(result.specS2N()[i].getExpS2NSpectrum().getData(), "Single Exp S/N");
            chart2.addArray(result.specS2N()[i].getFinalS2NSpectrum().getData(), "Final S/N  ");
            _println(chart2.getBufferedImage(), "Sig2N");
            _println("");

            singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
            finalS2N = _printSpecTag("Final S/N ASCII data");
        }

        _println("");
        device.setPrecision(2);  // TWO decimal places
        device.clear();

        _print("<HR align=left SIZE=3>");

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(result.source()));
        _println(instrument.toString());
        if (result.aoSystem().isDefined()) {
            _println(HtmlPrinter.printParameterSummary((Altair) result.aoSystem().get()));
        }
        _println(HtmlPrinter.printParameterSummary(result.telescope()));
        _println(HtmlPrinter.printParameterSummary(result.conditions()));
        _println(HtmlPrinter.printParameterSummary(result.observation()));
        _println(HtmlPrinter.printParameterSummary(pdp));

        final String header = "# NIFS ITC: " + Calendar.getInstance().getTime() + "\n";
        _println(result.specS2N()[result.specS2N().length-1].getSignalSpectrum(), header, sigSpec);
        _println(result.specS2N()[result.specS2N().length-1].getBackgroundSpectrum(), header, backSpec);
        _println(result.specS2N()[result.specS2N().length-1].getExpS2NSpectrum(), header, singleS2N);
        _println(result.specS2N()[result.specS2N().length-1].getFinalS2NSpectrum(), header, finalS2N);
    }

}
