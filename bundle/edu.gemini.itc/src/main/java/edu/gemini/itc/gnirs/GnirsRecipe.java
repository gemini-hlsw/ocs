package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import scala.Option;
import scala.Some;
import scala.collection.JavaConversions;

import java.util.ArrayList;
import java.util.List;

/**
 * This class performs the calculations for Gnirs used for imaging.
 */
public final class GnirsRecipe implements SpectroscopyRecipe {

    public static final int ORDERS = 6;

    private final ItcParameters p;
    private final Gnirs instrument;
    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final GnirsParameters _gnirsParameters;
    private final TelescopeDetails _telescope;

    private final VisitableSampledSpectrum[] signalOrder;
    private final VisitableSampledSpectrum[] backGroundOrder;
    private final VisitableSampledSpectrum[] finalS2NOrder;

    /**
     * Constructs a GnirsRecipe given the parameters. Useful for testing.
     */
    public GnirsRecipe(final ItcParameters p, final GnirsParameters instr)

    {
        this.p                  = p;
        instrument              = new Gnirs(instr, p.observation());
        _sdParameters           = p.source();
        _obsDetailParameters    = p.observation();
        _obsConditionParameters = p.conditions();
        _gnirsParameters        = instr;
        _telescope              = p.telescope();

        signalOrder = new VisitableSampledSpectrum[ORDERS];
        backGroundOrder = new VisitableSampledSpectrum[ORDERS];
        finalS2NOrder = new VisitableSampledSpectrum[ORDERS];

        // some general validations
        Validation.validate(instrument, _obsDetailParameters, _sdParameters);
    }

    public ItcSpectroscopyResult serviceResult(final SpectroscopyResult r) {
        final List<SpcChartData> dataSets = new ArrayList<SpcChartData>() {{
            if (instrument.XDisp_IsUsed()) {
                add(createGnirsSignalChart(r));
                add(createGnirsS2NChart(r));
            } else {
                add(Recipe$.MODULE$.createSignalChart(r, 0));
                add(Recipe$.MODULE$.createS2NChart(r));
            }
        }};
        return Recipe$.MODULE$.serviceResult(r, dataSets);
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


        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope);
        final VisitableSampledSpectrum sed = calcSource.sed;
        final VisitableSampledSpectrum sky = calcSource.sky;

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        final Slit slit = Slit$.MODULE$.apply(_sdParameters, _obsDetailParameters, instrument, instrument.getSlitWidth(), IQcalc.getImageQuality());
        final SlitThroughput st = new SlitThroughput(_sdParameters, slit, IQcalc.getImageQuality());

        // TODO: why, oh why?
        final double im_qual = _sdParameters.isUniform() ? 10000 : IQcalc.getImageQuality();

        final SpecS2NSlitVisitor specS2N = new SpecS2NSlitVisitor(
                slit,
                instrument.disperser(instrument.getOrder()),
                st.throughput(),
                instrument.getSpectralPixelWidth() / instrument.getOrder(),
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                im_qual,
                instrument.getReadNoise(),
                instrument.getDarkCurrent(),
                _obsDetailParameters);

