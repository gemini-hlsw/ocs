package edu.gemini.itc.nifs;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.ImageQualityCalculatable;
import edu.gemini.itc.operation.ImageQualityCalculationFactory;
import edu.gemini.itc.operation.SpecS2N;
import edu.gemini.itc.operation.SpecS2NLargeSlitVisitor;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.Site;
import scala.Option;
import scala.Tuple2;

import java.util.ArrayList;
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
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength().toNanometers() * 25))) {  // *25 b/c of increased resolutuion of transmission files
                throw new RuntimeException("Please use a model line width > 0.04 nm (or " + (3E5 / (_sdParameters.getELineWavelength().toNanometers() * 25)) + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }
        }

        // some general validations
        Validation.validate(_obsDetailParameters, _sdParameters);
    }

    /**
     * Performs recipe calculation.
     */
    public Tuple2<ItcSpectroscopyResult, SpectroscopyResult> calculateSpectroscopy() {
        final Nifs instrument = new Nifs(_nifsParameters, _obsDetailParameters);
        final SpectroscopyResult r = calculateSpectroscopy(instrument);
        final List<SpcChartData> dataSets = new ArrayList<>();
        for (int i = 0; i < r.specS2N().length; i++) {
            dataSets.add(createNifsSignalChart(r, i));
            dataSets.add(createNifsS2NChart(r, i));
        }
        final List<SpcDataFile> dataFiles = new ArrayList<SpcDataFile>() {{
            for (int i = 0; i < r.specS2N().length; i++) {
                add(new SpcDataFile(SignalData.instance(),     r.specS2N()[i].getSignalSpectrum().printSpecAsString()));
                add(new SpcDataFile(BackgroundData.instance(), r.specS2N()[i].getBackgroundSpectrum().printSpecAsString()));
                add(new SpcDataFile(SingleS2NData.instance(),  r.specS2N()[i].getExpS2NSpectrum().printSpecAsString()));
                add(new SpcDataFile(FinalS2NData.instance(),   r.specS2N()[i].getFinalS2NSpectrum().printSpecAsString()));
            }
        }};
        return new Tuple2<>(ItcSpectroscopyResult.apply(dataSets, dataFiles, new ArrayList<>()), r);
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
        final SpecS2N[] specS2Narr = new SpecS2N[_nifsParameters.getIFUMethod() instanceof IfuSummed ? 1 : sf_list.size()];

        while (src_frac_it.hasNext()) {
            double ap_diam = 1;

            if (_nifsParameters.getIFUMethod()  instanceof IfuSummed) {
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
        // TODO: no SFCalc and ST for Nifs, introduce specific result type? or optional values? work with null for now
        return new GenericSpectroscopyResult(p, instrument, null, IQcalc, specS2Narr, null, altair, ImagingResult.NoWarnings());
    }

    // NIFS CHARTS

    private static SpcChartData createNifsSignalChart(final SpectroscopyResult result, final int index) {
        final Nifs instrument = (Nifs) result.instrument();
        final List<Double> ap_offset_list = instrument.getIFU().getApertureOffsetList();
        final String title = instrument.getIFUMethod() instanceof IfuSummed ?
                "Signal and Background (IFU summed apertures: " +
                        instrument.getIFUNumX() + "x" + instrument.getIFUNumY() +
                        ", " + String.format("%.3f", instrument.getIFUNumX() * IFUComponent.IFU_LEN_X) + "\"x" +
                        String.format("%.3f", instrument.getIFUNumY() * IFUComponent.IFU_LEN_Y) + "\")" :
                "Signal and Background (IFU element offset: " + String.format("%.3f", ap_offset_list.get(index)) + " arcsec)";
        return Recipe$.MODULE$.createSignalChart(result, title, index);
    }

    private static SpcChartData createNifsS2NChart(final SpectroscopyResult result, final int index) {
        final Nifs instrument = (Nifs) result.instrument();
        final List<Double> ap_offset_list = instrument.getIFU().getApertureOffsetList();
        final String title = instrument.getIFUMethod() instanceof IfuSummed ?
                "Intermediate Single Exp and Final S/N \n(IFU apertures:" +
                        instrument.getIFUNumX() + "x" + instrument.getIFUNumY() +
                        ", " + String.format("%.3f", instrument.getIFUNumX() * IFUComponent.IFU_LEN_X) + "\"x" +
                        String.format("%.3f", instrument.getIFUNumY() * IFUComponent.IFU_LEN_Y) + "\")" :
                "Intermediate Single Exp and Final S/N (IFU element offset: " + String.format("%.3f", ap_offset_list.get(index)) + " arcsec)";
        return Recipe$.MODULE$.createS2NChart(result, title, index);
    }


}
