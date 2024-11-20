package edu.gemini.itc.ghost;

import edu.gemini.itc.base.*;
import edu.gemini.itc.gnirs.GnirsRecipe;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.ghost.Detector;
import scala.Option;
import scala.Some;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

    private final short WAVELENGTH_BLUE_END = 542; // nm . Range 347 - 542 nm

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
        final List<List<SpcChartData>> groups = new ArrayList<>();
        final List<SpcChartData> combined = new ArrayList<SpcChartData>();
        List<VisitableSampledSpectrum> listPerResElement = new ArrayList<VisitableSampledSpectrum>();
        for (SpectroscopyResult r1 : r) {
            final List<SpcChartData> dataSets1 = new ArrayList<SpcChartData>();
            dataSets1.add(Recipe$.MODULE$.createSignalChart(r1, 0));
            dataSets1.add(Recipe$.MODULE$.createS2NChart(r1, "Single Exposure and Final S/N in aperture per pixel", 0));
            VisitableSampledSpectrum finalS2N = (VisitableSampledSpectrum) r1.specS2N()[0].getFinalS2NSpectrum().clone();
            Ghost inst = (Ghost) r1.instrument();
            inst.transPerResolutionElement(finalS2N);
            VisitableSampledSpectrum singleS2N = (VisitableSampledSpectrum) r1.specS2N()[0].getExpS2NSpectrum().clone();
            inst.transPerResolutionElement(singleS2N);
            dataSets1.add(Recipe$.MODULE$.createS2NChartPerRes(singleS2N, finalS2N,
                    "Single Exposure and Final S/N in aperture per resolution Element",
                    ITCChart.DarkGreen, ITCChart.DarkRed));
            groups.add(dataSets1);
            listPerResElement.add(singleS2N);
            listPerResElement.add(finalS2N);
        }
        // This part creates a single graph to include the blue and red band.
        // This last graph is the one shown as the final result in the Ghost Web result.
        VisitableSampledSpectrum singleS2NCombPerRes = (VisitableSampledSpectrum) listPerResElement.get(2).clone();
        VisitableSampledSpectrum finalS2NSpecCombPerRes = (VisitableSampledSpectrum) listPerResElement.get(3).clone();
        VisitableSampledSpectrum sigSpectrumComb = (VisitableSampledSpectrum) r[1].specS2N()[0].getSignalSpectrum().clone();
        VisitableSampledSpectrum backGroundSpectComb = (VisitableSampledSpectrum) r[1].specS2N()[0].getBackgroundSpectrum().clone();
        VisitableSampledSpectrum expS2NComb = (VisitableSampledSpectrum) r[1].specS2N()[0].getExpS2NSpectrum().clone();
        VisitableSampledSpectrum finalS2NSpecComb = (VisitableSampledSpectrum) r[1].specS2N()[0].getFinalS2NSpectrum().clone();
        int indexWV = r[0].specS2N()[0].getSignalSpectrum().getLowerIndex(WAVELENGTH_BLUE_END);
        for (int i = 0; i <= indexWV; ++i) {
            sigSpectrumComb.setY(i, r[0].specS2N()[0].getSignalSpectrum().getY(i));
            backGroundSpectComb.setY(i, r[0].specS2N()[0].getBackgroundSpectrum().getY(i));
            expS2NComb.setY(i, r[0].specS2N()[0].getExpS2NSpectrum().getY(i));
            finalS2NSpecComb.setY(i, r[0].specS2N()[0].getFinalS2NSpectrum().getY(i));
            singleS2NCombPerRes.setY(i, listPerResElement.get(0).getY(i));
            finalS2NSpecCombPerRes.setY(i, listPerResElement.get(1).getY(i));
        }

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
        return Recipe$.MODULE$.serviceGroupedResult(r, groups, headless);
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

         final SpecS2NSlitVisitor specS2N = new SpecS2NSlitVisitor(
                slit,
                slit,
                instrument.disperser.get(),
                throughput,
                instrument.getSpectralPixelWidth(),   // using the grating dispersion * spectralBinning
                 instrument.getObservingStart(),
                instrument.getObservingEnd(),
                IQcalc.getImageQuality(),
                instrument.getReadNoise(),
                instrument.getDarkCurrent() * instrument.getSpatialBinning() * instrument.getSpectralBinning(),
                _obsDetailParameters,
                 false);

        specS2N.setSourceSpectrum(sed.sed);
        specS2N.setBackgroundSpectrum(sed.sky);
        sed.sed.accept(specS2N);

        final SpecS2N[] specS2Narr = new SpecS2N[] {specS2N};
        return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, throughput.throughput(), Option.empty(), Option.empty());
    }


    private Ghost[] createGhost(final GhostParameters parameters, final ObservationDetails observationDetails) {
        return new Ghost[] {new Ghost(parameters, observationDetails, Detector.BLUE),
                            new Ghost(parameters, observationDetails, Detector.RED)};
    }


}
