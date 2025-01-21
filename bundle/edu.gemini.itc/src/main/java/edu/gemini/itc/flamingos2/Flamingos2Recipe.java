package edu.gemini.itc.flamingos2;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.ReadMode;
import scala.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class performs the calculations for Flamingos 2 used for imaging.
 */
public final class Flamingos2Recipe implements ImagingRecipe, SpectroscopyRecipe {
    private static final Logger Log = Logger.getLogger(Flamingos2Recipe.class.getName());

    private final ItcParameters p;
    private final Flamingos2 instrument;
    private final Flamingos2Parameters _flamingos2Parameters;
    private final ObservingConditions _obsConditionParameters;
    private final ObservationDetails _obsDetailParameters;
    private final SourceDefinition _sdParameters;
    private final TelescopeDetails _telescope;
    private double exposureTime;
    private int numberExposures;
    private ReadMode readMode;

    /**
     * Constructs a Flamingos 2 object given the parameters. Useful for testing.
     */
    public Flamingos2Recipe(final ItcParameters p, final Flamingos2Parameters instr) {
        this.p                  = p;
        instrument              = new Flamingos2(instr);
        _sdParameters           = p.source();
        _obsDetailParameters    = p.observation();
        _obsConditionParameters = p.conditions();
        _flamingos2Parameters   = instr;
        _telescope              = p.telescope();
        this.exposureTime       = p.observation().exposureTime();
        this.readMode           = instrument.getReadMode();
        Log.fine("exposureTime = " + exposureTime);
        Log.fine("readMode = " + readMode.toString());
        validateInputParameters();
    }

    /**
     * Check input parameters for consistency
     */
    private void validateInputParameters() {
        if (_obsDetailParameters.calculationMethod() instanceof Spectroscopy) {
            switch (_flamingos2Parameters.grism()) {
                case NONE:          throw new IllegalArgumentException("In spectroscopy mode, a grism must be selected");
            }
            switch (_flamingos2Parameters.mask()) {
                case FPU_NONE:      throw new IllegalArgumentException("In spectroscopy mode, a FP must be selected");
            }
        }
        // some general validations
        Validation.validate(instrument, _obsDetailParameters, _sdParameters);
    }

    public ItcImagingResult serviceResult(final ImagingResult r) {
        return Recipe$.MODULE$.serviceResult(r);
    }

    public ItcSpectroscopyResult serviceResult(final SpectroscopyResult r, final boolean headless) {
        final List<SpcChartData> dataSets = new ArrayList<SpcChartData>() {{
            if (!headless) add(Recipe$.MODULE$.createSignalChart(r, 0));
            add(Recipe$.MODULE$.createS2NChart(r));
        }};
        return Recipe$.MODULE$.serviceResult(r, dataSets, headless);
    }

