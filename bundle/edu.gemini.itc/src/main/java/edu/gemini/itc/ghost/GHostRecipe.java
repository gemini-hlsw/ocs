package edu.gemini.itc.ghost;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.ghost.Detector;
import scala.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class performs the calculations for Ghost used for imaging.
 */
public final class GHostRecipe implements SpectroscopyArrayRecipe {

    private static final Logger Log = Logger.getLogger(GHostRecipe.class.getName());
    private final ItcParameters p;
    private final Ghost[] _mainInstrument;
    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final TelescopeDetails _telescope;

    private static final double WAVELENGTH_BLUE_END = 542;  // nm. Blue arm range: ~347 – 542 nm
    private static final double WAVELENGTH_RED_START = 520; // nm. Red arm range:  520 – redEnd nm

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

    public ItcSpectroscopyResult serviceResult(final SpectroscopyResult[] r, final boolean headless) {
        return serviceResult(r, headless, false);
    }

    public ItcSpectroscopyResult serviceResult(final SpectroscopyResult[] r, final boolean headless, final boolean forGPP) {
        final List<List<SpcChartData>> groups = new ArrayList<>();
        List<VisitableSampledSpectrum> listPerResElement = new ArrayList<VisitableSampledSpectrum>();
        for (SpectroscopyResult r1 : r) {
            final List<SpcChartData> dataSets1 = new ArrayList<SpcChartData>();
            dataSets1.add(Recipe$.MODULE$.createSignalChart(r1, 0));
            dataSets1.add(Recipe$.MODULE$.createS2NChart(r1, "Single Exposure and Final S/N in aperture per pixel", 0));
            if (!forGPP) {
                VisitableSampledSpectrum finalS2N = (VisitableSampledSpectrum) r1.specS2N()[0].getFinalS2NSpectrum().clone();
                Ghost inst = (Ghost) r1.instrument();
                inst.transPerResolutionElement(finalS2N);
                VisitableSampledSpectrum singleS2N = (VisitableSampledSpectrum) r1.specS2N()[0].getExpS2NSpectrum().clone();
                inst.transPerResolutionElement(singleS2N);
                dataSets1.add(Recipe$.MODULE$.createS2NChartPerRes(singleS2N, finalS2N,
                        "Single Exposure and Final S/N in aperture per resolution Element",
                        ITCChart.DarkGreen, ITCChart.DarkRed));
                listPerResElement.add(singleS2N);
                listPerResElement.add(finalS2N);
            }
            groups.add(dataSets1);
        }

        if (!forGPP) {
            // Build combined full-range spectra by stitching blue and red arm spectra.
            // Blue arm data is used up to its computed end (≈ WAVELENGTH_BLUE_END);
            // red arm data covers the remainder. The combined grid uses blue's pixel
            // width and spans from blueStart to redEnd.
            //
            // listPerResElement layout: [0]=blue single/res, [1]=blue final/res,
            //                           [2]=red  single/res, [3]=red  final/res
            final VisitableSampledSpectrum sigSpectrumComb = buildCombinedSpectrum(
                    r[0].specS2N()[0].getSignalSpectrum(),
                    r[1].specS2N()[0].getSignalSpectrum());
            final VisitableSampledSpectrum backGroundSpectComb = buildCombinedSpectrum(
                    r[0].specS2N()[0].getBackgroundSpectrum(),
                    r[1].specS2N()[0].getBackgroundSpectrum());
            final VisitableSampledSpectrum expS2NComb = buildCombinedSpectrum(
                    r[0].specS2N()[0].getExpS2NSpectrum(),
                    r[1].specS2N()[0].getExpS2NSpectrum());
            final VisitableSampledSpectrum finalS2NSpecComb = buildCombinedSpectrum(
                    r[0].specS2N()[0].getFinalS2NSpectrum(),
                    r[1].specS2N()[0].getFinalS2NSpectrum());
            final VisitableSampledSpectrum singleS2NCombPerRes = buildCombinedSpectrum(
                    listPerResElement.get(0),
                    listPerResElement.get(2));
            final VisitableSampledSpectrum finalS2NSpecCombPerRes = buildCombinedSpectrum(
                    listPerResElement.get(1),
                    listPerResElement.get(3));

            final List<SpcChartData> combined = new ArrayList<SpcChartData>();
            combined.add(Recipe$.MODULE$.createSignalChart(sigSpectrumComb, "Signal",
                    backGroundSpectComb, "SQRT(Background)",
                    "Signal & SQRT(Background) in one pixel"));

            combined.add(Recipe$.MODULE$.createS2NChart(expS2NComb, finalS2NSpecComb,
                    "Single Exp S/N", "Final S/N  ",
                    "Signal / Noise per spectral pixel"
                    ));

            combined.add(Recipe$.MODULE$.createS2NChartPerRes(singleS2NCombPerRes, finalS2NSpecCombPerRes,
                    "Single Exposure and Final S/N in aperture per resolution Element",
                    ITCChart.DarkGreen, ITCChart.DarkRed));
            groups.add(combined);
        }
    return Recipe$.MODULE$.serviceGhostResult(r, groups, headless);
}

    public SpectroscopyResult[] calculateSpectroscopy() {
        int length = _mainInstrument.length;

        final SpectroscopyResult[] results = new SpectroscopyResult[length];
        for (int i = 0; i < length; i++) {
            results[i] = calculateSpectroscopy(_mainInstrument[i]);
        }
        Log.info("calculateSpectroscopy getImageQuality[0]: " +
                            results[0].iqCalc().getImageQuality() + " [1]: "+ results[1].iqCalc().getImageQuality());
        return results;
    }

