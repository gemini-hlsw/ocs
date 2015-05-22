package edu.gemini.itc.niri;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.base.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.Site;
import scala.Option;
import scala.Tuple2;
import scala.collection.JavaConversions;

import java.util.ArrayList;
import java.util.List;

/**
 * This class performs the calculations for Niri used for imaging.
 */
public final class NiriRecipe implements ImagingRecipe, SpectroscopyRecipe {

    private final NiriParameters _niriParameters;
    private final ObservingConditions _obsConditionParameters;
    private final ObservationDetails _obsDetailParameters;
    private final SourceDefinition _sdParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs a NiriRecipe given the parameters.
     */
    public NiriRecipe(final SourceDefinition sdParameters,
                      final ObservationDetails obsDetailParameters,
                      final ObservingConditions obsConditionParameters,
                      final NiriParameters niriParameters,
                      final TelescopeDetails telescope)

    {
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _niriParameters = niriParameters;
        _telescope = telescope;

        validateInputParameters();
    }

    private void validateInputParameters() {
        if (_niriParameters.altair().isDefined()) {
            if (_obsDetailParameters.getMethod().isSpectroscopy()) {
                throw new IllegalArgumentException(
                        "Altair cannot currently be used with Spectroscopy mode in the ITC.  Please deselect either altair or spectroscopy and resubmit the form.");
            }
        }

        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE))
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength().toNanometers() * 25))) { // *25 b/c of increased resolution of transmission files
                throw new RuntimeException(
                        "Please use a model line width > 0.04 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength().toNanometers() * 25))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }

        // some general validations
        Validation.validate(_obsDetailParameters, _sdParameters);

    }

    public ImagingResult calculateImaging() {
        final Niri instrument = new Niri(_niriParameters, _obsDetailParameters);
        return calculateImaging(instrument);
    }

    public Tuple2<ItcSpectroscopyResult, SpectroscopyResult> calculateSpectroscopy() {
        final Niri instrument = new Niri(_niriParameters, _obsDetailParameters);
        final SpectroscopyResult r = calculateSpectroscopy(instrument);
        final List<SpcChartData> dataSets = new ArrayList<SpcChartData>() {{
            add(Recipe$.MODULE$.createSignalChart(r, 0));
            add(Recipe$.MODULE$.createS2NChart(r, 0));
        }};
        final List<SpcDataFile> dataFiles = new ArrayList<SpcDataFile>() {{
            add(new SpcDataFile(SignalData.instance(),     r.specS2N()[0].getSignalSpectrum().printSpecAsString()));
            add(new SpcDataFile(BackgroundData.instance(), r.specS2N()[0].getBackgroundSpectrum().printSpecAsString()));
            add(new SpcDataFile(SingleS2NData.instance(),  r.specS2N()[0].getExpS2NSpectrum().printSpecAsString()));
            add(new SpcDataFile(FinalS2NData.instance(),   r.specS2N()[0].getFinalS2NSpectrum().printSpecAsString()));
        }};
        return new Tuple2<>(ItcSpectroscopyResult.apply(_sdParameters, _obsDetailParameters, dataSets, dataFiles, new ArrayList<>()), r);
    }

    private SpectroscopyResult calculateSpectroscopy(final Niri instrument) {
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
        if (_niriParameters.altair().isDefined()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(), IQcalc.getImageQuality(), _niriParameters.altair().get(), 0.0);
            altair = Option.apply((AOSystem) ao);
        } else {
            altair = Option.empty();
        }

        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GN, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope, altair);

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

        final double pixel_size = instrument.getPixelSize();

        final SlitThroughput st;
        final SlitThroughput st_halo;
        if (!_obsDetailParameters.isAutoAperture()) {
            st = new SlitThroughput(im_qual,
                    _obsDetailParameters.getApertureDiameter(), pixel_size,
                    instrument.getFPMask());
            st_halo = new SlitThroughput(IQcalc.getImageQuality(),
                    _obsDetailParameters.getApertureDiameter(), pixel_size,
                    instrument.getFPMask());
        } else {
            st = new SlitThroughput(im_qual, pixel_size,
                    instrument.getFPMask());

            st_halo = new SlitThroughput(IQcalc.getImageQuality(), pixel_size,
                    instrument.getFPMask());
        }

        double ap_diam = st.getSpatialPix();
        double spec_source_frac = st.getSlitThroughput();
        double halo_spec_source_frac = st_halo.getSlitThroughput();

        if (_sdParameters.isUniform()) {

            if (_obsDetailParameters.isAutoAperture()) {
                ap_diam = new Double(1 / (instrument.getFPMask() * pixel_size) + 0.5).intValue();
                spec_source_frac = 1;
            } else {
                spec_source_frac = instrument.getFPMask() * ap_diam * pixel_size;
            }
        }

        final SpecS2NVisitor specS2N = new SpecS2NVisitor(instrument.getFPMask(),
                pixel_size, instrument.getSpectralPixelWidth(),
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                instrument.getGrismResolution(), spec_source_frac, im_qual,
                ap_diam,
                _obsDetailParameters.getNumExposures(),
                _obsDetailParameters.getSourceFraction(),
                _obsDetailParameters.getExposureTime(),
                instrument.getDarkCurrent(),
                instrument.getReadNoise());
        specS2N.setSourceSpectrum(calcSource.sed);
        specS2N.setBackgroundSpectrum(calcSource.sky);
        if (altair.isDefined())
            specS2N.setSpecHaloSourceFraction(halo_spec_source_frac);
        else
            specS2N.setSpecHaloSourceFraction(0.0);

        calcSource.sed.accept(specS2N);

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        final SpecS2N[] specS2Narr = new SpecS2N[1];
        specS2Narr[0] = specS2N;
        return new GenericSpectroscopyResult(p, instrument, SFcalc, IQcalc, specS2Narr, st, altair, ImagingResult.NoWarnings());
    }

    private ImagingResult calculateImaging(final Niri instrument) {
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
        if (_niriParameters.altair().isDefined()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(), IQcalc.getImageQuality(), _niriParameters.altair().get(), 0.0);
            altair = Option.apply((AOSystem) ao);
        } else {
            altair = Option.empty();
        }

        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GN, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope, altair);

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

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        return new ImagingResult(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc, altair, ImagingResult.NoWarnings());

    }

}
