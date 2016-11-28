package edu.gemini.itc.gmos;

import edu.gemini.itc.base.GaussianMorphology;
import edu.gemini.itc.base.ImagingArrayRecipe;
import edu.gemini.itc.base.ImagingResult;
import edu.gemini.itc.base.Recipe$;
import edu.gemini.itc.base.SEDFactory;
import edu.gemini.itc.base.SpectroscopyArrayRecipe;
import edu.gemini.itc.base.SpectroscopyResult;
import edu.gemini.itc.base.USBMorphology;
import edu.gemini.itc.base.Validation;
import edu.gemini.itc.base.VisitableMorphology;
import edu.gemini.itc.base.VisitableSampledSpectrum;
import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.operation.ImageQualityCalculatable;
import edu.gemini.itc.operation.ImageQualityCalculationFactory;
import edu.gemini.itc.operation.ImagingS2NCalculatable;
import edu.gemini.itc.operation.ImagingS2NCalculationFactory;
import edu.gemini.itc.operation.PeakPixelFlux;
import edu.gemini.itc.operation.Slit;
import edu.gemini.itc.operation.Slit$;
import edu.gemini.itc.operation.SlitThroughput;
import edu.gemini.itc.operation.SourceFraction;
import edu.gemini.itc.operation.SourceFractionFactory;
import edu.gemini.itc.operation.SpecS2N;
import edu.gemini.itc.operation.SpecS2NSlitVisitor;
import edu.gemini.itc.shared.BackgroundData;
import edu.gemini.itc.shared.ChartAxis;
import edu.gemini.itc.shared.ChartAxisRange;
import edu.gemini.itc.shared.FinalS2NData;
import edu.gemini.itc.shared.GmosParameters;
import edu.gemini.itc.shared.ITCChart;
import edu.gemini.itc.shared.IfuSum;
import edu.gemini.itc.shared.ItcImagingResult;
import edu.gemini.itc.shared.ItcParameters;
import edu.gemini.itc.shared.ItcSpectroscopyResult;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.ObservingConditions;
import edu.gemini.itc.shared.S2NChart;
import edu.gemini.itc.shared.SignalChart;
import edu.gemini.itc.shared.SignalData;
import edu.gemini.itc.shared.SignalPixelChart;
import edu.gemini.itc.shared.SingleS2NData;
import edu.gemini.itc.shared.SourceDefinition;
import edu.gemini.itc.shared.SpcChartData;
import edu.gemini.itc.shared.SpcSeriesData;
import edu.gemini.itc.shared.TelescopeDetails;
import scala.Option;
import scala.Some;
import scala.collection.JavaConversions;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class performs the calculations for Gmos used for imaging.
 */
public final class GmosRecipe implements ImagingArrayRecipe, SpectroscopyArrayRecipe {

    private final ItcParameters p;
    private final Gmos mainInstrument;
    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs a GmosRecipe given the parameters. Useful for testing.
     */
    public GmosRecipe(final ItcParameters p, final GmosParameters instr)

    {
        this.p                  = p;
        mainInstrument          = createGmos(instr, p.observation());
        _sdParameters           = p.source();
        _obsDetailParameters    = p.observation();
        _obsConditionParameters = p.conditions();
        _telescope              = p.telescope();

        // some general validations
        Validation.validate(mainInstrument, _obsDetailParameters, _sdParameters);
    }

    public ItcImagingResult serviceResult(final ImagingResult[] r) {
        return Recipe$.MODULE$.serviceResult(r);
    }

    public ItcSpectroscopyResult serviceResult(final SpectroscopyResult[] r) {
        final List<List<SpcChartData>> groups = new ArrayList<>();
        // The array specS2N represents the different IFUs, for each one we produce a separate set of charts.
        // For completeness: The result array holds the results for the different CCDs. For each CCD
        // the specS2N array holds the single result or the different IFU results. This should be made more obvious.
        for (int i = 0; i < r[0].specS2N().length; i++) {
            final List<SpcChartData> charts = new ArrayList<>();
            charts.add(createSignalChart(r, i));
            charts.add(createS2NChart(r, i));
            // IFU-2 case has an additional chart with signal in pixel space
            if (((Gmos) r[0].instrument()).isIfu2()) {
                charts.add(createSignalPixelChart(r, i));
            }
            groups.add(charts);
        }
        return Recipe$.MODULE$.serviceGroupedResult(r, groups);
    }

