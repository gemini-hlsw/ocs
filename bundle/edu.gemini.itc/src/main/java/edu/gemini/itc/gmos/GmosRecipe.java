package edu.gemini.itc.gmos;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import scala.Some;
import scala.Tuple2;
import scala.collection.JavaConversions;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class performs the calculations for Gmos used for imaging.
 */
public final class GmosRecipe implements ImagingArrayRecipe, SpectroscopyArrayRecipe {

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
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength().toNanometers()))) {
                throw new RuntimeException(
                        "Please use a model line width > 1 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength().toNanometers()))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }
        }

        // report error if this does not come out to be an integer
        Validation.checkSourceFraction(_obsDetailParameters.getNumExposures(), _obsDetailParameters.getSourceFraction());
    }

    public Tuple2<ItcSpectroscopyResult, SpectroscopyResult[]> calculateSpectroscopy() {
        final SpectroscopyResult[] r = calculateSpectroscopy(createGmos());
        final List<SpcChartData> dataSets = new ArrayList<SpcChartData>() {{
            add(createGmosChart(r, 0));
            add(createGmosChart(r, 1));
        }};
        final List<SpcDataFile> dataFiles = new ArrayList<SpcDataFile>() {{
            add(new SpcDataFile(SignalData.instance(),     toFile(r, "Sig")));
            add(new SpcDataFile(BackgroundData.instance(), toFile(r, "Bac")));
            add(new SpcDataFile(SingleS2NData.instance(),  toFile(r, "Sin")));
            add(new SpcDataFile(FinalS2NData.instance(),   toFile(r, "Fin")));
        }};
        return new Tuple2<>(new ItcSpectroscopyResult(_sdParameters, _obsDetailParameters, JavaConversions.asScalaBuffer(dataSets).toList(), JavaConversions.asScalaBuffer(dataFiles).toList()), r);
    }

    protected static String toFile(final SpectroscopyResult[] results, final String filename) {
        final Gmos mainInstrument = (Gmos) results[0].instrument();
        final DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();
        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();

        final StringBuilder sb = new StringBuilder();

        for (final Gmos instrument : ccdArray) {

            final int ccdIndex = instrument.getDetectorCcdIndex();
            final int first = tv.getDetectorCcdStartIndex(ccdIndex);
            final int last = tv.getDetectorCcdEndIndex(ccdIndex, ccdArray.length);
            // REL-478: include the gaps in the text data output
            final int lastWithGap = (ccdIndex < 2 && ccdArray.length > 1)
                    ? tv.getDetectorCcdStartIndex(ccdIndex + 1)
                    : last;

            final SpectroscopyResult result = results[ccdIndex];
            final VisitableSampledSpectrum sed;
            switch (filename) {
                // TODO: why are we using the last specS2N element only? this seems fishy..?
                case "Sig": sed = result.specS2N()[result.specS2N().length - 1].getSignalSpectrum(); break;
                case "Bac": sed = result.specS2N()[result.specS2N().length - 1].getBackgroundSpectrum(); break;
                case "Sin": sed = result.specS2N()[result.specS2N().length - 1].getExpS2NSpectrum(); break;
                case "Fin": sed = result.specS2N()[result.specS2N().length - 1].getFinalS2NSpectrum(); break;
                default:
                    throw new Error();
            }
            sb.append(sed.printSpecAsString(first, lastWithGap));
        }
        return sb.toString();
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
        return SpectroscopyResult$.MODULE$.apply(p, instrument, SFcalc, IQcalc, specS2N, st);

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

    // == GMOS CHARTS

    private static SpcChartData createGmosChart(final SpectroscopyResult[] results, final int index) {
        final Gmos mainInstrument = (Gmos) results[0].instrument(); // TODO: make sure this is indeed GMOS!
        final DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();
        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();

        final boolean ifuAndNotUniform = mainInstrument.isIfuUsed() && !(results[0].source().isUniform());
        final double ifu_offset = ifuAndNotUniform ? mainInstrument.getIFU().getApertureOffsetList().iterator().next() : 0.0;
        final SpcChartType type;
        final String title;
        final String yAxis;
        switch (index) {
            case 0:
                type = SignalChart.instance();
                title = ifuAndNotUniform ? "Signal and Background (IFU element offset: " + String.format("%.2f", ifu_offset) + " arcsec)" : "Signal and Background ";
                yAxis = "e- per exposure per spectral pixel";
                break;

            case 1:
                type = S2NChart.instance();
                title = ifuAndNotUniform ? "Intermediate Single Exp and Final S/N (IFU element offset: " + String.format("%.2f", ifu_offset) + " arcsec)" : "Intermediate Single Exp and Final S/N";
                yAxis = "Signal / Noise per spectral pixel";
                break;

            default:
                throw new Error();
        }

        final List<SpcSeriesData> data = new ArrayList<>();

        for (final Gmos instrument : ccdArray) {

            final int ccdIndex = instrument.getDetectorCcdIndex();
            final String ccdName = instrument.getDetectorCcdName();
            final int first = tv.getDetectorCcdStartIndex(ccdIndex);
            final int last = tv.getDetectorCcdEndIndex(ccdIndex, ccdArray.length);

            // assign colors; CCD0 is blue, CCD1 is green, CCD2 is red
            final Color lightColor;
            final Color darkColor;
            switch(ccdIndex) {
                case 0: lightColor = ITCChart.LightBlue;  darkColor = ITCChart.DarkBlue;  break;
                case 1: lightColor = ITCChart.LightGreen; darkColor = ITCChart.DarkGreen; break;
                case 2: lightColor = ITCChart.LightRed;   darkColor = ITCChart.DarkRed;   break;
                default: throw new Error();
            }

            // draw those charts
            final SpectroscopyResult result = results[ccdIndex];
            for (int i = 0; i < result.specS2N().length; i++) {
                switch (index) {
                    case 0:
                        data.add(new SpcSeriesData(SignalData.instance(),     "Signal "           + ccdName, result.specS2N()[i].getSignalSpectrum().getData(first, last),     new Some<>(lightColor)));
                        data.add(new SpcSeriesData(BackgroundData.instance(), "SQRT(Background) " + ccdName, result.specS2N()[i].getBackgroundSpectrum().getData(first, last), new Some<>(darkColor)));
                        break;

                    case 1:
                        data.add(new SpcSeriesData(SingleS2NData.instance(),  "Single Exp S/N "   + ccdName, result.specS2N()[i].getExpS2NSpectrum().getData(first, last),     new Some<>(lightColor)));
                        data.add(new SpcSeriesData(FinalS2NData.instance(),   "Final S/N "        + ccdName, result.specS2N()[i].getFinalS2NSpectrum().getData(first, last),   new Some<>(darkColor)));
                        break;

                    default:
                        throw new Error();
                }
            }
        }

        return new SpcChartData(type, title, "Wavelength (nm)", yAxis, JavaConversions.asScalaBuffer(data).toList());
    }



}