    private SpectroscopyResult calculateSpectroscopy(Ghost instrument) {
        Log.fine("Calculating...");

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
        double totalspsf = 0.0;  // total source fraction in the aperture
        int numfibers = 0;       // number of fibers being summed
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
            Log.fine(String.format("x = %5.2f  y = %5.2f  fractionSource = %s", h.getIfuPosX(), h.getIfuPosY(), h.getFractionOfSourceInAperture()));
        }
        Log.fine(String.format("slitLength = %.2f, throughput = %.5f, onePix = %.5f, numfibers = %d, centerFiber = %d",
           instrument.getSlitLength(), totalspsf, sf_list.get((sf_list.size() - 1) / 2), numfibers, (sf_list.size() - 1) / 2));


        final SpectroscopyS2N specS2NCalcMethod = getSpectroscopyS2N(instrument.getCamera());
        final ObservationDetails cameraObsDetails = new ObservationDetails(specS2NCalcMethod, _obsDetailParameters.analysisMethod());

        // Each arm covers only its own wavelength sub-range:
        //   Blue: instrument.getObservingStart() .. WAVELENGTH_BLUE_END (542 nm)
        //   Red:  WAVELENGTH_RED_START (520 nm)  .. instrument.getObservingEnd()
        final double effectiveStart = (instrument.getDetector() == Detector.BLUE)
                ? instrument.getObservingStart()
                : WAVELENGTH_RED_START;
        final double effectiveEnd = (instrument.getDetector() == Detector.BLUE)
                // Apparently the calculation is done with obsEnd - 1, so adding one gets the full range of blue
                ? WAVELENGTH_BLUE_END + 1
                : instrument.getObservingEnd();

        final SpecS2NSlitVisitor specS2N = new SpecS2NSlitVisitor(
                slit,
                slit,
                instrument.disperser.get(),
                throughput,
                instrument.getSpectralPixelWidth(),   // using the grating dispersion * spectralBinning
                effectiveStart,
                effectiveEnd,
                IQcalc.getImageQuality(),
                instrument.getReadNoise(),
                instrument.getDarkCurrent() * instrument.getSpatialBinning() * instrument.getSpectralBinning(),
                cameraObsDetails,
                false);

        specS2N.setSourceSpectrum(sed.sed);
        specS2N.setBackgroundSpectrum(sed.sky);
        sed.sed.accept(specS2N);

        final SpecS2N[] specS2Narr = new SpecS2N[] {specS2N};

        VisitableSampledSpectrum singleS2NSpectrum = specS2N.getExpS2NSpectrum();
        VisitableSampledSpectrum finalS2NSpectrum = specS2N.getFinalS2NSpectrum();

        final SpectroscopyS2N calcMethod = getSpectroscopyS2N(instrument.getCamera());
        final AllIntegrationTimes times = AllIntegrationTimes.single(new IntegrationTime(calcMethod.exposureTime(), calcMethod.exposures()));

        return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, throughput.throughput(), Option.empty(),
                RecipeUtil.instance().signalToNoiseAt(specS2NCalcMethod.atWithDefault(), singleS2NSpectrum, finalS2NSpectrum),
                times);
    }

    /**
     * Builds a combined full-range spectrum by stitching blue and red arm spectra.
     * Blue arm data is used for wavelengths up to its computed end (≈ WAVELENGTH_BLUE_END);
     * red arm data is used beyond that point.
     * The result has uniform sampling equal to the blue arm's pixel width and spans
     * from blueSpec.getStart() to redSpec.getEnd(). Linear interpolation (getY(double))
     * maps each arm onto the combined pixel grid, returning 0 outside a spectrum's range.
     */
    private VisitableSampledSpectrum buildCombinedSpectrum(
            final VisitableSampledSpectrum blueSpec,
            final VisitableSampledSpectrum redSpec) {
        final double start      = blueSpec.getStart();
        final double pixelWidth = blueSpec.getSampling();
        final double end        = redSpec.getEnd();
        final int n             = (int) ((end - start) / pixelWidth) + 1;
        final double[] y        = new double[n];
        for (int i = 0; i < n; i++) {
            final double x = start + i * pixelWidth;
            // Use blue arm data up to its computed end; red arm data beyond that.
            y[i] = (x <= blueSpec.getEnd()) ? blueSpec.getY(x) : redSpec.getY(x);
        }
        return new DefaultSampledSpectrum(y, start, pixelWidth);
    }

    private SpectroscopyS2N getSpectroscopyS2N(GhostCameraParameters camera) {
        // GPP will supply a calculation method per camera, but some places do not. So, we will use the
        // camera one if available, but drop back to the one in the observation details if not. It isn't ideal to
        // always create a whole new calculation method, but it is hard to work with the Scala option in Java.
        // final CalculationMethod calcMethod = instrument.getCamera().calculationMethodOrDefault(_obsDetailParameters.calculationMethod());
        final CalculationMethod calcMethod = camera.calculationMethodOrDefault(_obsDetailParameters.calculationMethod());
        if (calcMethod instanceof SpectroscopyS2N) {
            return (SpectroscopyS2N) calcMethod;
        } else {
            throw new Error("Ghost only supports SpectroscopyS2N calculation method");
        }
    }

    private Ghost[] createGhost(final GhostParameters parameters, final ObservationDetails observationDetails) {
        return new Ghost[] {new Ghost(parameters, observationDetails, Detector.BLUE),
                            new Ghost(parameters, observationDetails, Detector.RED)};
    }


}