    public SpectroscopyResult[] calculateSpectroscopy() {
        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();
        final SpectroscopyResult[] results = new SpectroscopyResult[ccdArray.length];
        for (int i = 0; i < ccdArray.length; i++) {
            final Gmos instrument = ccdArray[i];
            results[i] = calculateSpectroscopy(mainInstrument, instrument, ccdArray.length);
        }
        return results;
    }

    public ImagingResult[] calculateImaging() {
        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();
        final List<ImagingResult> results = new ArrayList<>();
        for (final Gmos instrument : ccdArray) {
            results.add(calculateImagingDo(instrument));
        }
        return results.toArray(new ImagingResult[results.size()]);
    }

    private Gmos createGmos(final GmosParameters parameters, final ObservationDetails observationDetails) {
        switch (parameters.site()) {
            case GN: return new GmosNorth(parameters, observationDetails, 0);
            case GS: return new GmosSouth(parameters, observationDetails, 0);
            default: throw new Error("invalid site");
        }
    }

    // TODO: bring mainInstrument and instrument together
    private SpectroscopyResult calculateSpectroscopy(final Gmos mainInstrument, final Gmos instrument, final int detectorCount) {

        final SpecS2NSlitVisitor[] specS2N;

        final SEDFactory.SourceResult src = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope);
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

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        final double dark_current = instrument.getDarkCurrent();
        final double read_noise = instrument.getReadNoise();

        // TODO: why, oh why?
        final double im_qual = _sdParameters.isUniform() ? 10000 : IQcalc.getImageQuality();

        // ==== IFU
        if (instrument.isIfuUsed()) {

            final VisitableMorphology morph = _sdParameters.isUniform() ? new USBMorphology() : new GaussianMorphology(IQcalc.getImageQuality());
            morph.accept(instrument.getIFU().getAperture());

            final List<Double> sf_list = instrument.getIFU().getFractionOfSourceInAperture();
            final double slitLength = 5 / instrument.getSpatialBinning();
            final Slit slit = Slit$.MODULE$.apply(instrument.getSlitWidth(), slitLength, instrument.getPixelSize());

            // for uniform sources the result is the same regardless of the IFU offsets/position
            // in this case we only calculate and display the result of the first IFU element
            // For the summed IFU we will also only have a single S/N value.
            int ifusToShow;
            if (instrument.isIfuUsed() &&  _obsDetailParameters.analysisMethod() instanceof IfuSum) {
                ifusToShow = 1;
            } else {
                ifusToShow = sf_list.size();
            }
            specS2N = new SpecS2NSlitVisitor[ifusToShow];

            // process all IFU elements
            double totalspsf = 0;
            if (instrument.isIfuUsed() &&  _obsDetailParameters.analysisMethod() instanceof IfuSum) {
                for (Double aSf_list : sf_list) {
                    final double spsf = aSf_list;
                    totalspsf += spsf;
                }
            }

            for (int i = 0; i < ifusToShow; i++) {
                double spsf;
                if (instrument.isIfuUsed() &&  _obsDetailParameters.analysisMethod() instanceof IfuSum) {
                    spsf = totalspsf;
                } else {
                    spsf = sf_list.get(i);
                }
                final Slit ifuSlit = Slit$.MODULE$.apply(instrument.getSlitWidth(), slitLength, instrument.getPixelSize());
                specS2N[i] = new SpecS2NSlitVisitor(
                        ifuSlit,
                        instrument.disperser.get(),
                        new SlitThroughput(spsf, spsf),
                        instrument.getSpectralPixelWidth(),
                        instrument.getObservingStart(),
                        instrument.getObservingEnd(),
                        im_qual,
                        read_noise,
                        dark_current * instrument.getSpatialBinning() * instrument.getSpectralBinning(),
                        _obsDetailParameters);

                specS2N[i].setCcdPixelRange(firstCcdIndex, lastCcdIndex);
                specS2N[i].setSourceSpectrum(src.sed);
                specS2N[i].setBackgroundSpectrum(src.sky);

                src.sed.accept(specS2N[i]);

            }

            return new SpectroscopyResult(p, instrument, IQcalc, specS2N, slit, sf_list.get(0), Option.empty());

        // ==== SLIT
        } else {

            final Slit slit = Slit$.MODULE$.apply(_sdParameters, _obsDetailParameters, instrument, instrument.getSlitWidth(), IQcalc.getImageQuality());
            final SlitThroughput throughput = new SlitThroughput(_sdParameters, slit, IQcalc.getImageQuality());

            specS2N = new SpecS2NSlitVisitor[1];
            specS2N[0] = new SpecS2NSlitVisitor(
                    slit,
                    instrument.disperser.get(),
                    throughput,
                    instrument.getSpectralPixelWidth(),
                    instrument.getObservingStart(),
                    instrument.getObservingEnd(),
                    im_qual,
                    read_noise,
                    dark_current * instrument.getSpatialBinning() * instrument.getSpectralBinning(),
                    _obsDetailParameters);


            specS2N[0].setCcdPixelRange(firstCcdIndex, lastCcdIndex);
            specS2N[0].setSourceSpectrum(src.sed);
            specS2N[0].setBackgroundSpectrum(src.sky);

            src.sed.accept(specS2N[0]);

            return new SpectroscopyResult(p, instrument, IQcalc, specS2N, slit, throughput.throughput(), Option.empty());
        }

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

