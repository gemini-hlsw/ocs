package edu.gemini.itc.gsaoi;

import edu.gemini.itc.gems.Gems;
import edu.gemini.itc.gems.GemsParameters;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.HtmlPrinter;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.spModel.core.Site;
import scala.Some;

import java.io.PrintWriter;

/**
 * This class performs the calculations for Gsaoi used for imaging.
 */
public final class GsaoiRecipe extends RecipeBase {

    private final GemsParameters _gemsParameters;
    private final GsaoiParameters _gsaoiParameters;
    private final ObservingConditions _obsConditionParameters;
    private final ObservationDetails _obsDetailParameters;
    private final SourceDefinition _sdParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs a GsaoiRecipe by parsing a Multipart servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public GsaoiRecipe(final ITCMultiPartParser r, final PrintWriter out) {
        super(out);

        _sdParameters = ITCRequest.sourceDefinitionParameters(r);
        _obsDetailParameters = ITCRequest.observationParameters(r);
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _gsaoiParameters = new GsaoiParameters(r);
        _telescope = ITCRequest.teleParameters(r);
        _gemsParameters = new GemsParameters(r);

        validateInputParameters();
    }

    /**
     * Constructs a GsaoiRecipe given the parameters. Useful for testing.
     */
    public GsaoiRecipe(final SourceDefinition sdParameters,
                       final ObservationDetails obsDetailParameters,
                       final ObservingConditions obsConditionParameters,
                       final GsaoiParameters gsaoiParameters, TelescopeDetails telescope,
                       final GemsParameters gemsParameters,
                       final PrintWriter out)

    {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _gsaoiParameters = gsaoiParameters;
        _telescope = telescope;
        _gemsParameters = gemsParameters;

        validateInputParameters();
    }

    private void validateInputParameters() {
        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE))
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters
                    .getELineWavelength() * 1000 * 25))) { // *25 b/c of
                // increased
                // resolution of
                // transmission
                // files
                throw new RuntimeException(
                        "Please use a model line width > 0.04 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }

        // report error if this does not come out to be an integer
        Validation.checkSourceFraction(_obsDetailParameters.getNumExposures(), _obsDetailParameters.getSourceFraction());
    }

    /**
     * Performes recipe calculation and writes results to a cached PrintWriter or to System.out.
     */
    public void writeOutput() {
        final Gsaoi instrument = new Gsaoi(_gsaoiParameters, _obsDetailParameters);
        final ImagingResult result = calculateImaging(instrument);
        writeImagingOutput(instrument, result);
    }

    public ImagingResult calculateImaging() {
        final Gsaoi instrument = new Gsaoi(_gsaoiParameters, _obsDetailParameters);
        return calculateImaging(instrument);
    }

    private ImagingResult calculateImaging(final Gsaoi instrument) {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        // Gems specific section
        final Gems gems = new Gems(instrument.getEffectiveWavelength(),
                _telescope.getTelescopeDiameter(), IQcalc.getImageQuality(),
                _gemsParameters.getAvgStrehl(), _gemsParameters.getStrehlBand(),
                _obsConditionParameters.getImageQualityPercentile(),
                _sdParameters);

        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GS, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope, null, new Some<AOSystem>(gems));


        // End of the Spectral energy distribution portion of the ITC.

        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        final double sed_integral = calcSource.sed.getIntegral();
        final double sky_integral = calcSource.sky.getIntegral();
        final double halo_integral = calcSource.halo.get().getIntegral();

        // if gems is used we need to calculate both a core and halo
        // source_fraction
        // halo first
        final SourceFraction SFcalc;
        final SourceFraction SFcalcHalo;
        final double im_qual = gems.getAOCorrectedFWHM();
        if (_obsDetailParameters.isAutoAperture()) {
            SFcalcHalo  = SourceFractionFactory.calculate(_sdParameters.isUniform(), false, 1.18 * im_qual, instrument.getPixelSize(), IQcalc.getImageQuality());
            SFcalc      = SourceFractionFactory.calculate(_sdParameters.isUniform(), _obsDetailParameters.isAutoAperture(), 1.18 * im_qual, instrument.getPixelSize(), im_qual);
        } else {
            SFcalcHalo  = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, IQcalc.getImageQuality());
            SFcalc      = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);
        }
        final double halo_source_fraction = SFcalcHalo.getSourceFraction();

        // Calculate peak pixel flux
        final double peak_pixel_count =
                PeakPixelFlux.calculateWithHalo(instrument, _sdParameters, _obsDetailParameters, SFcalc, im_qual, IQcalc.getImageQuality(), halo_integral, sed_integral, sky_integral);

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        // ObservationMode Imaging
        final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc, sed_integral, sky_integral);
        IS2Ncalc.setSecondaryIntegral(halo_integral);
        IS2Ncalc.setSecondarySourceFraction(halo_source_fraction);
        IS2Ncalc.calculate();

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        return ImagingResult.apply(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc, gems);

    }

    private void writeImagingOutput(final Gsaoi instrument, final ImagingResult result) {
        _println("");

        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        _println(((Gems)result.aoSystem().get()).printSummary());

        _print(result.sfCalc().getTextResult(device, false));
        _println("derived image halo size (FWHM) for a point source = "
                + device.toString(result.iqCalc().getImageQuality()) + " arcsec.\n");

        _println(result.is2nCalc().getTextResult(device));
        _println(result.is2nCalc().getBackgroundLimitResult());
        device.setPrecision(0); // NO decimal places
        device.clear();

        _println("");
        _println("The peak pixel signal + background is " + device.toString(result.peakPixelCount()));

        // REL-1353
        final int peak_pixel_percent = (int) (100 * result.peakPixelCount() / 126000);
        _println("This is " + peak_pixel_percent + "% of the full well depth of 126000 electrons");
        if (peak_pixel_percent > 65 && peak_pixel_percent <= 85) {
            _error("Warning: the peak pixel + background level exceeds 65% of the well depth and will cause deviations from linearity of more than 5%.");
        } else if (peak_pixel_percent > 85) {
            _error("Warning: the peak pixel + background level exceeds 85% of the well depth and may cause saturation.");
        }

        _println("");
        device.setPrecision(2); // TWO decimal places
        device.clear();

        _print("<HR align=left SIZE=3>");

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(_sdParameters));
        _println(instrument.toString());
        _println(printTeleParametersSummary("gems"));
        _println(_gemsParameters.printParameterSummary());
        _println(HtmlPrinter.printParameterSummary(_obsConditionParameters));
        _println(HtmlPrinter.printParameterSummary(_obsDetailParameters));

    }


    public String printTeleParametersSummary(String wfs) {
        StringBuffer sb = new StringBuffer();
        sb.append("Telescope configuration: \n");
        sb.append("<LI>" + _telescope.getMirrorCoating().displayValue() + " mirror coating.\n");
        sb.append("<LI>wavefront sensor: " + wfs + "\n");
        return sb.toString();
    }
}