    public SpectroscopyResult calculateSpectroscopy() {
        final CalculationMethod calcMethod = _obsDetailParameters.calculationMethod();
        Log.fine("calcMethod = " + calcMethod);

        if (calcMethod instanceof SpectroscopyS2N) {
            numberExposures = ((SpectroscopyS2N) calcMethod).exposures();
        } else if (calcMethod instanceof SpectroscopyInt) {
            numberExposures = ((SpectroscopyInt) calcMethod).exposures();
        } else {
            throw new Error("Unsupported calculation method");
        }
        Log.fine("numberExposures = " + numberExposures);

        SpectroscopyResult result;

        if (calcMethod instanceof SpectroscopyInt) {
            // Iteratively determine exposureTime & numberExposures that will give the requested S/N at wavelength.
            exposureTime = 80.0;  // starting point; choose the longest time that still yields medium read noise mode
            Log.fine(String.format("exposureTime = %.2f", exposureTime));
            readMode = instrument.getOptimalReadMode(exposureTime);
            Log.fine("readMode = " + readMode.toString());
            result = calculateSpectroscopy(instrument, readMode, exposureTime, numberExposures);

            double wavelength = ((SpectroscopyInt) _obsDetailParameters.calculationMethod()).wavelength();
            Log.fine(String.format("Wavelength = %.2f nm", wavelength));
            SpecS2N specS2N = result.specS2N()[0];
            final VisitableSampledSpectrum fins2n = specS2N.getFinalS2NSpectrum();
            double peakFlux = specS2N.getPeakPixelCount();
            Log.fine(String.format("peakFlux = %.0f e-", peakFlux));
            double snr = fins2n.getY(wavelength);
            Log.fine(String.format("snr = %.2f", snr));
            double maxFlux = instrument.maxFlux();  // maximum useful flux
            Log.fine(String.format("maxFlux = %.0f e-", maxFlux));
            double timeToHalfMax = maxFlux / 2. / peakFlux * exposureTime;  // time to reach half the maximum (goal)
            Log.fine(String.format("timeToHalfMax = %.2f seconds", timeToHalfMax));
            if (timeToHalfMax < 1.0) throw new RuntimeException(String.format(
                    "This target is too bright for this configuration.\n" +
                    "The detector will reach %.0f ADU in %.2f seconds.", maxFlux/2., timeToHalfMax));
            int maxExptime = Math.min(300, (int) timeToHalfMax);  // 300s is an arbitrary maximum
            Log.fine(String.format("maxExptime = %d seconds", maxExptime));
            double desiredSNR = ((SpectroscopyInt) calcMethod).sigma();
            Log.fine(String.format("desiredSNR = %.2f", desiredSNR));

            int oldNumberExposures = 0;
            double oldExposureTime = 0.0;
            int iterations = 1;
            final int maxIterations = 10;
            double minExposureTime;

            while ((numberExposures != oldNumberExposures || exposureTime != oldExposureTime) && iterations <= maxIterations) {
                Log.fine(String.format("--- ITERATION %d (%d != %d) or (%.2f != %.2f) ---",
                        iterations, oldNumberExposures, numberExposures, oldExposureTime, exposureTime));
                oldNumberExposures = numberExposures;
                oldExposureTime = exposureTime;
                // Estimate the time required to achieve the requested S/N assuming S/N scales as sqrt(t):
                double totalTime = exposureTime * numberExposures * (desiredSNR / snr) * (desiredSNR / snr);
                Log.fine(String.format("totalTime = %.2f s", totalTime));

                numberExposures = (int) Math.ceil(totalTime / maxExptime);
                Log.fine(String.format("numberExposures = %d", numberExposures));
                if (numberExposures % 4 != 0) {
                    numberExposures += 4 - numberExposures % 4;  // make a multiple of 4
                    Log.fine(String.format("numberExposures = %d (as a multiple of 4)", numberExposures));
                }

                if ((totalTime / numberExposures) > 10.0) {
                    exposureTime = Math.ceil(totalTime / numberExposures);  // round long exposure times up to an integer
                } else {
                    exposureTime = (Math.ceil(totalTime / numberExposures * 10.0)) / 10.0; // round short times up to 0.1 sec
                }

                readMode = instrument.getOptimalReadMode(exposureTime);
                Log.fine("readMode = " + readMode.toString());

                minExposureTime = instrument.getMinExposureTime(readMode);
                Log.fine(String.format("minExposureTime = %.2f s", minExposureTime));

                if (exposureTime < minExposureTime) { exposureTime = minExposureTime; }
                Log.fine(String.format("exposureTime = %.3f", exposureTime));

                if ((numberExposures != oldNumberExposures || exposureTime != oldExposureTime) && iterations <= maxIterations) {
                    result = calculateSpectroscopy(instrument, readMode, exposureTime, numberExposures);
                    specS2N = result.specS2N()[0];
                    final VisitableSampledSpectrum newfins2n = specS2N.getFinalS2NSpectrum();
                    snr = newfins2n.getY(wavelength);
                    Log.fine(String.format("--> Iteration %d: %d x %.2f sec -> S/N @ %.2f nm = %.2f",
                            iterations, numberExposures, exposureTime, wavelength, snr));
                    iterations += 1;
                }
            }
        } else {
            result = calculateSpectroscopy(instrument, readMode, exposureTime, numberExposures);
        }
        return result;
    }

    public SpectroscopyResult calculateSpectroscopy(
            final Flamingos2 instrument,
            final ReadMode readMode,
            final double exposureTime,
            final int numberExposures) {

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

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();
        final double im_qual = IQcalc.getImageQuality();

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        final Slit slit = Slit$.MODULE$.apply(_sdParameters, _obsDetailParameters, instrument, instrument.getSlitWidth(), im_qual);
        final SlitThroughput throughput = new SlitThroughput(_sdParameters, slit, im_qual);

        final SpecS2NSlitVisitor specS2N = new SpecS2NSlitVisitor(
                slit,
                instrument.disperser(),
                throughput,
                instrument.getSpectralPixelWidth(),
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                im_qual,
                readMode.readNoise(),
                instrument.getDarkCurrent(),
                _obsDetailParameters,
                exposureTime,
                numberExposures);

        specS2N.setSourceSpectrum(src.sed);
        specS2N.setBackgroundSpectrum(src.sky);
        src.sed.accept(specS2N);

        final SpecS2NSlitVisitor[] specS2Narr = new SpecS2NSlitVisitor[1];
        specS2Narr[0] = specS2N;

        return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, throughput.throughput(), Option.empty(), Option.empty());
    }

    public ImagingResult calculateImaging() {

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

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();
        final double im_qual = IQcalc.getImageQuality();

        // Calculate Source fraction
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        // Get the summed source and sky
        final VisitableSampledSpectrum sed = src.sed;
        final VisitableSampledSpectrum sky = src.sky;
        final double sed_integral = sed.getIntegral();
        final double sky_integral = sky.getIntegral();

        // Calculate the Signal to Noise
        final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument, SFcalc, im_qual, sed_integral, sky_integral);
        IS2Ncalc.calculate();
        exposureTime = (int) IS2Ncalc.getExposureTime();
        numberExposures = IS2Ncalc.numberSourceExposures();

        // Calculate the Peak Pixel Flux
        final double peak_pixel_count = PeakPixelFlux.calculate(instrument, _sdParameters, exposureTime, SFcalc, im_qual, sed_integral, sky_integral);

        return ImagingResult.apply(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc);
    }

    public double getExposureTime() {
        return exposureTime;
    }

    public int getNumberExposures() {
        return numberExposures;
    }

    public ReadMode getReadMode() {
        return readMode;
    }

}