        if (instrument.XDisp_IsUsed()) {

            final double trimCenter;
            if (instrument.getGrating().equals(GNIRSParams.Disperser.D_111)) {
                trimCenter = _gnirsParameters.centralWavelength().toNanometers();
            } else {
                trimCenter = 2200.0;
            }

            final VisitableSampledSpectrum[] sedOrder = new VisitableSampledSpectrum[ORDERS];
            final VisitableSampledSpectrum[] skyOrder = new VisitableSampledSpectrum[ORDERS];
            for (int i = 0; i < ORDERS; i++) {
                final int order = i + 3;
                final double d         = instrument.getGratingDispersion() / order * Gnirs.DETECTOR_PIXELS / 2;
                final double trimStart = trimCenter * 3 / order - d;
                final double trimEnd   = trimCenter * 3 / order + d;

                sedOrder[i] = (VisitableSampledSpectrum) sed.clone();
                sedOrder[i].accept(instrument.getGratingOrderNTransmission(order));
                sedOrder[i].trim(trimStart, trimEnd);

                skyOrder[i] = (VisitableSampledSpectrum) sky.clone();
                skyOrder[i].accept(instrument.getGratingOrderNTransmission(order));
                skyOrder[i].trim(trimStart, trimEnd);

                specS2N.setSourceSpectrum(sedOrder[i]);
                specS2N.setBackgroundSpectrum(skyOrder[i]);

                specS2N.setDisperser(instrument.disperser(order));
                specS2N.setSpectralPixelWidth(instrument.getSpectralPixelWidth() / order);

                specS2N.setStartWavelength(sedOrder[i].getStart());
                specS2N.setEndWavelength(sedOrder[i].getEnd());

                sed.accept(specS2N);

                signalOrder[i] = (VisitableSampledSpectrum) specS2N.getSignalSpectrum().clone();
                backGroundOrder[i] = (VisitableSampledSpectrum) specS2N.getBackgroundSpectrum().clone();
            }

            for (int i = 0; i < ORDERS; i++) {
                final int order = i + 3;
                specS2N.setSourceSpectrum(sedOrder[i]);
                specS2N.setBackgroundSpectrum(skyOrder[i]);

                specS2N.setDisperser(instrument.disperser(order));
                specS2N.setSpectralPixelWidth(instrument.getSpectralPixelWidth() / order);

                specS2N.setStartWavelength(sedOrder[i].getStart());
                specS2N.setEndWavelength(sedOrder[i].getEnd());

                sed.accept(specS2N);

                finalS2NOrder[i] = (VisitableSampledSpectrum) specS2N.getFinalS2NSpectrum().clone();
            }

            final SpecS2N[] specS2Narr = new SpecS2N[ORDERS];
            for (int i = 0; i < ORDERS; i++) {
                final SpecS2N s2n = new GnirsSpecS2N(im_qual, signalOrder[i], backGroundOrder[i], null, finalS2NOrder[i]);
                specS2Narr[i] = s2n;
            }

            return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, st.throughput(), Option.<AOSystem>empty());

        } else {

            sed.accept(instrument.getGratingOrderNTransmission(instrument.getOrder()));

            specS2N.setSourceSpectrum(sed);
            specS2N.setBackgroundSpectrum(sky);
            sed.accept(specS2N);

            final SpecS2N[] specS2Narr = new SpecS2N[] {specS2N};
            return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, st.throughput(), Option.<AOSystem>empty());
        }


    }

    // == GNIRS CHARTS

    private static SpcChartData createGnirsSignalChart(final SpectroscopyResult result) {
        final String title = "Signal and SQRT(Background) in one pixel";
        final String xAxis = "Wavelength (nm)";
        final String yAxis = "e- per exposure per spectral pixel";
        final List<SpcSeriesData> data = new ArrayList<>();
        for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
            data.add(new SpcSeriesData(SignalData.instance(),     "Signal Order "           + (i + 3), result.specS2N()[i].getSignalSpectrum().getData(),     new Some<>(ITCChart.colorByIndex(2*i    ))));
            data.add(new SpcSeriesData(BackgroundData.instance(), "SQRT(Background) Order " + (i + 3), result.specS2N()[i].getBackgroundSpectrum().getData(), new Some<>(ITCChart.colorByIndex(2*i + 1))));
        }
        return SpcChartData.apply(SignalChart.instance(), title, xAxis, yAxis, JavaConversions.asScalaBuffer(data).toList());
    }

    private static SpcChartData createGnirsS2NChart(final SpectroscopyResult result) {
        final String title = "Final S/N";
        final String xAxis = "Wavelength (nm)";
        final String yAxis = "Signal / Noise per spectral pixel";
        final List<SpcSeriesData> data = new ArrayList<>();
        for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
           data.add(new SpcSeriesData(FinalS2NData.instance(),   "Final S/N Order "        + (i + 3), result.specS2N()[i].getFinalS2NSpectrum().getData(),     new Some<>(ITCChart.colorByIndex(2*i))));
        }
        return SpcChartData.apply(S2NChart.instance(), title, xAxis, yAxis, JavaConversions.asScalaBuffer(data).toList());
    }

    // SpecS2N implementation to hold results for GNIRS cross dispersion mode calculations.
    class GnirsSpecS2N implements SpecS2N {

        private final double imgQuality;
        private final VisitableSampledSpectrum signal;
        private final VisitableSampledSpectrum background;
        private final VisitableSampledSpectrum exps2n;
        private final VisitableSampledSpectrum fins2n;

        public GnirsSpecS2N(
                final double imgQuality,
                final VisitableSampledSpectrum signal,
                final VisitableSampledSpectrum background,
                final VisitableSampledSpectrum exps2n,
                final VisitableSampledSpectrum fins2n) {
            this.imgQuality   = imgQuality;
            this.signal       = signal;
            this.background   = background;
            this.exps2n       = exps2n;
            this.fins2n       = fins2n;
        }

        @Override public VisitableSampledSpectrum getSignalSpectrum() {
            return signal;
        }

        @Override public VisitableSampledSpectrum getBackgroundSpectrum() {
            return background;
        }

        @Override public VisitableSampledSpectrum getExpS2NSpectrum() {
            return exps2n;
        }

        @Override public VisitableSampledSpectrum getFinalS2NSpectrum() {
            return fins2n;
        }

        @Override public double getImageQuality() {
            return imgQuality;
        }

    }

}
