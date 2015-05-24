package edu.gemini.itc.acqcam;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.Site;

/**
 * This class performs the calculations for the Acquisition Camera used for imaging.
 */
public final class AcqCamRecipe implements ImagingRecipe {

    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final TelescopeDetails _telescope;
    private final AcquisitionCamera instrument;

    /**
     * Constructs an AcqCamRecipe given the parameters.
     * Useful for testing.
     */
    public AcqCamRecipe(final SourceDefinition sdParameters,
                        final ObservationDetails obsDetailParameters,
                        final ObservingConditions obsConditionParameters,
                        final TelescopeDetails telescope,
                        final AcquisitionCamParameters acqCamParameters) {

        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _telescope = telescope;

        // create instrument
        instrument = new AcquisitionCamera(acqCamParameters);

        validateInputParameters();
    }

    private void validateInputParameters() {
        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE)) {
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength().toNanometers()))) {
                throw new IllegalArgumentException("Please use a model line width > 1 nm (or " + (3E5 / (_sdParameters.getELineWavelength().toNanometers())) + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }
        }

        // some general validations
        Validation.validate(_obsDetailParameters, _sdParameters);
    }

    public ImagingResult calculateImaging() {
        return calculateImaging(instrument);
    }

    /**
     * Performs recipe calculation.
     */
    private ImagingResult calculateImaging(final AcquisitionCamera instrument) {

        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GN, ITCConstants.VISIBLE, _sdParameters, _obsConditionParameters, _telescope);
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

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        return ImagingResult.apply(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc);

    }

}
