package edu.gemini.itc.acqcam;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.ObservingConditions;
import edu.gemini.itc.shared.SourceDefinition;
import edu.gemini.itc.shared.TelescopeDetails;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.HtmlPrinter;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.spModel.core.Site;

import java.io.PrintWriter;

/**
 * This class performs the calculations for the Acquisition Camera
 * used for imaging.
 */
public final class AcqCamRecipe extends RecipeBase {
    // Parameters from the web page.
    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final AcquisitionCamParameters _acqCamParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs an AcqCamRecipe by parsing a Multi part servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public AcqCamRecipe(final ITCMultiPartParser r, final PrintWriter out) {
        super(out);

        // Read parameters from the four main sections of the web page.
        _sdParameters = ITCRequest.sourceDefinitionParameters(r);
        _obsDetailParameters = ITCRequest.observationParameters(r);
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _acqCamParameters = new AcquisitionCamParameters(r);
        _telescope = ITCRequest.teleParameters(r);

        validateInputParameters();
    }

    /**
     * Constructs an AcqCamRecipe given the parameters.
     * Useful for testing.
     */
    public AcqCamRecipe(final SourceDefinition sdParameters,
                        final ObservationDetails obsDetailParameters,
                        final ObservingConditions obsConditionParameters,
                        final AcquisitionCamParameters acqCamParameters,
                        final TelescopeDetails telescope,
                        final PrintWriter out) {
        super(out);

        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _acqCamParameters = acqCamParameters;
        _telescope = telescope;

        validateInputParameters();
    }

    private void validateInputParameters() {
        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE)) {
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength() * 1000))) {
                throw new IllegalArgumentException("Please use a model line width > 1 nm (or " + (3E5 / (_sdParameters.getELineWavelength() * 1000)) + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }
        }

        // report error if this does not come out to be an integer
        checkSourceFraction(_obsDetailParameters.getNumExposures(), _obsDetailParameters.getSourceFraction());
    }

    public void writeOutput() {
        final AcquisitionCamera instrument = new AcquisitionCamera(_acqCamParameters.getColorFilter(), _acqCamParameters.getNDFilter());
        final ImagingResult result = calculateImaging(instrument);
        writeImagingOutput(instrument, result);
    }

    public ImagingResult calculateImaging() {
        final AcquisitionCamera instrument = new AcquisitionCamera(_acqCamParameters.getColorFilter(), _acqCamParameters.getNDFilter());
        return calculateImaging(instrument);
    }

    /**
     * Performes recipe calculation .
     */
    private ImagingResult calculateImaging(final AcquisitionCamera instrument) {

        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GN, ITCConstants.VISIBLE, _sdParameters, _obsConditionParameters, _telescope, null);
        final VisitableSampledSpectrum sed = calcSource.sed;
        final VisitableSampledSpectrum sky = calcSource.sky;
        final double sed_integral = sed.getIntegral();
        final double sky_integral = sky.getIntegral();

        if (sed.getStart() > instrument.getObservingStart() || sed.getEnd() < instrument.getObservingEnd()) {
            throw new IllegalArgumentException("Shifted spectrum lies outside of observed wavelengths");
        }

        // End of the Spectral energy distribution portion of the ITC.

        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio.  There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();
        final double im_qual = IQcalc.getImageQuality();


        //Calculate Source fraction
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);

        // Calculate the Peak Pixel Flux
        final double peak_pixel_count = PeakPixelFlux.calculate(instrument, _sdParameters, _obsDetailParameters, SFcalc, im_qual, sed_integral, sky_integral);

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        // Observation method

        //Calculate the Signal to Noise

        final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc, sed_integral, sky_integral);
        IS2Ncalc.calculate();

        return ImagingResult.create(IQcalc, SFcalc, peak_pixel_count, IS2Ncalc);

    }

    // ===================================================================================================================
    // TODO: OUTPUT METHODS
    // TODO: These need to be simplified/cleaned/shared and then go to the web module.. and then be deleted and forgotten.
    // ===================================================================================================================

    public void writeImagingOutput(final AcquisitionCamera instrument, final ImagingResult result) {
        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();
        _println("");

        _print(result.SFcalc.getTextResult(device));
        _println(result.IQcalc.getTextResult(device));

        _println(result.IS2Ncalc.getTextResult(device));
        device.setPrecision(0);  // NO decimal places
        device.clear();

        _println("");
        _println("The peak pixel signal + background is " + device.toString(result.peak_pixel_count) + ". This is " +
                device.toString(result.peak_pixel_count / instrument.getWellDepth() * 100) +
                "% of the full well depth of " + device.toString(instrument.getWellDepth()) + ".");

        if (result.peak_pixel_count > (.8 * instrument.getWellDepth()))
            _println("Warning: peak pixel exceeds 80% of the well depth and may be saturated");

        _println("");
        device.setPrecision(2);  // TWO decimal places
        device.clear();

        printConfiguration(instrument);

    }


    private void printConfiguration(final AcquisitionCamera instrument) {
        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(_sdParameters));
        _println(instrument.toString());
        _println(HtmlPrinter.printParameterSummary(_telescope));
        _println(HtmlPrinter.printParameterSummary(_obsConditionParameters));
        _println(HtmlPrinter.printParameterSummary(_obsDetailParameters));

    }
}
