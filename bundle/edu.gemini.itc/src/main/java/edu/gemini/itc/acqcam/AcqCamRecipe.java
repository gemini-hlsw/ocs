package edu.gemini.itc.acqcam;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class performs the calculations for the Acquisition Camera used for imaging.
 */
public final class AcqCamRecipe implements ImagingRecipe {

    private final ItcParameters p;
    private final AcquisitionCamera instrument;
    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs an AcqCamRecipe given the parameters.
     * Useful for testing.
     */
    public AcqCamRecipe(final ItcParameters p, final AcquisitionCamParameters instr) {

        this.p                  = p;
        instrument              = new AcquisitionCamera(instr);
        _sdParameters           = p.source();
        _obsDetailParameters    = p.observation();
        _obsConditionParameters = p.conditions();
        _telescope              = p.telescope();

        // some general validations
        Validation.validate(instrument, _obsDetailParameters, _sdParameters);
    }

    public ItcImagingResult serviceResult(final ImagingResult r) {
        return Recipe$.MODULE$.serviceResult(r);
    }

    /**
     * Performs recipe calculation.
     */
    public ImagingResult calculateImaging() {

        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope);
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

        final List<ItcWarning>  warnings = warningsForImaging(instrument, peak_pixel_count);
        return ImagingResult.apply(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc, warnings);

    }

    // TODO: some of these warnings are similar for different instruments and could be calculated in a central place
    private List<ItcWarning> warningsForImaging(final AcquisitionCamera instrument, final double peakPixelCount) {
        final double wellLimit = 0.8 * instrument.getWellDepth();
        return new ArrayList<ItcWarning>() {{
            if (peakPixelCount > wellLimit) add(new ItcWarning("Warning: peak pixel exceeds 80% of the well depth and may be saturated"));
        }};
    }

}
