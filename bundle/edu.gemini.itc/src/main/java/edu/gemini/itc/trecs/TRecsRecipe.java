package edu.gemini.itc.trecs;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.Site;

/**
 * This class performs the calculations for T-Recs used for imaging.
 */
public final class TRecsRecipe {

    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final TRecsParameters _trecsParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs a TRecsRecipe given the parameters. Useful for testing.
     */
    public TRecsRecipe(final SourceDefinition sdParameters,
                       final ObservationDetails obsDetailParameters,
                       final ObservingConditions obsConditionParameters,
                       final TRecsParameters trecsParameters,
                       final TelescopeDetails telescope) {

        _sdParameters = sdParameters;
        _obsDetailParameters = correctedObsDetails(trecsParameters, obsDetailParameters);
        _obsConditionParameters = obsConditionParameters;
        _trecsParameters = trecsParameters;
        _telescope = telescope;

        validateInputParameters();
    }

    private void validateInputParameters() {
        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE))
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters
                    .getELineWavelength() * 1000 / 4))) { // /4 b/c of increased
                // resolution of
                // transmission
                // files
                throw new RuntimeException(
                        "Please use a model line width > 4 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength() * 1000 / 4))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }

        // For mid-IR observation the watervapor percentile and sky background
        // percentile must be the same
        if (!_obsConditionParameters.getSkyTransparencyWaterCategory().equals(_obsConditionParameters.getSkyBackgroundCategory())) {
            throw new RuntimeException("Sky background percentile must be equal to sky transparency(water vapor): \n "
                    + "    Please modify the Observing condition constraints section of the HTML form \n"
                    + "    and recalculate.");
        }

        // report error if this does not come out to be an integer
        Validation.checkSourceFraction(_obsDetailParameters.getNumExposures(), _obsDetailParameters.getSourceFraction());
    }

    private ObservationDetails correctedObsDetails(final TRecsParameters tp, final ObservationDetails odp) {
        // TODO : These corrections were previously done in random places throughout the recipe. I moved them here
        // TODO : so the ObservationDetailsParameters object can become immutable. Basically this calculates
        // TODO : some missing parameters and/or turns the total exposure time into a single exposure time.
        // TODO : This is a temporary hack. There needs to be a better solution for this.
        // NOTE : odp.getExposureTime() carries the TOTAL exposure time (as opposed to exp time for a single frame)
        final TRecs instrument = new TRecs(tp, odp); // TODO: Avoid creating an instrument instance twice.
        final double correctedExposureTime = instrument.getFrameTime();
        final int correctedNumExposures = new Double(odp.getExposureTime() / instrument.getFrameTime() + 0.5).intValue();
        if (odp.getMethod() instanceof ImagingInt) {
            return new ObservationDetails(
                    new ImagingInt(odp.getSNRatio(), correctedExposureTime, odp.getSourceFraction()),
                    odp.getAnalysis()
            );
        } else if (odp.getMethod() instanceof ImagingSN) {
            return new ObservationDetails(
                    new ImagingSN(correctedNumExposures, correctedExposureTime, odp.getSourceFraction()),
                    odp.getAnalysis()
            );
        } else if (odp.getMethod() instanceof SpectroscopySN) {
            return new ObservationDetails(
                    new SpectroscopySN(correctedNumExposures, correctedExposureTime, odp.getSourceFraction()),
                    odp.getAnalysis()
            );
        } else {
            throw new IllegalArgumentException();
        }
    }

    public SpectroscopyResult calculateSpectroscopy() {
        final TRecs instrument = new TRecs(_trecsParameters, _obsDetailParameters);
        return calculateSpectroscopy(instrument);
    }

    public ImagingResult calculateImaging() {
        final TRecs instrument = new TRecs(_trecsParameters, _obsDetailParameters);
        return calculateImaging(instrument);
    }

    private SpectroscopyResult calculateSpectroscopy(final TRecs instrument) {

        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GS, ITCConstants.MID_IR, _sdParameters, _obsConditionParameters, _telescope);
        final VisitableSampledSpectrum sed = calcSource.sed;
        final VisitableSampledSpectrum sky = calcSource.sky;

        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        double pixel_size = instrument.getPixelSize();
        double ap_diam = 0;

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        final double exp_time = _obsDetailParameters.getExposureTime();
        final int number_exposures = _obsDetailParameters.getNumExposures();
        final double frac_with_source = _obsDetailParameters.getSourceFraction();
        double spec_source_frac = 0;

        final SlitThroughput st;
        if (!_obsDetailParameters.isAutoAperture()) {
            st = new SlitThroughput(IQcalc.getImageQuality(),
                    _obsDetailParameters.getApertureDiameter(), pixel_size,
                    instrument.getFPMask());
        } else {
            st = new SlitThroughput(IQcalc.getImageQuality(), pixel_size, instrument.getFPMask());
        }


        ap_diam = st.getSpatialPix();
        spec_source_frac = st.getSlitThroughput();

        // For the usb case we want the resolution to be determined by the
        // slit width and not the image quality for a point source.
        final double im_qual;
        if (_sdParameters.isUniform()) {
            im_qual = 10000;
            if (_obsDetailParameters.isAutoAperture()) {
                ap_diam = new Double(1 / (instrument.getFPMask() * pixel_size) + 0.5).intValue();
                spec_source_frac = 1;
            } else {
                spec_source_frac = instrument.getFPMask() * ap_diam * pixel_size; // ap_diam = Spec_NPix
            }
        } else {
            im_qual = IQcalc.getImageQuality();
        }

        final SpecS2NLargeSlitVisitor specS2N = new SpecS2NLargeSlitVisitor(instrument.getFPMask(),
                pixel_size, instrument.getSpectralPixelWidth(),
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                instrument.getGratingDispersion_nm(),
                instrument.getGratingDispersion_nmppix(),
                instrument.getGratingResolution(), spec_source_frac,
                im_qual, ap_diam, number_exposures, frac_with_source,
                exp_time,
                instrument.getDarkCurrent(),
                instrument.getReadNoise(),
                _obsDetailParameters.getSkyApertureDiameter());
        specS2N.setDetectorTransmission(instrument.getDetectorTransmision());
        specS2N.setSourceSpectrum(sed);
        specS2N.setBackgroundSpectrum(sky);
        sed.accept(specS2N);

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        final SpecS2NLargeSlitVisitor[] specS2Narr = new SpecS2NLargeSlitVisitor[1];
        specS2Narr[0] = specS2N;
        return SpectroscopyResult.apply(p, instrument, null, IQcalc, specS2Narr, st); // TODO SFCalc not needed!
    }

    private ImagingResult calculateImaging(final TRecs instrument) {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GS, ITCConstants.MID_IR, _sdParameters, _obsConditionParameters, _telescope);
        final VisitableSampledSpectrum sed = calcSource.sed;
        final VisitableSampledSpectrum sky = calcSource.sky;
        double sed_integral = sed.getIntegral();
        double sky_integral = sky.getIntegral();


        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        // Calculate the Fraction of source in the aperture
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, IQcalc.getImageQuality());

        // Calculate the Peak Pixel Flux
        final double peak_pixel_count = PeakPixelFlux.calculate(instrument, _sdParameters, _obsDetailParameters, SFcalc, IQcalc.getImageQuality(), sed_integral, sky_integral);

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc, sed_integral, sky_integral);
        IS2Ncalc.calculate();

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        return ImagingResult.apply(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc);

    }

}