package edu.gemini.itc.niri;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import scala.Option;

import java.util.ArrayList;
import java.util.List;

/**
 * This class performs the calculations for Niri used for imaging.
 */
public final class NiriRecipe implements ImagingRecipe, SpectroscopyRecipe {

    private final ItcParameters p;
    private final Niri instrument;
    private final NiriParameters niriParameters;
    private final ObservingConditions _obsConditionParameters;
    private final ObservationDetails _obsDetailParameters;
    private final SourceDefinition _sdParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs a NiriRecipe given the parameters.
     */
    public NiriRecipe(final ItcParameters p, final NiriParameters instr)

    {
        this.p                  = p;
        niriParameters          = instr;
        _sdParameters           = p.source();
        _obsDetailParameters    = p.observation();
        _obsConditionParameters = p.conditions();
        _telescope              = p.telescope();
        instrument = new Niri(niriParameters, _obsDetailParameters);

        validateInputParameters();
    }

    private void validateInputParameters() {
        if (niriParameters.altair().isDefined()) {
            if (_obsDetailParameters.calculationMethod() instanceof Spectroscopy) {
                throw new IllegalArgumentException(
                        "Altair cannot currently be used with Spectroscopy mode in the ITC.  Please deselect either altair or spectroscopy and resubmit the form.");
            }
        }

        // some general validations
        Validation.validate(instrument, _obsDetailParameters, _sdParameters);

    }

    public ItcImagingResult serviceResult(final ImagingResult r) {
        return Recipe$.MODULE$.serviceResult(r);
    }

    public ItcSpectroscopyResult serviceResult(final SpectroscopyResult r, final boolean headless) {
        final List<SpcChartData> dataSets = new ArrayList<SpcChartData>() {{
            if (!headless) add(Recipe$.MODULE$.createSignalChart(r, 0));
            add(Recipe$.MODULE$.createS2NChart(r, 0));
        }};
        return Recipe$.MODULE$.serviceResult(r, dataSets, headless);
    }

    public SpectroscopyResult calculateSpectroscopy() {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        // Altair specific section
        final Option<AOSystem> altair;
        if (niriParameters.altair().isDefined()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(), IQcalc.getImageQuality(), _obsConditionParameters.cc().getExtinction(), niriParameters.altair().get(), 0.0);
            altair = Option.<AOSystem>apply((AOSystem) ao);
        } else {
            altair = Option.<AOSystem>empty();
        }

        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope, altair);

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

        final double im_qual = altair.isDefined() ? altair.get().getAOCorrectedFWHM() : IQcalc.getImageQuality();

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        final Slit slit = Slit$.MODULE$.apply(_sdParameters, _obsDetailParameters, instrument, instrument.getSlitWidth(), IQcalc.getImageQuality());
        final SlitThroughput throughput = new SlitThroughput(_sdParameters, slit, im_qual);

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

        specS2N.setSourceSpectrum(calcSource.sed);
        specS2N.setBackgroundSpectrum(calcSource.sky);

        calcSource.sed.accept(specS2N);

        final SpecS2N[] specS2Narr = new SpecS2N[1];
        specS2Narr[0] = specS2N;
        return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, throughput.throughput(), altair);
    }

    public ImagingResult calculateImaging() {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifted SED

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        // Altair specific section
        final Option<AOSystem> altair;
        if (niriParameters.altair().isDefined()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(), IQcalc.getImageQuality(), _obsConditionParameters.cc().getExtinction(), niriParameters.altair().get(), 0.0);
            altair = Option.<AOSystem>apply((AOSystem) ao);
        } else {
            altair = Option.<AOSystem>empty();
        }

        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope, altair);

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

        // if altair is used we need to calculate both a core and halo
        // source_fraction
        // halo first
        final SourceFraction SFcalc;
        if (altair.isDefined()) {
            final double aoCorrImgQual = altair.get().getAOCorrectedFWHM();
            if (_obsDetailParameters.isAutoAperture()) {
                SFcalc = SourceFractionFactory.calculate(_sdParameters.isUniform(), _obsDetailParameters.isAutoAperture(), 1.18 * aoCorrImgQual, instrument.getPixelSize(), aoCorrImgQual);
            } else {
                SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, aoCorrImgQual);
            }
        } else {
            // this will be the core for an altair source; unchanged for non altair.
            SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, IQcalc.getImageQuality());
        }

        final double im_qual = altair.isDefined() ? altair.get().getAOCorrectedFWHM() : IQcalc.getImageQuality();

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        final double sed_integral = calcSource.sed.getIntegral();
        final double sky_integral = calcSource.sky.getIntegral();
        final double halo_integral = altair.isDefined() ? calcSource.halo.get().getIntegral() : 0.0;

        // Calculate peak pixel flux
        final double peak_pixel_count = altair.isDefined() ?
                PeakPixelFlux.calculateWithHalo(instrument, _sdParameters, _obsDetailParameters, SFcalc, im_qual, IQcalc.getImageQuality(), halo_integral, sed_integral, sky_integral) :
                PeakPixelFlux.calculate(instrument, _sdParameters, _obsDetailParameters, SFcalc, im_qual, sed_integral, sky_integral);

        final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc, sed_integral, sky_integral);
        if (altair.isDefined()) {
            final SourceFraction SFcalcHalo;
            final double aoCorrImgQual = altair.get().getAOCorrectedFWHM();
            if (_obsDetailParameters.isAutoAperture()) {
                SFcalcHalo = SourceFractionFactory.calculate(_sdParameters.isUniform(), false, 1.18 * aoCorrImgQual, instrument.getPixelSize(), IQcalc.getImageQuality());
            } else {
                SFcalcHalo = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, IQcalc.getImageQuality());
            }
            IS2Ncalc.setSecondaryIntegral(halo_integral);
            IS2Ncalc.setSecondarySourceFraction(SFcalcHalo.getSourceFraction());
        }
        IS2Ncalc.calculate();

        return new ImagingResult(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc, altair);

    }

}
