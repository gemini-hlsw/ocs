package edu.gemini.itc.gsaoi;

import edu.gemini.itc.base.*;
import edu.gemini.itc.gems.Gems;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.Site;
import scala.Some;

import java.util.ArrayList;
import java.util.List;

/**
 * This class performs the calculations for Gsaoi used for imaging.
 */
public final class GsaoiRecipe implements ImagingRecipe {

    private final GsaoiParameters _gsaoiParameters;
    private final ObservingConditions _obsConditionParameters;
    private final ObservationDetails _obsDetailParameters;
    private final SourceDefinition _sdParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs a GsaoiRecipe given the parameters. Useful for testing.
     */
    public GsaoiRecipe(final SourceDefinition sdParameters,
                       final ObservationDetails obsDetailParameters,
                       final ObservingConditions obsConditionParameters,
                       final GsaoiParameters gsaoiParameters,
                       final TelescopeDetails telescope)

    {
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _gsaoiParameters = gsaoiParameters;
        _telescope = telescope;

        validateInputParameters();
    }

    private void validateInputParameters() {
        // some general validations
        Validation.validate(_obsDetailParameters, _sdParameters, 25.0);
    }

    public ImagingResult calculateImaging() {
        final Gsaoi instrument = new Gsaoi(_gsaoiParameters, _obsDetailParameters);
        return calculateImaging(instrument);
    }

    private ImagingResult calculateImaging(final Gsaoi instrument) {
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
                _telescope.getTelescopeDiameter(), IQcalc.getImageQuality(),
                _gsaoiParameters.gems().avgStrehl(), _gsaoiParameters.gems().strehlBand(),
                _obsConditionParameters.getImageQualityPercentile(),
                _sdParameters);

        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GS, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope, new Some<AOSystem>(gems));


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

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        final List<ItcWarning> warnings = warningsForImaging(instrument, peak_pixel_count);
        return ImagingResult.apply(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc, gems, warnings);
    }

    // TODO: some of these warnings are similar for different instruments and could be calculated in a central place
    private List<ItcWarning> warningsForImaging(final Gsaoi instrument, final double peakPixelCount) {
        final int peakPixelPercent = (int) (100 * peakPixelCount / Gsaoi.WELL_DEPTH);
        return new ArrayList<ItcWarning>() {{
            if (peakPixelPercent > 65 && peakPixelPercent <= 85) add(new ItcWarning("Warning: the peak pixel + background level exceeds 65% of the well depth and will cause deviations from linearity of more than 5%."));
            if (peakPixelPercent > 85)                           add(new ItcWarning("Warning: the peak pixel + background level exceeds 85% of the well depth and may cause saturation."));
        }};
    }


}
