package edu.gemini.itc.gnirs;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.GaussianSource;
import edu.gemini.spModel.core.PointSource$;
import edu.gemini.spModel.core.UniformSource$;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import scala.Option;
import scala.Some;
import scala.collection.JavaConversions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class performs the calculations for GNIRS
 */
public final class GnirsRecipe implements ImagingRecipe, SpectroscopyRecipe {

    private static final Logger Log = Logger.getLogger(GnirsRecipe.class.getName());
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

        validateInputParameters();
    }

    private void validateInputParameters() {
        // some general validations
        Validation.validate(instrument, _obsDetailParameters, _sdParameters);
    }

    public ItcImagingResult serviceResult(final ImagingResult r) {
        return Recipe$.MODULE$.serviceResult(r);
    }

    public ItcSpectroscopyResult serviceResult(final SpectroscopyResult r, final boolean headless) {

        final List<List<SpcChartData>> groups = new ArrayList<>();

        if (instrument.XDisp_IsUsed()) {
            Log.fine("Generating XD charts...");
            final List<SpcChartData> charts = new ArrayList<>();
            charts.add(createGnirsSignalChart(r));
            charts.add(createGnirsS2NChart(r));
            groups.add(charts);

        } else if (instrument.isIfuUsed()) {
            // Create a chart for each IFU element stored in the array specS2N:
            Log.fine("Generating " + r.specS2N().length + " IFU charts...");
            for (int i = 0; i < r.specS2N().length; i++) {
                final List<SpcChartData> charts = new ArrayList<>();
                charts.add(createGnirsIfuSignalChart(r, i));
                charts.add(createGnirsIfuS2NChart(r, i));
                groups.add(charts);
            }

        } else {  // use the generic chart creator in Recipe.scala
            final List<SpcChartData> charts = new ArrayList<>();
            charts.add(Recipe$.MODULE$.createSignalChart(r,0));
            charts.add(Recipe$.MODULE$.createS2NChart(r));
            groups.add(charts);
        }

        return Recipe$.MODULE$.serviceGroupedResult(r, groups, headless);
    }

    public SpectroscopyResult calculateSpectroscopy() {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifted SED

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        // Altair specific section
        final Option<AOSystem> altair;
        if (_gnirsParameters.altair().isDefined()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(), IQcalc.getImageQuality(), _obsConditionParameters.ccExtinction(), _gnirsParameters.altair().get(), 0.1);
            altair = Option.apply(ao);
        } else {
            altair = Option.empty();
        }

        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope, altair);
        final VisitableSampledSpectrum sed = calcSource.sed;
        final VisitableSampledSpectrum sky = calcSource.sky;
        final Option<VisitableSampledSpectrum> halo = calcSource.halo;

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        final double im_qual = altair.isDefined() ? altair.get().getAOCorrectedFWHM() : IQcalc.getImageQuality();

        // TODO: why, oh why?
        final double im_qual1 = _sdParameters.isUniform() ? 10000 : im_qual;

        if (instrument.isIfuUsed()) {  // === IFU ===
            // Module 1a
            // The purpose of this section is to calculate the fraction of the
            // source flux which is contained within an aperture which we adopt
            // to derive the signal to noise ratio.  There are several cases
            // depending on the source morphology.
            // Define the source morphology
            //
            // inputs: source morphology specification

            sed.accept(instrument.getGratingOrderNTransmission(instrument.getOrder()));

            // Morphology section
            final VisitableMorphology morph, haloMorphology;
            if (_sdParameters.profile() == PointSource$.MODULE$) {
                morph = new AOMorphology(im_qual);
                haloMorphology = new AOMorphology(IQcalc.getImageQuality());
            } else if (_sdParameters.profile() instanceof GaussianSource) {
                Log.fine("Gaussian & Halo FWHM = " + IQcalc.getImageQuality() + " arcsec");
                morph = new GaussianMorphology(IQcalc.getImageQuality());
                haloMorphology = new GaussianMorphology(IQcalc.getImageQuality());
            } else if (_sdParameters.profile() == UniformSource$.MODULE$) {
                morph = new USBMorphology();
                haloMorphology = new USBMorphology();
            } else {
                throw new IllegalArgumentException();
            }
            morph.accept(instrument.getIFU().getAperture());

            //for now just a single item from the list
            final List<Double> sf_list = instrument.getIFU().getFractionOfSourceInAperture();  //extract corrected source fraction list
            Log.fine("Fraction of source in " + sf_list.size() + " IFU elements = " + sf_list);

            instrument.getIFU().clearFractionOfSourceInAperture();
            haloMorphology.accept(instrument.getIFU().getAperture());

            final List<Double> halo_sf_list = instrument.getIFU().getFractionOfSourceInAperture();  //extract uncorrected halo source fraction list
            Log.fine("halo_sf_list = " + halo_sf_list);

            final List<Double> ap_offset_list = instrument.getIFU().getApertureOffsetList();
            Log.fine("ap_offset_list = " + ap_offset_list);

            // In this version we are bypassing morphology modules 3a-5a.
            // i.e. the output morphology is same as the input morphology.
            // Might implement these modules at a later time.
            double throughput = 0.0;
            double haloThroughput = 0.0;
            double onePixThroughput = 0.0;

            final Iterator<Double> src_frac_it = sf_list.iterator();
            final Iterator<Double> halo_src_frac_it = halo_sf_list.iterator();

            int i = 0;
            double t;
            final SpecS2N[] specS2Narr = new SpecS2N[_obsDetailParameters.analysisMethod() instanceof IfuSummed ? 1 : sf_list.size()];

            while (src_frac_it.hasNext()) {
                Log.fine(String.format("Processing IFU element %d of %d -----", i, ap_offset_list.size() - 1));
                double slitLength = 1.0;  // pixel

                if (_obsDetailParameters.analysisMethod() instanceof IfuSummed) {
                    while (src_frac_it.hasNext()) {
                        t = src_frac_it.next();
                        throughput += t;
                        onePixThroughput = Math.max(onePixThroughput, t);  // plot the pixel with the largest throughput
                        haloThroughput += halo_src_frac_it.next();
                    }
                    slitLength = ap_offset_list.size() / 2.0;
                } else {
                    throughput = src_frac_it.next();
                    onePixThroughput = throughput;
                    haloThroughput = halo_src_frac_it.next();
                }
                Log.fine("Fraction of source in IFU:  1 pix = " + onePixThroughput + ", " + slitLength + " pix = " + throughput);

                // The IFUs have anamorphic magnification which makes the output slit width 2x the input slice width:
                Log.fine("Input slice width = " + instrument.getSlitWidth() + " arcsec, " +
                        "output slit width = " + 2.0 * instrument.getSlitWidth() + " arcsec");
                final Slit input_slit = Slit$.MODULE$.apply(instrument.getSlitWidth(), slitLength, instrument.getPixelSize());
                final Slit output_slit = Slit$.MODULE$.apply(2.0 * instrument.getSlitWidth(), slitLength, instrument.getPixelSize());

                final SpecS2NSlitVisitor specS2N = new SpecS2NSlitVisitor(
                        input_slit,
                        output_slit,
                        instrument.disperser(instrument.getOrder()),
                        new SlitThroughput(throughput, onePixThroughput),
                        instrument.getSpectralPixelWidth() / instrument.getOrder(),
                        instrument.getObservingStart(),
                        instrument.getObservingEnd(),
                        im_qual1,
                        instrument.getReadNoise(),
                        instrument.getDarkCurrent(),
                        _obsDetailParameters,
                        false);

                specS2N.setSourceSpectrum(sed);
                specS2N.setBackgroundSpectrum(sky);
                if (altair.isDefined()) {
                    specS2N.setHaloSpectrum(halo.get(), new SlitThroughput(haloThroughput, haloThroughput), IQcalc.getImageQuality());
                }
                sed.accept(specS2N);
                specS2Narr[i++] = specS2N;
            }

            return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, null, 0, altair, Option.empty());

        } else {  // === SLIT ===

            final Slit slit = Slit$.MODULE$.apply(_sdParameters, _obsDetailParameters, instrument, instrument.getSlitWidth(), im_qual);
            final SlitThroughput throughput = new SlitThroughput(_sdParameters, slit, im_qual);
            final Option<SlitThroughput> haloThroughput = altair.isDefined()
                    ? Option.<SlitThroughput>apply(new SlitThroughput(_sdParameters, slit, IQcalc.getImageQuality()))
                    : Option.<SlitThroughput>empty();

            final SpecS2NSlitVisitor specS2N = new SpecS2NSlitVisitor(
                    slit,
                    instrument.disperser(instrument.getOrder()),
                    throughput,
                    instrument.getSpectralPixelWidth() / instrument.getOrder(),
                    instrument.getObservingStart(),
                    instrument.getObservingEnd(),
                    im_qual1,
                    instrument.getReadNoise(),
                    instrument.getDarkCurrent(),
                    _obsDetailParameters);

            if (instrument.XDisp_IsUsed()) {
                final VisitableSampledSpectrum[] sedOrder = new VisitableSampledSpectrum[ORDERS];
                final VisitableSampledSpectrum[] haloOrder = new VisitableSampledSpectrum[ORDERS];
                final VisitableSampledSpectrum[] skyOrder = new VisitableSampledSpectrum[ORDERS];

                /**
                 * The orders here are calculated now the same way it's done for the OT in InstGNIRS.java.
                 * I couldn't reuse the calculation methods from there, since they are coupled to the OT interface.
                 * Still order calculations probably should be generalized.
                 */
                final double centralWavelength = _gnirsParameters.centralWavelength().toMicrons();

                final double[] centralWavelengthArray = new double[GNIRSParams.Order.NUM_ORDERS];
                {
                    GNIRSParams.Order o = GNIRSParams.Order.getOrder(centralWavelength, null);
                    if (o == null)
                        throw new IllegalArgumentException("The order for this wavelength cannot be found");

                    double d = o.getOrder() * centralWavelength;
                    for (int i = 1; i <= GNIRSParams.Order.NUM_ORDERS; i++) {
                        centralWavelengthArray[i - 1] = d / i;
                    }
                }

                final GNIRSParams.PixelScale pixelScale = instrument.getPixelScale();
                final GNIRSParams.Disperser disperser = instrument.getGrating();

                final int n = GNIRSParams.Order.values().length;

                for (int j = 0; j < n; j++) {
                    GNIRSParams.Order o = GNIRSParams.Order.getOrderByIndex(j);

                    if (o == GNIRSParams.Order.ONE || o == GNIRSParams.Order.TWO || o == GNIRSParams.Order.XD) {
                        continue; // skip orders 1, 2, and XD
                    }
                    final int order = o.getOrder(); // order number
                    final int i = order - 3;

                    final double wavelength = centralWavelengthArray[order - 1];
                    final double trimStart = o.getStartWavelength(wavelength, disperser, pixelScale) * 1000; // in nm
                    final double trimEnd = o.getEndWavelength(wavelength, disperser, pixelScale) * 1000;

                    sedOrder[i] = (VisitableSampledSpectrum) sed.clone();
                    sedOrder[i].accept(instrument.getGratingOrderNTransmission(order));
                    sedOrder[i].trim(trimStart, trimEnd);

                    if (halo.nonEmpty()) {
                        haloOrder[i] = (VisitableSampledSpectrum) halo.get().clone();
                        haloOrder[i].accept(instrument.getGratingOrderNTransmission(order));
                        haloOrder[i].trim(trimStart, trimEnd);
                        specS2N.setHaloSpectrum(haloOrder[i], haloThroughput.get(), IQcalc.getImageQuality());
                    }

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
                    if (haloThroughput.nonEmpty()) {
                        specS2N.setHaloSpectrum(haloOrder[i], haloThroughput.get(), IQcalc.getImageQuality());
                    }

                    specS2N.setDisperser(instrument.disperser(order));
                    specS2N.setSpectralPixelWidth(instrument.getSpectralPixelWidth() / order);

                    specS2N.setStartWavelength(sedOrder[i].getStart());
                    specS2N.setEndWavelength(sedOrder[i].getEnd());

                    sed.accept(specS2N);

                    finalS2NOrder[i] = (VisitableSampledSpectrum) specS2N.getFinalS2NSpectrum().clone();
                }

                final SpecS2N[] specS2Narr = new SpecS2N[ORDERS];
                for (int i = 0; i < ORDERS; i++) {
                    final SpecS2N s2n = new GnirsSpecS2N(signalOrder[i], backGroundOrder[i], null, finalS2NOrder[i]);
                    specS2Narr[i] = s2n;
                }

                return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, throughput.throughput(), altair, Option.empty());

            } else {  // === NOT XD ===

                sed.accept(instrument.getGratingOrderNTransmission(instrument.getOrder()));

                specS2N.setSourceSpectrum(sed);
                specS2N.setBackgroundSpectrum(sky);
                if (altair.isDefined() && halo.isDefined() && haloThroughput.isDefined()) {
                    specS2N.setHaloSpectrum(halo.get(), haloThroughput.get(), IQcalc.getImageQuality());
                }
                sed.accept(specS2N);

                final SpecS2N[] specS2Narr = new SpecS2N[]{specS2N};
                return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, throughput.throughput(), altair, Option.empty());
            }

        }

    }

    // === CHARTS ===

    private static SpcChartData createGnirsSignalChart(final SpectroscopyResult result) {
        final String title = "Signal and SQRT(Background) in one pixel";
        final String xAxis = "Wavelength (nm)";
        final String yAxis = "e- per coadd per spectral pixel";
        final List<SpcSeriesData> data = new ArrayList<>();
        for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
            data.add(new SpcSeriesData(SignalData.instance(),     "Signal Order "           + (i + 3), result.specS2N()[i].getSignalSpectrum().getData(),     new Some<>(ITCChart.colorByIndex(2*i + 1))));
            data.add(new SpcSeriesData(BackgroundData.instance(), "SQRT(Background) Order " + (i + 3), result.specS2N()[i].getBackgroundSpectrum().getData(), new Some<>(ITCChart.colorByIndex(2*i))));
        }
        return SpcChartData.apply(SignalChart.instance(), title, xAxis, yAxis, JavaConversions.asScalaBuffer(data).toList());
    }

    private static SpcChartData createGnirsS2NChart(final SpectroscopyResult result) {
        final String title = "Final S/N";
        final String xAxis = "Wavelength (nm)";
        final String yAxis = "Signal / Noise per spectral pixel";
        final List<SpcSeriesData> data = new ArrayList<>();
        for (int i = 0; i < GnirsRecipe.ORDERS; i++) {
           data.add(new SpcSeriesData(FinalS2NData.instance(),
                   "Final S/N Order "        + (i + 3), result.specS2N()[i].getFinalS2NSpectrum().getData(),
                   new Some<>(ITCChart.colorByIndex(2*i + 1))));
        }
        return SpcChartData.apply(S2NChart.instance(), title, xAxis, yAxis, JavaConversions.asScalaBuffer(data).toList());
    }

    private static SpcChartData createGnirsIfuSignalChart(final SpectroscopyResult result, final int index) {
        final Gnirs instrument = (Gnirs) result.instrument();
        final double offset = instrument.getIFU().getApertureOffsetList().get(index);
        String title = "Signal and SQRT(Background) in one pixel";
        if ((instrument.getIFUMethod() instanceof IfuSingle) || (instrument.getIFUMethod() instanceof IfuRadial)) {
            if (offset != 0.0) { title += String.format("\nIFU element offset: %.3f arcseconds", offset); }
        }
        return Recipe$.MODULE$.createSignalChart(result, title, index);
    }

    private static SpcChartData createGnirsIfuS2NChart(final SpectroscopyResult result, final int index) {
        final Gnirs instrument = (Gnirs) result.instrument();
        final double offset = instrument.getIFU().getApertureOffsetList().get(index);
        String title = "Intermediate Single Exp and Final S/N\n";
        if (instrument.getIFUMethod() instanceof IfuSummed) {
            title += "IFU summed apertures: " + instrument.getIFUNumX() + "x" + instrument.getIFUNumY() + "  (" +
                    String.format("%.3f", instrument.getIFUNumX() * IFUComponent.ifuElementSize) + "\" x " +
                    String.format("%.3f", instrument.getIFUNumY() * IFUComponent.ifuElementSize) + "\")";
        } else if (offset != 0.0) {
            title += String.format("\nIFU element offset: %.3f arcseconds", offset);
        }
        return Recipe$.MODULE$.createS2NChart(result, title, index);
    }


    // SpecS2N implementation to hold results for GNIRS cross dispersion mode calculations.
    class GnirsSpecS2N implements SpecS2N {

        private final VisitableSampledSpectrum signal;
        private final VisitableSampledSpectrum background;
        private final VisitableSampledSpectrum exps2n;
        private final VisitableSampledSpectrum fins2n;

        public GnirsSpecS2N(
                final VisitableSampledSpectrum signal,
                final VisitableSampledSpectrum background,
                final VisitableSampledSpectrum exps2n,
                final VisitableSampledSpectrum fins2n) {
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

    }


    public ImagingResult calculateImaging() {
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
        if (_gnirsParameters.altair().isDefined()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(), IQcalc.getImageQuality(), _obsConditionParameters.ccExtinction(), _gnirsParameters.altair().get(), 0.1); // Since GNIRS does not have perfect optics, the PSF delivered by Altair is convolved with a ~0.10" Gaussian to reproduce the ~0.12" images which are measured under optimal conditions.
            altair = Option.apply(ao);
        } else {
            altair = Option.empty();
        }

        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope, altair);

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

        return new ImagingResult(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc, altair);

    }

}
