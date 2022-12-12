package edu.gemini.itc.ghost;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.ghost.GhostType;
import scala.Option;
import scala.Some;
import scala.collection.JavaConversions;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * This class performs the calculations for Ghost used for imaging.
 */
public final class GHostRecipe  {

    private static final Logger Log = Logger.getLogger(GHostRecipe.class.getName());
    private final ItcParameters p;
    private final Ghost[] _mainInstrument;
    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs a GhostRecipe given the parameters. Useful for testing.
     */
    public GHostRecipe(final ItcParameters p, final GhostParameters instr)

    {
        this.p                  = p;
        _mainInstrument         = createGhost(instr, p.observation());
        _sdParameters           = p.source();
        _obsDetailParameters    = p.observation();
        _obsConditionParameters = p.conditions();
        _telescope              = p.telescope();

        // some general validations
        Validation.validate(_mainInstrument[0], _obsDetailParameters, _sdParameters);
    }
/*
    public ItcSpectroscopyResult serviceResult(final SpectroscopyResult[] r, final boolean headless) {
        final List<SpcChartData> dataSets = new ArrayList<SpcChartData>();
        dataSets.add(Recipe$.MODULE$.createSignalChart(r[0], 0));
        dataSets.add(Recipe$.MODULE$.createS2NChart(r[0], _mainInstrument[0].getCCDType() ,0));
        return Recipe$.MODULE$.serviceResult(r[0], dataSets, headless);
    }
  */

    public ItcSpectroscopyResult serviceResult(final SpectroscopyResult[] r, final boolean headless) {
        final List<List<SpcChartData>> groups = new ArrayList<>();
        final List<SpcChartData> dataSets1 = new ArrayList<SpcChartData>();
        final List<SpcChartData> dataSets2 = new ArrayList<SpcChartData>();
        dataSets1.add(Recipe$.MODULE$.createSignalChart(r[0], 0));
        dataSets1.add(Recipe$.MODULE$.createS2NChart(r[0], _mainInstrument[0].getCCDType() ,0));
        groups.add(dataSets1);
        dataSets2.add(Recipe$.MODULE$.createSignalChart(r[1], 0));
        dataSets2.add(Recipe$.MODULE$.createS2NChart(r[1], _mainInstrument[1].getCCDType() ,0));
        groups.add(dataSets2);
        return Recipe$.MODULE$.serviceGroupedResult(r, groups, headless);
    }



    private static SpcChartData createS2NChart(final SpectroscopyResult result, final int i) {
        String title = "Intermediate Single Exp and Final S/N in aperture";
        final ChartAxis xAxis = ChartAxis.apply("Wavelength (nm)");
        final ChartAxis yAxis = ChartAxis.apply("Signal / Noise per spectral pixel");
        final List<SpcSeriesData> data = new ArrayList<>();
        data.add(new SpcSeriesData(FinalS2NData.instance(), "Single Exp S/N ", result.specS2N()[0].getExpS2NSpectrum().getData(), new Some<>(ITCChart.LightBlue)));
        data.add(new SpcSeriesData(FinalS2NData.instance(), "Final S/N ", result.specS2N()[0].getFinalS2NSpectrum().getData(), new Some<>(ITCChart.DarkRed)));
        final List<ChartAxis> axes = new ArrayList<>();
        final scala.collection.immutable.List<SpcSeriesData> scalaData = JavaConversions.asScalaBuffer(data).toList();
        final scala.collection.immutable.List<ChartAxis>     scalaAxes = JavaConversions.asScalaBuffer(axes).toList();
        return new SpcChartData(S2NChart.instance(), title, xAxis, yAxis, scalaData, scalaAxes);
    }


    public SpectroscopyResult[] calculateSpectroscopy() {
        int length = _mainInstrument.length;
        final SpectroscopyResult[] results = new SpectroscopyResult[length];
        for (int i = 0; i < length; i++) {
            results[i] = calculateSpectroscopy(_mainInstrument[i], length);
        }
        Log.info("calculateSpectroscopy getImageQuality[0]: " +
                            results[0].iqCalc().getImageQuality() + " [1]: "+ results[1].iqCalc().getImageQuality());
        return results;
    }

