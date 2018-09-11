package edu.gemini.itc.trecs;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import scala.Option;

import java.util.ArrayList;
import java.util.List;

/**
 * This class performs the calculations for T-Recs used for imaging.
 */
public final class TRecsRecipe implements ImagingRecipe, SpectroscopyRecipe {

    private final ItcParameters p;
    private final TRecs instrument;
    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs a TRecsRecipe given the parameters. Useful for testing.
     */
    public TRecsRecipe(final ItcParameters p, final TRecsParameters instr) {
        instrument              = new TRecs(instr, p.observation());
        _sdParameters           = p.source();
        _obsDetailParameters    = correctedObsDetails(instr, p.observation());
        _obsConditionParameters = p.conditions();
        _telescope              = p.telescope();
        // update parameters with "corrected" version
        this.p                  = new ItcParameters(p.source(), _obsDetailParameters, p.conditions(), p.telescope(), p.instrument());

        validateInputParameters();
    }

    private void validateInputParameters() {
        // For mid-IR observation the watervapor percentile and sky background
        // percentile must be the same
        if (!(_obsConditionParameters.wv().getPercentage() == _obsConditionParameters.sb().getPercentage())) {
            throw new RuntimeException("Sky background percentile must be equal to sky transparency(water vapor): \n "
                    + "    Please modify the Observing condition constraints section of the HTML form \n"
                    + "    and recalculate.");
        }

        // some general validations
        Validation.validate(instrument, _obsDetailParameters, _sdParameters);
    }

    private ObservationDetails correctedObsDetails(final TRecsParameters tp, final ObservationDetails odp) {
        // TODO : These corrections were previously done in random places throughout the recipe. I moved them here
        // TODO : so the ObservationDetailsParameters object can become immutable. Basically this calculates
        // TODO : some missing parameters and/or turns the total exposure time into a single exposure time.
        // TODO : This is a temporary hack. There needs to be a better solution for this.
        // NOTE : odp.exposureTime() carries the TOTAL exposure time (as opposed to exp time for a single frame)
        final TRecs instrument = new TRecs(tp, odp); // TODO: Avoid creating an instrument instance twice.
        final double correctedExposureTime = instrument.getFrameTime();
        final int correctedNumExposures = new Double(odp.exposureTime() / instrument.getFrameTime() + 0.5).intValue();
        if (odp.calculationMethod() instanceof ImagingInt) {
            return new ObservationDetails(
                    new ImagingInt(((ImagingInt) odp.calculationMethod()).sigma(), correctedExposureTime, odp.coadds(), odp.sourceFraction(), odp.offset()),
                    odp.analysisMethod()
            );
        } else if (odp.calculationMethod() instanceof ImagingS2N) {
            return new ObservationDetails(
                    new ImagingS2N(correctedNumExposures, odp.coadds(), correctedExposureTime, odp.sourceFraction(), odp.offset()),
                    odp.analysisMethod()
            );
        } else if (odp.calculationMethod() instanceof SpectroscopyS2N) {
            return new ObservationDetails(
                    new SpectroscopyS2N(correctedNumExposures, odp.coadds(), correctedExposureTime, odp.sourceFraction(), odp.offset()),
                    odp.analysisMethod()
            );
        } else {
            throw new IllegalArgumentException();
        }
    }

    public ItcImagingResult serviceResult(final ImagingResult r) {
        return Recipe$.MODULE$.serviceResult(r);
    }

    public ItcSpectroscopyResult serviceResult(final SpectroscopyResult r) {
        final List<SpcChartData> dataSets = new ArrayList<SpcChartData>() {{
            add(Recipe$.MODULE$.createSignalChart(r, 0));
            add(Recipe$.MODULE$.createS2NChart(r, 0));
        }};
        return Recipe$.MODULE$.serviceResult(r, dataSets);
    }

    public SpectroscopyResult calculateSpectroscopy() {

        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope);
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

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        final Slit slit = Slit$.MODULE$.apply(_sdParameters, _obsDetailParameters, instrument, instrument.getSlitWidth(), IQcalc.getImageQuality());
        final SlitThroughput throughput = new SlitThroughput(_sdParameters, slit, IQcalc.getImageQuality());

        // TODO: why, oh why?
        final double im_qual = _sdParameters.isUniform() ? 10000 : IQcalc.getImageQuality();

        final SpecS2NSlitVisitor specS2N = new SpecS2NSlitVisitor(
                slit,
                instrument.disperser.get(),
                throughput,
                instrument.getSpectralPixelWidth(),
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                im_qual,
                instrument.getReadNoise(),
                instrument.getDarkCurrent(),
                _obsDetailParameters);

        specS2N.setSourceSpectrum(sed);
        specS2N.setBackgroundSpectrum(sky);
        sed.accept(specS2N);

        final SpecS2NSlitVisitor[] specS2Narr = new SpecS2NSlitVisitor[1];
        specS2Narr[0] = specS2N;
        return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, throughput.throughput(), Option.empty());
    }

    public ImagingResult calculateImaging() {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope);
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

        return ImagingResult.apply(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc);

    }

}