        final SEDFactory.SourceResult src = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope);
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

        final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc, sed_integral, sky_integral);
        IS2Ncalc.calculate();

        return ImagingResult.apply(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc);

    }

    // == GMOS CHARTS

    /** Creates the signal in wavelength space chart. */
    private static SpcChartData createSignalChart(final SpectroscopyResult[] results, final int i) {
        final Gmos mainInstrument = (Gmos) results[0].instrument(); // This must be GMOS here.
        final Gmos[] ccdArray     = mainInstrument.getDetectorCcdInstruments();
        final DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();

        final boolean ifuUsed   = mainInstrument.isIfuUsed();
        double  ifuOffset = ifuUsed ? mainInstrument.getIFU().getApertureOffsetList().get(i) : 0.0;
        if (mainInstrument.isIfuUsed() && mainInstrument.getIfuMethod().get() instanceof IfuSum) {
            ifuOffset = 0.0;
        }
        final List<ChartAxis> axes = new ArrayList<>();
        String title = "Signal and SQRT(Background) in one pixel" + (ifuUsed ? "\nIFU element offset: " + String.format("%.2f", ifuOffset) + " arcsec" : "");
        if (mainInstrument.isIfuUsed() &&  mainInstrument.getIfuMethod().get() instanceof IfuSum) {
            final IfuSum ifu = (IfuSum) mainInstrument.getIfuMethod().get();
            title = "Signal and SQRT(Background)\nIFU elements summed in a radius of " + String.format("%.2f", ifu.num()) + " arcsec";
        }
        final ChartAxis xAxis = ChartAxis.apply("Wavelength (nm)");
        final ChartAxis yAxis = ChartAxis.apply("e- per exposure per spectral pixel");

        final List<SpcSeriesData> data = new ArrayList<>();

        for (final Gmos instrument : ccdArray) {
            final String ccdName = results.length > 1 ? instrument.getDetectorCcdName() : "";
            final int ccdIndex   = instrument.getDetectorCcdIndex();
            final int first      = tv.getDetectorCcdStartIndex(ccdIndex);
            final int last       = tv.getDetectorCcdEndIndex(ccdIndex, ccdArray.length);
            final SpectroscopyResult result = results[ccdIndex];
            data.addAll(sigChartSeries(mainInstrument, ccdIndex, result.specS2N()[i], first, last, tv, ccdName));
        }

        final scala.collection.immutable.List<SpcSeriesData> scalaData = JavaConversions.asScalaBuffer(data).toList();
        final scala.collection.immutable.List<ChartAxis>     scalaAxes = JavaConversions.asScalaBuffer(axes).toList();
        return new SpcChartData(SignalChart.instance(), title, xAxis, yAxis, scalaData, scalaAxes);
    }

    /** Creates the signal to noise in wavelength space chart. */
    private static SpcChartData createS2NChart(final SpectroscopyResult[] results, final int i) {
        final Gmos mainInstrument  = (Gmos) results[0].instrument(); // This must be GMOS here.
        final Gmos[] ccdArray      = mainInstrument.getDetectorCcdInstruments();
        final DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();

        final boolean ifuUsed   = mainInstrument.isIfuUsed();
        final double  ifuOffset = ifuUsed ? mainInstrument.getIFU().getApertureOffsetList().get(i) : 0.0;
        final List<ChartAxis> axes = new ArrayList<>();

        String title = "Intermediate Single Exp and Final S/N in aperture" + (ifuUsed ? "\nIFU element offset: " + String.format("%.2f", ifuOffset) + " arcsec" : "");
        if (mainInstrument.isIfuUsed() &&  mainInstrument.getIfuMethod().get() instanceof IfuSum) {
            final IfuSum ifu = (IfuSum) mainInstrument.getIfuMethod().get();
            title = "Intermediate Single Exp and Final S/N in aperture\nIFU elements summed in a radius of " + String.format("%.2f", ifu.num()) + " arcsec";
        }
        final ChartAxis xAxis = ChartAxis.apply("Wavelength (nm)");
        final ChartAxis yAxis = ChartAxis.apply("Signal / Noise per spectral pixel");

        final List<SpcSeriesData> data = new ArrayList<>();

        for (final Gmos instrument : ccdArray) {
            final String ccdName = results.length > 1 ? instrument.getDetectorCcdName() : "";
            final int ccdIndex   = instrument.getDetectorCcdIndex();
            final int first      = tv.getDetectorCcdStartIndex(ccdIndex);
            final int last       = tv.getDetectorCcdEndIndex(ccdIndex, ccdArray.length);
            final SpectroscopyResult result = results[ccdIndex];
            data.addAll(s2nChartSeries(mainInstrument, ccdIndex, result.specS2N()[i], first, last, tv, ccdName));
        }

        final scala.collection.immutable.List<SpcSeriesData> scalaData = JavaConversions.asScalaBuffer(data).toList();
        final scala.collection.immutable.List<ChartAxis>     scalaAxes = JavaConversions.asScalaBuffer(axes).toList();
        return new SpcChartData(S2NChart.instance(), title, xAxis, yAxis, scalaData, scalaAxes);
    }

    /** Creates the signal in pixel space chart. */
    private static SpcChartData createSignalPixelChart(final SpectroscopyResult[] results, final int i) {
        final Gmos mainInstrument = (Gmos) results[0].instrument(); // This must be GMOS here.
        final Gmos[] ccdArray     = mainInstrument.getDetectorCcdInstruments();
        final DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();

        final boolean ifuUsed   = mainInstrument.isIfuUsed();
        final double  ifuOffset = ifuUsed ? mainInstrument.getIFU().getApertureOffsetList().get(i) : 0.0;

        final List<ChartAxis> axes = new ArrayList<>();
        final String title    = "Pixel Signal and SQRT(Background)" + (ifuUsed ? "\nIFU element offset: " + String.format("%.2f", ifuOffset) + " arcsec" : "");
        final ChartAxis xAxis = new ChartAxis("Pixels", true, new Some<>(new ChartAxisRange(0, tv.fullArrayPix())));
        final ChartAxis yAxis = ChartAxis.apply("e- per exposure per spectral pixel");

        axes.add(new ChartAxis("Wavelength (nm) (Red)",  false, new Some<>(new ChartAxisRange(tv.ifu2RedStart(),  tv.ifu2RedEnd()))));
        axes.add(new ChartAxis("Wavelength (nm) (Blue)", false, new Some<>(new ChartAxisRange(tv.ifu2BlueStart(), tv.ifu2BlueEnd()))));

        final List<SpcSeriesData> data = new ArrayList<>();

        for (final Gmos instrument : ccdArray) {
            final String ccdName = results.length > 1 ? " " + instrument.getDetectorCcdName() : "";
            final int ccdIndex   = instrument.getDetectorCcdIndex();
            final int first      = tv.getDetectorCcdStartIndex(ccdIndex);
            final int last       = tv.getDetectorCcdEndIndex(ccdIndex, ccdArray.length);
            final SpectroscopyResult result = results[ccdIndex];
            data.addAll(signalPixelChartSeries(result.specS2N()[i], first, last, tv, ccdName));
        }

        final scala.collection.immutable.List<SpcSeriesData> scalaData = JavaConversions.asScalaBuffer(data).toList();
        final scala.collection.immutable.List<ChartAxis>     scalaAxes = JavaConversions.asScalaBuffer(axes).toList();
        return new SpcChartData(SignalPixelChart.instance(), title, xAxis, yAxis, scalaData, scalaAxes);
    }

    /** Creates all data series for the signal in wavelength space chart. */
    private static List<SpcSeriesData> sigChartSeries(final Gmos mainInstrument, final int ccdIndex, final SpecS2N result, final int start, final int end, final DetectorsTransmissionVisitor tv, final String ccdName) {
        final String sigTitle = "Signal" + ccdName;
        final String bkgTitle = "SQRT(Background)" + ccdName;

        final VisitableSampledSpectrum sig = ((VisitableSampledSpectrum) result.getSignalSpectrum().clone());
        final VisitableSampledSpectrum bkg = ((VisitableSampledSpectrum) result.getBackgroundSpectrum().clone());

        // For the IFU-2 case (or IFU in general?) it is difficult to show the gaps at the
        // right positions so for now we don't show them at all in the wavelength charts.
        if (!mainInstrument.isIfu2()) {
            // Note: Accept() does its magic in-place and will destroy the original values!
            sig.accept(tv);
            bkg.accept(tv);
        }

        final double[][] signalWithGaps = sig.getData(start, end);
        final double[][] backgrWithGaps = bkg.getData(start, end);

        // ===== fix gap borders to avoid signal/s2n spikes
        if (mainInstrument.getDetectorCcdInstruments().length > 1) {
            fixGapBorders(signalWithGaps);
            fixGapBorders(backgrWithGaps);
        }
        // =====

        final List<SpcSeriesData> series = new ArrayList<>();
        series.add(new SpcSeriesData(SignalData.instance(),     sigTitle, signalWithGaps, new Some<>(ccdDarkColor(ccdIndex))));
        series.add(new SpcSeriesData(BackgroundData.instance(), bkgTitle, backgrWithGaps, new Some<>(ccdLightColor(ccdIndex))));

        return series;
    }

    /** Creates all data series for the signal to noise in wavelength space chart. */
    private static List<SpcSeriesData> s2nChartSeries(final Gmos mainInstrument, final int ccdIndex, final SpecS2N result, final int start, final int end, final DetectorsTransmissionVisitor tv, final String ccdName) {
        final String s2nTitle = "Single Exp S/N" + ccdName;
        final String finTitle = "Final S/N" + ccdName;

        // For the IFU-2 case (or IFU in general?) it is difficult to show the gaps at the
        // right positions so for now we don't show them at all in the wavelength charts.
        if (!mainInstrument.isIfu2()) {
            // Note: Accept() does its magic in-place and will destroy the original values!
            result.getExpS2NSpectrum().accept(tv);
            result.getFinalS2NSpectrum().accept(tv);
        }

        final double[][] s2n = result.getExpS2NSpectrum().getData(start, end);
        final double[][] fin = result.getFinalS2NSpectrum().getData(start, end);

        // ===== fix gap borders to avoid signal/s2n spikes
        if (mainInstrument.getDetectorCcdInstruments().length > 1) {
            fixGapBorders(s2n);
            fixGapBorders(fin);
        }
        // =====

        final List<SpcSeriesData> series = new ArrayList<>();
        series.add(new SpcSeriesData(SingleS2NData.instance(), s2nTitle, s2n,   new Some<>(ccdDarkColor(ccdIndex))));
        series.add(new SpcSeriesData(FinalS2NData.instance(),  finTitle, fin,   new Some<>(ccdLightColor(ccdIndex))));

        return series;
    }

    /** Creates all data series for the signal in pixel space chart. */
    private static List<SpcSeriesData> signalPixelChartSeries(final SpecS2N result, final int start, final int end, final DetectorsTransmissionVisitor tv, final String ccdName) {
        // This type of chart is currently only used for IFU-2. It transforms the signal from
        // wavelength space to pixel space and displays it as a chart including gaps between CCDs.

        // those values are still original, no gaps added, do transformation to pixel space first
        final VisitableSampledSpectrum red = ((VisitableSampledSpectrum) result.getSignalSpectrum().clone());
        final VisitableSampledSpectrum blu = ((VisitableSampledSpectrum) result.getSignalSpectrum().clone());

        final VisitableSampledSpectrum redBkg = ((VisitableSampledSpectrum) result.getBackgroundSpectrum().clone());
        final VisitableSampledSpectrum bluBkg = ((VisitableSampledSpectrum) result.getBackgroundSpectrum().clone());

        // to pixel transform also adds gaps (i.e. sets values to zero..)
        final double shift = tv.ifu2shift();
        final double shiftedRed[][]     = tv.toPixelSpace(red.getData(start, end),     shift);
        final double shiftedBlue[][]    = tv.toPixelSpace(blu.getData(start, end),    -shift);
        final double shiftedRedBkg[][]  = tv.toPixelSpace(redBkg.getData(start, end),  shift);
        final double shiftedBlueBkg[][] = tv.toPixelSpace(bluBkg.getData(start, end), -shift);

        final List<SpcSeriesData> series = new ArrayList<>();
        series.add(new SpcSeriesData(SignalData.instance(),     "Red Signal"            + ccdName, shiftedRed,     new Some<>(ITCChart.DarkRed)));
        series.add(new SpcSeriesData(SignalData.instance(),     "Blue Signal"           + ccdName, shiftedBlue,    new Some<>(ITCChart.DarkBlue)));
        series.add(new SpcSeriesData(BackgroundData.instance(), "SQRT(Red Background)"  + ccdName, shiftedRedBkg,  new Some<>(ITCChart.LightRed)));
        series.add(new SpcSeriesData(BackgroundData.instance(), "SQRT(Blue Background)" + ccdName, shiftedBlueBkg, new Some<>(ITCChart.LightBlue)));

        return series;
    }

    /** Gets the light color for the given CCD. */
    private static Color ccdLightColor(final int ccdIndex) {
        switch(ccdIndex) {
            case 0:  return ITCChart.LightBlue;
            case 1:  return ITCChart.LightGreen;
            case 2:  return ITCChart.LightRed;
            default: throw new Error();
        }
    }

    /** Gets the dark color for the given CCD. */
    private static Color ccdDarkColor(final int ccdIndex) {
        switch(ccdIndex) {
            case 0:  return ITCChart.DarkBlue;
            case 1:  return ITCChart.DarkGreen;
            case 2:  return ITCChart.DarkRed;
            default: throw new Error();
        }
    }

    // In the multi-CCD case we have to force the first and last y values to 0 to cancel out signal and s2n spikes
    // caused by interpolation, resampling or some other effect of how data is calculated around CCD gaps.
    // TODO: DetectorTransmissionVisitor needs a serious overhaul so that this behavior becomes more predictable.
    private static void fixGapBorders(double[][] data) {
        data[1][0]                  = 0.0;
        data[1][data[1].length - 1] = 0.0;
    }
}
