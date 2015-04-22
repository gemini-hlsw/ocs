package edu.gemini.itc.nifs;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.Site;
import scala.Option;

import java.util.Iterator;
import java.util.List;

/**
 * This class performs the calculations for Nifs
 * used for imaging.
 */
public final class NifsRecipe implements SpectroscopyRecipe {

    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final NifsParameters _nifsParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs a NifsRecipe given the parameters.
     * Useful for testing.
     */
    public NifsRecipe(final SourceDefinition sdParameters,
                      final ObservationDetails obsDetailParameters,
                      final ObservingConditions obsConditionParameters,
                      final NifsParameters nifsParameters,
                      final TelescopeDetails telescope)

    {
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _nifsParameters = nifsParameters;
        _telescope = telescope;

        validateInputParameters();
    }

    private void validateInputParameters() {
        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE)) {
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25))) {  // *25 b/c of increased resolutuion of transmission files
                throw new RuntimeException("Please use a model line width > 0.04 nm (or " + (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25)) + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }
        }

        // report error if this does not come out to be an integer
        Validation.checkSourceFraction(_obsDetailParameters.getNumExposures(), _obsDetailParameters.getSourceFraction());
    }

    /**
     * Performs recipe calculation.
     */
    public SpectroscopyResult calculateSpectroscopy() {
        final Nifs instrument = new Nifs(_nifsParameters, _obsDetailParameters);
        return calculateSpectroscopy(instrument);
    }


    private SpectroscopyResult calculateSpectroscopy(final Nifs instrument) {

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        final Option<AOSystem> altair;
        if (_nifsParameters.getAltair().isDefined()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(), IQcalc.getImageQuality(), _nifsParameters.getAltair().get(), 0.0);
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
        // to derive the signal to noise ratio.  There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        final double pixel_size = instrument.getPixelSize();

        //IFU morphology section
        final double im_qual = altair.isDefined() ? altair.get().getAOCorrectedFWHM() : IQcalc.getImageQuality();
        final VisitableMorphology morph, haloMorphology;
        switch (_sdParameters.getProfileType()) {
            case POINT:
                morph = new AOMorphology(im_qual);
                haloMorphology = new AOMorphology(IQcalc.getImageQuality());
                break;
            case GAUSSIAN:
                morph = new GaussianMorphology(im_qual);
                haloMorphology = new GaussianMorphology(IQcalc.getImageQuality());
                break;
            case UNIFORM:
                morph = new USBMorphology();
                haloMorphology = new USBMorphology();
                break;
            default:
                throw new IllegalArgumentException();
        }
        morph.accept(instrument.getIFU().getAperture());

        //for now just a single item from the list
        final List<Double> sf_list = instrument.getIFU().getFractionOfSourceInAperture();  //extract corrected source fraction list

        instrument.getIFU().clearFractionOfSourceInAperture();
        haloMorphology.accept(instrument.getIFU().getAperture());


        final List<Double> halo_sf_list = instrument.getIFU().getFractionOfSourceInAperture();  //extract uncorrected halo source fraction list

        final List<Double> ap_offset_list = instrument.getIFU().getApertureOffsetList();

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        double spec_source_frac = 0;
        double halo_spec_source_frac = 0;
        final int number_exposures = _obsDetailParameters.getNumExposures();
        final double frac_with_source = _obsDetailParameters.getSourceFraction();
        final double exposure_time = _obsDetailParameters.getExposureTime();

        final Iterator<Double> src_frac_it = sf_list.iterator();
        final Iterator<Double> halo_src_frac_it = halo_sf_list.iterator();

        int i = 0;
        final SpecS2N[] specS2Narr = new SpecS2N[_nifsParameters.getIFUMethod().equals(NifsParameters.SUMMED_APERTURE_IFU) ? 1 : sf_list.size()];

        while (src_frac_it.hasNext()) {
            double ap_diam = 1;

            if (_nifsParameters.getIFUMethod().equals(NifsParameters.SUMMED_APERTURE_IFU)) {
                while (src_frac_it.hasNext()) {
                    spec_source_frac = spec_source_frac + src_frac_it.next();
                    halo_spec_source_frac = halo_spec_source_frac + halo_src_frac_it.next();
                    ap_diam = (ap_offset_list.size() / 2);
                }
            } else {
                spec_source_frac = src_frac_it.next();
                halo_spec_source_frac = halo_src_frac_it.next();
                ap_diam = 1;
            }


            final SpecS2NLargeSlitVisitor specS2N = new SpecS2NLargeSlitVisitor(_nifsParameters.getFPMask(), pixel_size,
                    instrument.getSpectralPixelWidth(),
                    instrument.getObservingStart(),
                    instrument.getObservingEnd(),
                    instrument.getGratingDispersion_nm(),
                    instrument.getGratingDispersion_nmppix(),
                    instrument.getGratingResolution(),
                    spec_source_frac, im_qual,
                    ap_diam, number_exposures,
                    frac_with_source, exposure_time,
                    instrument.getDarkCurrent(),
                    instrument.getReadNoise(),
                    _obsDetailParameters.getSkyApertureDiameter());

            specS2N.setDetectorTransmission(instrument.getDetectorTransmision());
            specS2N.setSourceSpectrum(calcSource.sed);
            specS2N.setBackgroundSpectrum(calcSource.sky);
            specS2N.setHaloSpectrum(altair.isDefined() ? calcSource.halo.get() : (VisitableSampledSpectrum) calcSource.sed.clone());
            specS2N.setHaloImageQuality(IQcalc.getImageQuality());
            if (_nifsParameters.getAltair().isDefined())
                specS2N.setSpecHaloSourceFraction(halo_spec_source_frac);
            else
                specS2N.setSpecHaloSourceFraction(0.0);

            calcSource.sed.accept(specS2N);

            specS2Narr[i++] = specS2N;
        }

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        return new SpectroscopyResult(p, instrument, (SourceFraction) null, IQcalc, specS2Narr, (SlitThroughput) null, altair); // TODO no SFCalc and ST for Nifs
    }

}