    private SpectroscopyResult calculateSpectroscopy(Ghost instrument, int length) {

        // In this first step is defined a source energy function (as function of wavelength).
        // The source can be BlackBodySpectrum, EmissionLineSpectrum, PowerLawSpectrum, UserDefinedSpectrum, a source from
        // resources/sed/stellar/ or resources/sed/non_stellar source
        // calculates: redshifted SED too.
        // The units of the source normalized is ph/s/m^2/nm
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();
        SEDFactory.SourceResult sed = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope);
        final VisitableMorphology morph = _sdParameters.isUniform() ? new USBMorphology() : new GaussianMorphology(IQcalc.getImageQuality());
        morph.accept(instrument.getIFU().getAperture());

        final List<Double> sf_list = instrument.getIFU().getFractionOfSourceInAperture();
        double totalspsf = 0;  // total source fraction in the aperture
        double numfibers = 0;  // number of fibers being summed
        for (Double aSf_list : sf_list) {
            final double spsf = aSf_list;
            totalspsf += spsf;
            numfibers += 1;
        }

        final Slit slit = Slit$.MODULE$.apply(instrument.getSlitWidth(),
                                    instrument.getSlitLength()/instrument.getSpatialBinning(),
                                              instrument.getPixelSize());
        final SlitThroughput throughput = new SlitThroughput(totalspsf, sf_list.get((sf_list.size() - 1) / 2));

        ApertureComposite IFUApertures = (ApertureComposite) instrument.getIFU().getAperture();
        List<ApertureComponent> list = IFUApertures.getApertureList();
        for( ApertureComponent ac : list) {
            HexagonalAperture h = (HexagonalAperture) ac;
            Log.info("x: "+ h.getIfuPosX() + " y: "+ h.getIfuPosY() + " fractionSource: "+  h.getFractionOfSourceInAperture());
        }
        Log.info("slitLength: "+ instrument.getSlitLength() + " throughtput: "+ totalspsf +
                           " onePIx: " + sf_list.get((sf_list.size() - 1) / 2) + " numfibers: "+ numfibers + " centerFiber: "+ (sf_list.size() - 1) / 2);

         final SpecS2NSlitVisitor specS2N = new SpecS2NSlitVisitor(
                slit,
                instrument.disperser.get(),
                throughput,
                instrument.getSpectralPixelWidth(),   // using the grating dispersion * spectralBinning
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                IQcalc.getImageQuality(),
                instrument.getReadNoise(),
                instrument.getDarkCurrent() * instrument.getSpatialBinning() * instrument.getSpectralBinning(),
                _obsDetailParameters);

        specS2N.setSourceSpectrum(sed.sed);
        specS2N.setBackgroundSpectrum(sed.sky);
        sed.sed.accept(specS2N);
        final SpecS2N[] specS2Narr = new SpecS2N[] {specS2N};

