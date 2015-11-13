package edu.gemini.itc.flamingos2;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import scala.Option;

import java.util.ArrayList;
import java.util.List;

/**
 * This class performs the calculations for Flamingos 2 used for imaging.
 */
public final class Flamingos2Recipe implements ImagingRecipe, SpectroscopyRecipe {

    private final ItcParameters p;
    private final Flamingos2 instrument;
    private final Flamingos2Parameters _flamingos2Parameters;
    private final ObservingConditions _obsConditionParameters;
    private final ObservationDetails _obsDetailParameters;
    private final SourceDefinition _sdParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs an Flamingos 2 object given the parameters. Useful for
     * testing.
     */
    public Flamingos2Recipe(final ItcParameters p, final Flamingos2Parameters instr) {
        this.p                  = p;
        instrument              = new Flamingos2(instr);
        _sdParameters           = p.source();
        _obsDetailParameters    = p.observation();
        _obsConditionParameters = p.conditions();
        _flamingos2Parameters   = instr;
        _telescope              = p.telescope();

        validateInputParameters();
    }

    /**
     * Check input parameters for consistency
     */
    private void validateInputParameters() {
        if (_obsDetailParameters.getMethod().isSpectroscopy()) {
            switch (_flamingos2Parameters.grism()) {
                case NONE:          throw new IllegalArgumentException("In spectroscopy mode, a grism must be selected");
            }
            switch (_flamingos2Parameters.mask()) {
                case FPU_NONE:      throw new IllegalArgumentException("In spectroscopy mode, a FP must be selected");
            }
        }

        // some general validations
        Validation.validate(instrument, _obsDetailParameters, _sdParameters);
    }

    public ItcImagingResult serviceResult(final ImagingResult r) {
        return Recipe$.MODULE$.serviceResult(r);
    }

    public ItcSpectroscopyResult serviceResult(final SpectroscopyResult r) {
        final List<SpcChartData> dataSets = new ArrayList<SpcChartData>() {{
            add(Recipe$.MODULE$.createSignalChart(r));
            add(Recipe$.MODULE$.createS2NChart(r));
        }};
        return Recipe$.MODULE$.serviceResult(r, dataSets);
    }

    public SpectroscopyResult calculateSpectroscopy() {
        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification
        final SEDFactory.SourceResult src = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope);

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

        specS2N = new SpecS2NLargeSlitVisitor(
                instrument.getSlitSize() * pixel_size,
                pixel_size,
                instrument.getSpectralPixelWidth(),
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                gratDispersion_nm,
                gratDispersion_nmppix,
                spec_source_frac, im_qual,
                ap_diam,
                _obsDetailParameters.calculationMethod,
                instrument.getDarkCurrent(),
                instrument.getReadNoise(),
                _obsDetailParameters.getSkyApertureDiameter());

        specS2N.setSourceSpectrum(src.sed);
        specS2N.setBackgroundSpectrum(src.sky);
        specS2N.setSpecHaloSourceFraction(0.0);
        src.sed.accept(specS2N);

        final SpecS2NLargeSlitVisitor[] specS2Narr = new SpecS2NLargeSlitVisitor[1];
        specS2Narr[0] = specS2N;

        return new SpectroscopyResult(p, instrument, SFcalc, IQcalc, specS2Narr, st, Option.empty());
    }

    public ImagingResult calculateImaging() {

        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification
        final SEDFactory.SourceResult src = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope);

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

        return ImagingResult.apply(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc);
    }

}
