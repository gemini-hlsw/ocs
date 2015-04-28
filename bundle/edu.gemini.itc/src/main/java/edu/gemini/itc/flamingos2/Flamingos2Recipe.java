package edu.gemini.itc.flamingos2;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.Site;

/**
 * This class performs the calculations for Flamingos 2 used for imaging.
 */
public final class Flamingos2Recipe implements ImagingRecipe, SpectroscopyRecipe {

    private final Flamingos2Parameters _flamingos2Parameters;
    private final ObservingConditions _obsConditionParameters;
    private final ObservationDetails _obsDetailParameters;
    private final SourceDefinition _sdParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs an Flamingos 2 object given the parameters. Useful for
     * testing.
     */
    public Flamingos2Recipe(final SourceDefinition sdParameters,
                            final ObservationDetails obsDetailParameters,
                            final ObservingConditions obsConditionParameters,
                            final Flamingos2Parameters flamingos2Parameters,
                            final TelescopeDetails telescope) {
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _flamingos2Parameters = flamingos2Parameters;
        _telescope = telescope;

        validateInputParameters();
    }

    /**
     * Check input parameters for consistency
     */
    private void validateInputParameters() {
        if (_obsDetailParameters.getMethod().isSpectroscopy()) {
            switch (_flamingos2Parameters.grism()) {
                case NONE: throw new IllegalArgumentException("In spectroscopy mode, a grism must be selected");
            }
            switch (_flamingos2Parameters.mask()) {
                case FPU_NONE: throw new IllegalArgumentException("In spectroscopy mode, a FP must must be selected");
            }
        }

        // report error if this does not come out to be an integer
        Validation.checkSourceFraction(_obsDetailParameters.getNumExposures(), _obsDetailParameters.getSourceFraction());
    }

    public SpectroscopyResult calculateSpectroscopy() {
        final Flamingos2 instrument = new Flamingos2(_flamingos2Parameters);
        return calculateSpectroscopy(instrument);
    }

    public ImagingResult calculateImaging() {
        final Flamingos2 instrument = new Flamingos2(_flamingos2Parameters);
        return calculateImaging(instrument);
    }

    private SpectroscopyResult calculateSpectroscopy(final Flamingos2 instrument) {
        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification
        final SEDFactory.SourceResult src = SEDFactory.calculate(instrument, Site.GS, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope);

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();
        final double im_qual = IQcalc.getImageQuality();

        // Calculate Source fraction
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        final double pixel_size = instrument.getPixelSize();
        final SpecS2NLargeSlitVisitor specS2N;
        final SlitThroughput st;

        if (!_obsDetailParameters.isAutoAperture()) {
            st = new SlitThroughput(im_qual, _obsDetailParameters.getApertureDiameter(), pixel_size, instrument.getSlitSize() * pixel_size);
        } else {
            st = new SlitThroughput(im_qual, pixel_size, instrument.getSlitSize() * pixel_size);
        }

        double ap_diam = st.getSpatialPix();
        double spec_source_frac = st.getSlitThroughput();

        if (_sdParameters.isUniform()) {
            if (_obsDetailParameters.isAutoAperture()) {
                ap_diam = new Double(1 / (instrument.getSlitSize() * pixel_size) + 0.5).intValue();
                spec_source_frac = 1;
            } else {
                spec_source_frac = instrument.getSlitSize() * pixel_size * ap_diam * pixel_size;
            }
        }

        final double gratDispersion_nmppix = instrument.getSpectralPixelWidth();
        final double gratDispersion_nm = 0.5 / pixel_size * gratDispersion_nmppix;

        specS2N = new SpecS2NLargeSlitVisitor(instrument.getSlitSize() * pixel_size,
                pixel_size, instrument.getSpectralPixelWidth(),
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                gratDispersion_nm,
                gratDispersion_nmppix,
                instrument.getGrismResolution(), spec_source_frac, im_qual,
                ap_diam,
                _obsDetailParameters.getNumExposures(),
                _obsDetailParameters.getSourceFraction(),
                _obsDetailParameters.getExposureTime(),
                instrument.getDarkCurrent(),
                instrument.getReadNoise(),
                _obsDetailParameters.getSkyApertureDiameter());

        specS2N.setDetectorTransmission(instrument.getDetectorTransmision());
        specS2N.setSourceSpectrum(src.sed);
        specS2N.setBackgroundSpectrum(src.sky);
        specS2N.setSpecHaloSourceFraction(0.0);
        src.sed.accept(specS2N);

        final SpecS2NLargeSlitVisitor[] specS2Narr = new SpecS2NLargeSlitVisitor[1];
        specS2Narr[0] = specS2N;

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        return SpectroscopyResult$.MODULE$.apply(p, instrument, SFcalc, IQcalc, specS2Narr, st);
    }

    private ImagingResult calculateImaging(final Flamingos2 instrument) {

        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification
        final SEDFactory.SourceResult src = SEDFactory.calculate(instrument, Site.GS, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope);

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();
        final double im_qual = IQcalc.getImageQuality();

        // Calculate Source fraction
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        // Get the summed source and sky
        final VisitableSampledSpectrum sed = src.sed;
        final VisitableSampledSpectrum sky = src.sky;
        final double sed_integral = sed.getIntegral();
        final double sky_integral = sky.getIntegral();

        // Calculate the Peak Pixel Flux
        final double peak_pixel_count = PeakPixelFlux.calculate(instrument, _sdParameters, _obsDetailParameters, SFcalc, im_qual, sed_integral, sky_integral);

        // Calculate the Signal to Noise
        final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc, sed_integral, sky_integral);
        IS2Ncalc.calculate();

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        return ImagingResult.apply(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc);
    }

}
