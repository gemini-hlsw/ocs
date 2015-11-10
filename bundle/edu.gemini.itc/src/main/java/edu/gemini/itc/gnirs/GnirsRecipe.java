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
                add(Recipe$.MODULE$.createSigSwAppChart(r, 0));
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

        final double pixel_size = instrument.getPixelSize();
        double ap_diam = 0;

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();


        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope);
        final VisitableSampledSpectrum sed = calcSource.sed;
        final VisitableSampledSpectrum sky = calcSource.sky;

        // Calculate the Fraction of source in the aperture
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, IQcalc.getImageQuality());

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        final SlitThroughput st;
        if (!_obsDetailParameters.isAutoAperture()) {
            st = new SlitThroughput(IQcalc.getImageQuality(),
                    _obsDetailParameters.getApertureDiameter(),
                    pixel_size, instrument.getFPMask());
        } else {
            st = new SlitThroughput(IQcalc.getImageQuality(), pixel_size, instrument.getFPMask());
        }

        ap_diam = st.getSpatialPix(); // ap_diam really Spec_Npix on

        double spec_source_frac = st.getSlitThroughput();

        // For the usb case we want the resolution to be determined by the
        // slit width and not the image quality for a point source.
        final double im_qual;
        if (_sdParameters.isUniform()) {
            im_qual = 10000;
            if (_obsDetailParameters.isAutoAperture()) {
                ap_diam = new Double(1 / (instrument.getFPMask() * pixel_size) + 0.5).intValue();
                spec_source_frac = 1;
            } else {
                spec_source_frac = instrument.getFPMask() * ap_diam * pixel_size;
            }
        } else {
            im_qual = IQcalc.getImageQuality();
        }

        final SpecS2NLargeSlitVisitor specS2N = new SpecS2NLargeSlitVisitor(
                instrument.getFPMask(), pixel_size,
                instrument.getSpectralPixelWidth() / instrument.getOrder(),
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                instrument.getGratingDispersion_nm(),
                instrument.getGratingDispersion_nmppix(),
                spec_source_frac,
                im_qual, ap_diam,
                _obsDetailParameters.getNumExposures(),
                _obsDetailParameters.getSourceFraction(),
                _obsDetailParameters.getExposureTime(),
                instrument.getDarkCurrent(),
                instrument.getReadNoise(),
                _obsDetailParameters.getSkyApertureDiameter());

        if (instrument.XDisp_IsUsed()) {
            final VisitableSampledSpectrum[] sedOrder = new VisitableSampledSpectrum[ORDERS];
            for (int i = 0; i < ORDERS; i++) {
                sedOrder[i] = (VisitableSampledSpectrum) sed.clone();
            }

            final VisitableSampledSpectrum[] skyOrder = new VisitableSampledSpectrum[ORDERS];
            for (int i = 0; i < ORDERS; i++) {
                skyOrder[i] = (VisitableSampledSpectrum) sky.clone();
            }

            final double trimCenter;
            if (instrument.getGrating().equals(GNIRSParams.Disperser.D_111)) {
                trimCenter = _gnirsParameters.centralWavelength().toNanometers();
            } else {
                trimCenter = 2200.0;
            }

            for (int i = 0; i < ORDERS; i++) {
                final int order = i + 3;
                final double d         = instrument.getGratingDispersion_nmppix() / order * Gnirs.DETECTOR_PIXELS / 2;
                final double trimStart = trimCenter * 3 / order - d;
                final double trimEnd   = trimCenter * 3 / order + d;

                sedOrder[i].accept(instrument.getGratingOrderNTransmission(order));
                sedOrder[i].trim(trimStart, trimEnd);

                skyOrder[i].accept(instrument.getGratingOrderNTransmission(order));
                skyOrder[i].trim(trimStart, trimEnd);
            }

            for (int i = 0; i < ORDERS; i++) {
                final int order = i + 3;
                specS2N.setSourceSpectrum(sedOrder[i]);
                specS2N.setBackgroundSpectrum(skyOrder[i]);

                specS2N.setGratingDispersion_nmppix(instrument.getGratingDispersion_nmppix() / order);
                specS2N.setGratingDispersion_nm(instrument.getGratingDispersion_nm() / order);
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

                specS2N.setGratingDispersion_nmppix(instrument.getGratingDispersion_nmppix() / order);
                specS2N.setGratingDispersion_nm(instrument.getGratingDispersion_nm() / order);
                specS2N.setSpectralPixelWidth(instrument.getSpectralPixelWidth() / order);

                specS2N.setStartWavelength(sedOrder[i].getStart());
                specS2N.setEndWavelength(sedOrder[i].getEnd());

                sed.accept(specS2N);

                finalS2NOrder[i] = (VisitableSampledSpectrum) specS2N.getFinalS2NSpectrum().clone();
            }

            final SpecS2N[] specS2Narr = new SpecS2N[ORDERS];
            for (int i = 0; i < ORDERS; i++) {
                final SpecS2N s2n = new GnirsSpecS2N(im_qual, ap_diam, signalOrder[i], backGroundOrder[i], null, finalS2NOrder[i]);
                specS2Narr[i] = s2n;
            }

            return new SpectroscopyResult(p, instrument, SFcalc, IQcalc, specS2Narr, st, Option.<AOSystem>empty());

        } else {

            sed.accept(instrument.getGratingOrderNTransmission(instrument.getOrder()));

            specS2N.setSourceSpectrum(sed);
            specS2N.setBackgroundSpectrum(sky);
            specS2N.setHaloImageQuality(0.0);
            specS2N.setSpecHaloSourceFraction(0.0);

            sed.accept(specS2N);

            final SpecS2N[] specS2Narr = new SpecS2N[] {specS2N};
            return new SpectroscopyResult(p, instrument, SFcalc, IQcalc, specS2Narr, st, Option.<AOSystem>empty());
        }


    }

    // == GNIRS CHARTS

    private static SpcChartData createGnirsSignalChart(final SpectroscopyResult result) {
        final String title = "Signal and Background in software aperture of " + result.specS2N()[0].getSpecNpix() + " pixels";
        final String xAxis = "Wavelength (nm)";
        final String yAxis = "e- per exposure per spectral pixel";
        final List<SpcSeriesData> data = new ArrayList<>();
        for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
            data.add(new SpcSeriesData(SignalData.instance(),     "Signal Order "           + (i + 3), result.specS2N()[i].getSignalSpectrum().getData(),     new Some<>(ITCChart.colorByIndex(2*i    ))));
            data.add(new SpcSeriesData(BackgroundData.instance(), "SQRT(Background) Order " + (i + 3), result.specS2N()[i].getBackgroundSpectrum().getData(), new Some<>(ITCChart.colorByIndex(2*i + 1))));
        }
        return new SpcChartData(SignalChart.instance(), title, xAxis, yAxis, JavaConversions.asScalaBuffer(data).toList());
    }

    private static SpcChartData createGnirsS2NChart(final SpectroscopyResult result) {
        final String title = "Final S/N";
        final String xAxis = "Wavelength (nm)";
        final String yAxis = "Signal / Noise per spectral pixel";
        final List<SpcSeriesData> data = new ArrayList<>();
        for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
           data.add(new SpcSeriesData(FinalS2NData.instance(),   "Final S/N Order "        + (i + 3), result.specS2N()[i].getFinalS2NSpectrum().getData(),     new Some<>(ITCChart.colorByIndex(2*i))));
        }
        return new SpcChartData(S2NChart.instance(), title, xAxis, yAxis, JavaConversions.asScalaBuffer(data).toList());
    }

    // SpecS2N implementation to hold results for GNIRS cross dispersion mode calculations.
    class GnirsSpecS2N implements SpecS2N {

        private final double imgQuality;
        private final double nPix;
        private final VisitableSampledSpectrum signal;
        private final VisitableSampledSpectrum background;
        private final VisitableSampledSpectrum exps2n;
        private final VisitableSampledSpectrum fins2n;

        public GnirsSpecS2N(
                final double imgQuality,
                final double nPix,
                final VisitableSampledSpectrum signal,
                final VisitableSampledSpectrum background,
                final VisitableSampledSpectrum exps2n,
                final VisitableSampledSpectrum fins2n) {
            this.imgQuality   = imgQuality;
            this.nPix         = nPix;
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

        @Override public double getSpecNpix() {
            return nPix;
        }
    }

}
