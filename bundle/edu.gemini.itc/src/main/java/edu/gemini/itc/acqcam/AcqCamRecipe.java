package edu.gemini.itc.acqcam;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.service.ObservationDetails;
import edu.gemini.itc.service.ObservingConditions;
import edu.gemini.itc.service.SourceDefinition;
import edu.gemini.itc.service.TelescopeDetails;
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
    }

    /**
     * Performes recipe calculation and writes results to a cached PrintWriter
     * or to System.out.
     */
    public void writeOutput() {
        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();
        _println("");

        final AcquisitionCamera instrument = new AcquisitionCamera(_acqCamParameters.getColorFilter(), _acqCamParameters.getNDFilter());

        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE)) {
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength() * 1000))) {
                throw new IllegalArgumentException("Please use a model line width > 1 nm (or " + (3E5 / (_sdParameters.getELineWavelength() * 1000)) + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }
        }

        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GN, ITCConstants.VISIBLE, _sdParameters, _obsConditionParameters, _telescope, null);
        final VisitableSampledSpectrum sed = calcSource.sed;
        final VisitableSampledSpectrum sky = calcSource.sky;
        final double sed_integral = sed.getIntegral();
        final double sky_integral = sky.getIntegral();

        if (sed.getStart() > instrument.getObservingStart() || sed.getEnd() < instrument.getObservingEnd()) {
            _println(" Sed start" + sed.getStart() + "> than instrument start" + instrument.getObservingStart());
            _println(" Sed END" + sed.getEnd() + "< than instrument end" + instrument.getObservingEnd());
            throw new IllegalArgumentException("Shifted spectrum lies outside of observed wavelengths");
        }


        // For debugging, print the spectrum integrals.
        //_println("SED integral: "+sed_integral+"\tSKY integral: "+sky_integral);

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

        final double pixel_size = instrument.getPixelSize();
        final double peak_pixel_count;

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();
        final double im_qual = IQcalc.getImageQuality();


//Calculate Source fraction
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);
        _print(SFcalc.getTextResult(device));
        _println(IQcalc.getTextResult(device));

// Calculate the Peak Pixel Flux
        final PeakPixelFluxCalc ppfc;

        if (!_sdParameters.isUniform()) {

            ppfc = new
                    PeakPixelFluxCalc(im_qual, pixel_size,
                    _obsDetailParameters.getExposureTime(),
                    sed_integral, sky_integral,
                    instrument.getDarkCurrent());

            peak_pixel_count = ppfc.getFluxInPeakPixel();

        } else {

            ppfc = new
                    PeakPixelFluxCalc(im_qual, pixel_size,
                    _obsDetailParameters.getExposureTime(),
                    sed_integral, sky_integral,
                    instrument.getDarkCurrent());
            peak_pixel_count = ppfc.getFluxInPeakPixelUSB(SFcalc.getSourceFraction(), SFcalc.getNPix());

        }

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.


        // Observation method

        final int number_exposures = _obsDetailParameters.getNumExposures();
        final double frac_with_source = _obsDetailParameters.getSourceFraction();

        // report error if this does not come out to be an integer
        checkSourceFraction(number_exposures, frac_with_source);

        //Calculate the Signal to Noise

        final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc);
        IS2Ncalc.setSedIntegral(sed_integral);
        IS2Ncalc.setSkyIntegral(sky_integral);
        IS2Ncalc.setSkyAperture(_obsDetailParameters.getSkyApertureDiameter());
        IS2Ncalc.setDarkCurrent(instrument.getDarkCurrent());
        IS2Ncalc.calculate();
        _println(IS2Ncalc.getTextResult(device));
        device.setPrecision(0);  // NO decimal places
        device.clear();

        _println("");
        _println("The peak pixel signal + background is " + device.toString(peak_pixel_count) + ". This is " +
                device.toString(peak_pixel_count / instrument.getWellDepth() * 100) +
                "% of the full well depth of " + device.toString(instrument.getWellDepth()) + ".");

        if (peak_pixel_count > (.8 * instrument.getWellDepth()))
            _println("Warning: peak pixel exceeds 80% of the well depth and may be saturated");

        _println("");
        device.setPrecision(2);  // TWO decimal places
        device.clear();


        ///////////////////////////////////////////////
        //////////Print Config////////////////////////

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
