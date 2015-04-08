package edu.gemini.itc.gmos;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class performs the calculations for Gmos used for imaging.
 */
public final class GmosRecipe {

    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final GmosParameters _gmosParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs a GmosRecipe given the parameters. Useful for testing.
     */
    public GmosRecipe(final SourceDefinition sdParameters,
                      final ObservationDetails obsDetailParameters,
                      final ObservingConditions obsConditionParameters,
                      final GmosParameters gmosParameters,
                      final TelescopeDetails telescope)

    {
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _gmosParameters = gmosParameters;
        _telescope = telescope;

        validateInputParamters();
    }

    private void validateInputParamters() {
        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE)) {
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength() * 1000))) {
                throw new RuntimeException(
                        "Please use a model line width > 1 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength() * 1000))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }
        }

        // report error if this does not come out to be an integer
        Validation.checkSourceFraction(_obsDetailParameters.getNumExposures(), _obsDetailParameters.getSourceFraction());
    }

    public SpectroscopyResult[] calculateSpectroscopy() {
        return calculateSpectroscopy(createGmos());
    }

    public ImagingResult[] calculateImaging() {
        return calculateImaging(createGmos());
    }

    private SpectroscopyResult[] calculateSpectroscopy(final Gmos mainInstrument) {
        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();
        final SpectroscopyResult[] results = new SpectroscopyResult[ccdArray.length];
        for (int i = 0; i < ccdArray.length; i++) {
            final Gmos instrument = ccdArray[i];
            results[i] = calculateSpectroscopy(mainInstrument, instrument, ccdArray.length);
        }
        return results;
    }

    private ImagingResult[] calculateImaging(final Gmos mainInstrument) {
        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();
        final ImagingResult[] results = new ImagingResult[ccdArray.length];
        for (int i = 0; i < ccdArray.length; i++) {
            final Gmos instrument = ccdArray[i];
            results[i] = calculateImagingDo(instrument);
        }
        return results;
    }

    private Gmos createGmos() {
        switch (_gmosParameters.site()) {
            case GN: return new GmosNorth(_gmosParameters, _obsDetailParameters, 0);
            case GS: return new GmosSouth(_gmosParameters, _obsDetailParameters, 0);
            default: throw new Error("invalid site");
        }
    }

    // TODO: bring mainInstrument and instrument together
    private SpectroscopyResult calculateSpectroscopy(final Gmos mainInstrument, final Gmos instrument, final int detectorCount) {

        final SpecS2NLargeSlitVisitor[] specS2N;
        final SlitThroughput st;

        final SEDFactory.SourceResult src = SEDFactory.calculate(instrument, _gmosParameters.site(), ITCConstants.VISIBLE, _sdParameters, _obsConditionParameters, _telescope);
        final int ccdIndex = instrument.getDetectorCcdIndex();
        final DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();
        final int firstCcdIndex = tv.getDetectorCcdStartIndex(ccdIndex);
        final int lastCcdIndex = tv.getDetectorCcdEndIndex(ccdIndex, detectorCount);

        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        final double pixel_size = instrument.getPixelSize();
        double ap_diam;
        double source_fraction;
        List<Double> sf_list = new ArrayList<>();

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();
        double im_qual = IQcalc.getImageQuality();

        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);
        if (!instrument.isIfuUsed()) {
            source_fraction = SFcalc.getSourceFraction();
        } else {
            final VisitableMorphology morph;
            if (!_sdParameters.isUniform()) {
                morph = new GaussianMorphology(im_qual);
            } else {
                morph = new USBMorphology();
            }
            morph.accept(instrument.getIFU().getAperture());
            // for now just a single item from the list
            sf_list = instrument.getIFU().getFractionOfSourceInAperture();
            source_fraction = sf_list.get(0);
        }


        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        double spec_source_frac;
        final int number_exposures = _obsDetailParameters.getNumExposures();
        final double frac_with_source = _obsDetailParameters.getSourceFraction();
        final double dark_current = instrument.getDarkCurrent();
        final double exposure_time = _obsDetailParameters.getExposureTime();
        final double read_noise = instrument.getReadNoise();

        // ObservationMode Imaging or spectroscopy
        if (!instrument.isIfuUsed()) {
            if (!_obsDetailParameters.isAutoAperture()) {
                st = new SlitThroughput(im_qual, _obsDetailParameters.getApertureDiameter(), pixel_size, instrument.getSlitWidth());
            } else {
                st = new SlitThroughput(im_qual, pixel_size, instrument.getSlitWidth());
            }
            ap_diam = st.getSpatialPix();
            spec_source_frac = st.getSlitThroughput();
        } else {
            st = null; // TODO: how to deal with no ST in case of IFU?
            spec_source_frac = source_fraction;
            ap_diam = 5 / instrument.getSpatialBinning();
        }

        // For the usb case we want the resolution to be determined by the
        // slit width and not the image quality for a point source.
        if (_sdParameters.isUniform()) {
            im_qual = 10000;

            if (!instrument.isIfuUsed()) {

                if (!_obsDetailParameters.isAutoAperture()) {
                    spec_source_frac = instrument.getSlitWidth() * ap_diam * pixel_size;
                } else {
                    ap_diam = new Double(1 / (instrument.getSlitWidth() * pixel_size) + 0.5).intValue();
                    spec_source_frac = 1;
                }
            }
        }

        if (instrument.isIfuUsed() && !_sdParameters.isUniform()) {
            specS2N = new SpecS2NLargeSlitVisitor[sf_list.size()];
            for (int i = 0; i < sf_list.size(); i++) {
                final double spsf = sf_list.get(i);
                specS2N[i] = new SpecS2NLargeSlitVisitor(
                        instrument.getSlitWidth(),
                        pixel_size,
                        instrument.getSpectralPixelWidth(),
                        instrument.getObservingStart(),
                        instrument.getObservingEnd(),
                        instrument.getGratingDispersion_nm(),
                        instrument.getGratingDispersion_nmppix(),
                        instrument.getGratingResolution(),
                        spsf,
                        im_qual,
                        ap_diam,
                        number_exposures,
                        frac_with_source,
                        exposure_time,
                        dark_current * instrument.getSpatialBinning() * instrument.getSpectralBinning(),
                        read_noise,
                        _obsDetailParameters.getSkyApertureDiameter());

                specS2N[i].setDetectorTransmission(mainInstrument.getDetectorTransmision());
                specS2N[i].setCcdPixelRange(firstCcdIndex, lastCcdIndex);
                specS2N[i].setSourceSpectrum(src.sed);
                specS2N[i].setBackgroundSpectrum(src.sky);
                src.sed.accept(specS2N[i]);

            }
        } else {
            specS2N = new SpecS2NLargeSlitVisitor[1];
            specS2N[0] = new SpecS2NLargeSlitVisitor(
                    instrument.getSlitWidth(),
                    pixel_size,
                    instrument.getSpectralPixelWidth(),
                    instrument.getObservingStart(),
                    instrument.getObservingEnd(),
                    instrument.getGratingDispersion_nm(),
                    instrument.getGratingDispersion_nmppix(),
                    instrument.getGratingResolution(),
                    spec_source_frac,
                    im_qual,
                    ap_diam,
                    number_exposures,
                    frac_with_source,
                    exposure_time,
                    dark_current * instrument.getSpatialBinning() * instrument.getSpectralBinning(),
                    read_noise,
                    _obsDetailParameters.getSkyApertureDiameter());

            specS2N[0].setDetectorTransmission(mainInstrument.getDetectorTransmision());
            specS2N[0].setCcdPixelRange(firstCcdIndex, lastCcdIndex);
            specS2N[0].setSourceSpectrum(src.sed);
            specS2N[0].setBackgroundSpectrum(src.sky);
            src.sed.accept(specS2N[0]);

        }

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        return SpectroscopyResult.apply(p, instrument, SFcalc, IQcalc, specS2N, st);

    }

    private ImagingResult calculateImagingDo(final Gmos instrument) {

        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        final SEDFactory.SourceResult src = SEDFactory.calculate(instrument, _gmosParameters.site(), ITCConstants.VISIBLE, _sdParameters, _obsConditionParameters, _telescope);
        final double sed_integral = src.sed.getIntegral();
        final double sky_integral = src.sky.getIntegral();

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();
        final double im_qual = IQcalc.getImageQuality();

        // Calculate the Fraction of source in the aperture
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);

        // Calculate the Peak Pixel Flux
        final double peak_pixel_count = PeakPixelFlux.calculate(instrument, _sdParameters, _obsDetailParameters, SFcalc, im_qual, sed_integral, sky_integral);

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        final int number_exposures = _obsDetailParameters.getNumExposures();
        final double frac_with_source = _obsDetailParameters.getSourceFraction();
        // report error if this does not come out to be an integer
        Validation.checkSourceFraction(number_exposures, frac_with_source);

        final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc, sed_integral, sky_integral);
        IS2Ncalc.calculate();

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        return ImagingResult.apply(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc);

    }

}