        return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, throughput.throughput(), Option.empty());
    }


    private Ghost[] createGhost(final GhostParameters parameters, final ObservationDetails observationDetails) {
        return new Ghost[] {new Ghost(parameters, observationDetails, GhostType.DetectorManufacturer.BLUE),
                            new Ghost(parameters, observationDetails, GhostType.DetectorManufacturer.RED)};
    }

    // SpecS2N implementation to hold results for IFU mode calculations.
    // It contains a set of values per each slit.
    class GhostSpecS2N implements SpecS2N {

        private final VisitableSampledSpectrum[] signal;
        private final VisitableSampledSpectrum[] background;
        private final VisitableSampledSpectrum[] exps2n;
        private final VisitableSampledSpectrum[] fins2n;
        private final int numberOfSlits;

        public GhostSpecS2N(int numberOfSlits) {
            this.numberOfSlits = numberOfSlits;
            signal = new VisitableSampledSpectrum[numberOfSlits];
            background = new VisitableSampledSpectrum[numberOfSlits];
            exps2n = new VisitableSampledSpectrum[numberOfSlits];
            fins2n = new VisitableSampledSpectrum[numberOfSlits];
        }

        public void setSlitS2N(
                int slitIndex,
                final VisitableSampledSpectrum signal,
                final VisitableSampledSpectrum background,
                final VisitableSampledSpectrum exps2n,
                final VisitableSampledSpectrum fins2n) {
            this.signal[slitIndex]       = signal;
            this.background[slitIndex]   = background;
            this.exps2n[slitIndex]       = exps2n;
            this.fins2n[slitIndex]       = fins2n;
        }

        public int getNumberOfSlits() { return numberOfSlits; }

        public VisitableSampledSpectrum getSignalSpectrum(int slit) {
            return signal[slit];
        }

        public VisitableSampledSpectrum getBackgroundSpectrum(int slit) {
            return background[slit];
        }

        public VisitableSampledSpectrum getExpS2NSpectrum(int slit) {
            return exps2n[slit];
        }

        public VisitableSampledSpectrum getFinalS2NSpectrum(int slit) {
            return fins2n[slit];
        }

        public double getPeakPixelCount(int slit) {
            final double[] sig = getSignalSpectrum(slit).getValues();
            final double[] bck = getBackgroundSpectrum(slit).getValues();

            if (getSignalSpectrum(slit).getStart() != getBackgroundSpectrum(slit).getStart()) throw new Error();
            if (getSignalSpectrum(slit).getEnd()   != getBackgroundSpectrum(slit).getEnd())   throw new Error();
            if (sig.length != bck.length)                                             throw new Error();

            // Calculate the peak pixel
            double peak = IntStream.range(0, sig.length).mapToDouble(i -> bck[i]*bck[i] + sig[i]).max().getAsDouble();
            Log.fine("Peak = " + peak);

            return peak;
        }

        // Max peak pixel count across all the CCD and spectra
        @Override
        public double getPeakPixelCount() {
            double maxPeak = 0;

            for (int i = 0 ; i < numberOfSlits; i++) {
                if (getPeakPixelCount(i) > maxPeak)
                    maxPeak = getPeakPixelCount(i);
            }

            return maxPeak;
        }

        /*
         * We need to implement these methods to comply with the SpecS2N
         * interface, but they make no sense, and will not be used, but just
         * in case, we'll throw an exception if someone tries to used them!
         */
        @Override public VisitableSampledSpectrum getSignalSpectrum() {
            throw new UnsupportedOperationException();
        }

        @Override public VisitableSampledSpectrum getBackgroundSpectrum() {
            throw new UnsupportedOperationException();
        }

        @Override public VisitableSampledSpectrum getExpS2NSpectrum() {
            throw new UnsupportedOperationException();
        }

        @Override public VisitableSampledSpectrum getFinalS2NSpectrum() {
            throw new UnsupportedOperationException();
        }

    }


    // == Ghost CHARTS



    /** Creates the signal to noise in wavelength space chart. */
    /*
    private static SpcChartData createS2NChart(final SpectroscopyResult[] results, final int i) {
        final Ghost mainInstrument  = (Ghost) results[0].instrument(); // This must be Ghost here.
        final Ghost[] ccdArray      = mainInstrument.getDetectorCcdInstruments();
        final DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();
        final List<ChartAxis> axes = new ArrayList<>();

        String title = "Intermediate Single Exp and Final S/N in aperture ";

        final ChartAxis xAxis = ChartAxis.apply("Wavelength (nm)");
        final ChartAxis yAxis = ChartAxis.apply("Signal / Noise per spectral pixel");

        final List<SpcSeriesData> data = new ArrayList<>();

        for (final Ghost instrument : ccdArray) {
            final String ccdName = results.length > 1 ? instrument.getDetectorCcdName() : "";
            final int ccdIndex   = instrument.getDetectorCcdIndex();
            final int first      = tv.getDetectorCcdStartIndex(ccdIndex);
            final int last       = tv.getDetectorCcdEndIndex(ccdIndex, ccdArray.length);
            final SpectroscopyResult result = results[ccdIndex];
            data.addAll(s2nChartSeries(mainInstrument, ccdIndex, (GhostSpecS2N)result.specS2N()[i], first, last, tv, ccdName));
        }

        final scala.collection.immutable.List<SpcSeriesData> scalaData = JavaConversions.asScalaBuffer(data).toList();
        final scala.collection.immutable.List<ChartAxis>     scalaAxes = JavaConversions.asScalaBuffer(axes).toList();
        return new SpcChartData(S2NChart.instance(), title, xAxis, yAxis, scalaData, scalaAxes);
    }
    */
    /** Creates the IFU signal in pixel-space chart. */
    /*
    private static SpcChartData createSignalPixelChart(final SpectroscopyResult[] results, final int i) {
        final Ghost mainInstrument = (Ghost) results[0].instrument(); // This must be Ghost here.
        final Ghost[] ccdArray     = mainInstrument.getDetectorCcdInstruments();
        final DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();

        final double ifuOffset = mainInstrument.getIfuMethod().get() instanceof IfuSum ? 0.0 : mainInstrument.getIFU().getApertureOffsetList().get(i);

        final List<ChartAxis> axes = new ArrayList<>();
        final String title = "Pixel Signal and SQRT(Background)\nIFU element offset: " + String.format("%.2f", ifuOffset) + " arcsec";
        final ChartAxis xAxis = new ChartAxis("Pixels", true, new Some<>(new ChartAxisRange(0, tv.fullArrayPix())));
        final ChartAxis yAxis = ChartAxis.apply("e- per exposure per spectral pixel");

        axes.add(new ChartAxis("Wavelength (nm) (Red slit)",  false, new Some<>(new ChartAxisRange(tv.ifu2RedStart(),  tv.ifu2RedEnd()))));
        axes.add(new ChartAxis("Wavelength (nm) (Blue slit)", false, new Some<>(new ChartAxisRange(tv.ifu2BlueStart(), tv.ifu2BlueEnd()))));

        final List<SpcSeriesData> data = new ArrayList<>();

        for (final Ghost instrument : ccdArray) {
            final String ccdName = results.length > 1 ? " " + instrument.getDetectorCcdName() : "";
            final int ccdIndex   = instrument.getDetectorCcdIndex();
            final int first      = tv.getDetectorCcdStartIndex(ccdIndex);
            final int last       = tv.getDetectorCcdEndIndex(ccdIndex, ccdArray.length);
            final SpectroscopyResult result = results[ccdIndex];
            data.addAll(signalPixelChartSeries((GhostSpecS2N)result.specS2N()[i], first, last, tv, ccdName));
        }

        final scala.collection.immutable.List<SpcSeriesData> scalaData = JavaConversions.asScalaBuffer(data).toList();
        final scala.collection.immutable.List<ChartAxis>     scalaAxes = JavaConversions.asScalaBuffer(axes).toList();
        return new SpcChartData(SignalPixelChart.instance(), title, xAxis, yAxis, scalaData, scalaAxes);
    }
 */
    /** Creates all data series for the signal in wavelength space chart. */
    /*
    private static List<SpcSeriesData> sigChartSeries(final Ghost mainInstrument, final int ccdIndex, final GhostSpecS2N result, final int start, final int end, final DetectorsTransmissionVisitor tv, final String ccdName) {
        String sigTitle;
        String bkgTitle;
        Color colorSig;
        Color colorBkg;
        // The suffix is a hack to overcome the requirement for titles of series to be unique when depricating
        // extra legend items with IFU-2 with Hamamatsu
        String suffix = ccdName;

        System.out.println("TODO, Not implemented yet");
        final List<SpcSeriesData> series = new ArrayList<>();

        return series;
    }

     */
    /** Creates all data series for the signal to noise in wavelength space chart. */
    private static List<SpcSeriesData> s2nChartSeries(final Ghost mainInstrument, final int ccdIndex, final GhostSpecS2N result, final int start, final int end, final DetectorsTransmissionVisitor tv, final String ccdName) {
        String s2nTitle;
        String finTitle;
        Color colorS2N;
        Color colorFin;
        // The suffix is a hack to overcome the requirement for titles of series to be unique when depricating
        // extra legend items with IFU-2 with Hamamatsu
        String suffix = ccdName;

        final List<SpcSeriesData> series = new ArrayList<>();
        // for IFU-2 with Hamamatsu we don't show CCDs in different colors, hence need to disable extra legend items
        final boolean disableLegend = ccdIndex != 0;

        for (int i = 0; i < result.numberOfSlits; i++) {


            if (result.numberOfSlits == 1) {
                s2nTitle = "Single Exp S/N " + suffix;
                finTitle = "Final S/N " + suffix;
            } else {
                if (i == 0) {
                    s2nTitle = "Blue Slit Single Exp S/N " + suffix;
                    finTitle = "Blue Slit Final S/N " + suffix;
                } else {
                    s2nTitle = "Red Slit Single Exp S/N " + suffix;
                    finTitle = "Red Slit Final S/N " + suffix;
                }
            }

            result.getExpS2NSpectrum(i).accept(tv);
            result.getFinalS2NSpectrum(i).accept(tv);


            final double[][] s2n = result.getExpS2NSpectrum(i).getData(start, end);
            final double[][] fin = result.getFinalS2NSpectrum(i).getData(start, end);

            // ===== fix gap borders to avoid signal/s2n spikes
            if (mainInstrument.getDetectorCcdInstruments().length > 1) {
                fixGapBorders(s2n);
                fixGapBorders(fin);
            }
            // =====

            if (result.getNumberOfSlits() == 1) {
                colorS2N = ccdLightColor(ccdIndex);
                colorFin = ccdDarkColor(ccdIndex);
            } else if (i == 0) {
                colorS2N = ITCChart.LightBlue;
                colorFin = ITCChart.DarkBlue;
            } else {
                colorS2N = ITCChart.LightRed;
                colorFin = ITCChart.DarkRed;
            }

            series.add(SpcSeriesData.withVisibility(!disableLegend, SingleS2NData.instance(), s2nTitle, s2n, new Some<>(colorS2N)));
            series.add(SpcSeriesData.withVisibility(!disableLegend, FinalS2NData.instance(), finTitle, fin, new Some<>(colorFin)));
        }
        return series;
    }

    /** Creates all data series for the signal in pixel space chart. */
    private static List<SpcSeriesData> signalPixelChartSeries(final GhostSpecS2N result, final int start, final int end, final DetectorsTransmissionVisitor tv, final String ccdName) {
        // This type of chart is currently only used for IFU-2. It transforms the signal from
        // wavelength space to pixel space and displays it as a chart including gaps between CCDs.

        // For IFU-2 with Hamamatsu we don't show CCDs in different colors, hence need to disable extra legend items
        final boolean visibleLegend = (ccdName.equals("") || ccdName.equals(" BB(B)") || ccdName.equals(" BB"));

        // The suffix is a hack to overcome the requirement for titles of series to be unique when depricating
        // extra legend items with IFU-2 with Hamamatsu
        String suffix = ccdName;
        if (visibleLegend) { suffix = ""; }

        // those values are still original, no gaps added, do transformation to pixel space first
        final VisitableSampledSpectrum red = ((VisitableSampledSpectrum) result.getSignalSpectrum(1).clone());
        final VisitableSampledSpectrum blue = ((VisitableSampledSpectrum) result.getSignalSpectrum(0).clone());

        final VisitableSampledSpectrum redBkg = ((VisitableSampledSpectrum) result.getBackgroundSpectrum(1).clone());
        final VisitableSampledSpectrum blueBkg = ((VisitableSampledSpectrum) result.getBackgroundSpectrum(0).clone());

        // to pixel transform also adds gaps (i.e. sets values to zero..)
        final double shift = tv.ifu2shift();
        final double pixelPlotRed[][]     = tv.toPixelSpace(red.getData(start, end), shift);
        final double pixelPlotBlue[][]    = tv.toPixelSpace(blue.getData(start, end), -shift);
        final double pixelPlotRedBkg[][]  = tv.toPixelSpace(redBkg.getData(start, end), shift);
        final double pixelPlotBlueBkg[][] = tv.toPixelSpace(blueBkg.getData(start, end), -shift);

        fixGapBorders(pixelPlotRed);
        fixGapBorders(pixelPlotBlue);
        fixGapBorders(pixelPlotRedBkg);
        fixGapBorders(pixelPlotBlueBkg);

        final List<SpcSeriesData> series = new ArrayList<>();
        series.add(SpcSeriesData.withVisibility(visibleLegend, SignalData.instance(),     "Blue Slit Signal" + suffix,              pixelPlotBlue,    new Some<>(ITCChart.DarkBlue)));
        series.add(SpcSeriesData.withVisibility(visibleLegend, BackgroundData.instance(), "SQRT(Blue Slit Background)" + suffix,    pixelPlotBlueBkg, new Some<>(ITCChart.LightBlue)));
        series.add(SpcSeriesData.withVisibility(visibleLegend, SignalData.instance(),     "Red Slit Signal" + suffix,               pixelPlotRed,     new Some<>(ITCChart.DarkRed)));
        series.add(SpcSeriesData.withVisibility(visibleLegend, BackgroundData.instance(), "SQRT(Red Slit Background)" + suffix,     pixelPlotRedBkg,  new Some<>(ITCChart.LightRed)));

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
