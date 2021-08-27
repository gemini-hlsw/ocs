package edu.gemini.itc.gsaoi;

import edu.gemini.itc.base.*;
import edu.gemini.itc.gems.Gems;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import scala.Some;

/**
 * This class performs the calculations for Gsaoi used for imaging.
 */
public final class GsaoiRecipe implements ImagingRecipe {

    private final ItcParameters p;
    private final Gsaoi instrument;
    private final GsaoiParameters _gsaoiParameters;
    private final ObservingConditions _obsConditionParameters;
    private final ObservationDetails _obsDetailParameters;
    private final SourceDefinition _sdParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs a GsaoiRecipe given the parameters. Useful for testing.
     */
    public GsaoiRecipe(final ItcParameters p, final GsaoiParameters instr)

    {
        this.p                  = p;
        instrument              = new Gsaoi(instr, p.observation());
        _sdParameters           = p.source();
        _obsDetailParameters    = p.observation();
        _obsConditionParameters = p.conditions();
        _gsaoiParameters        = instr;
        _telescope              = p.telescope();

        // some general validations
        Validation.validate(instrument, _obsDetailParameters, _sdParameters);
    }

    public ItcImagingResult serviceResult(final ImagingResult r) {
        return Recipe$.MODULE$.serviceResult(r);
    }

    public ImagingResult calculateImaging() {
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
                IQcalc.getImageQuality(),
                _gsaoiParameters.gems().avgStrehl(),
                _gsaoiParameters.gems().strehlBand(),
                _obsConditionParameters.javaIq(),
                _sdParameters);

        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope, new Some<>(gems));


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

        return ImagingResult.apply(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc, gems);
    }


}
