package edu.gemini.itc.igrins2;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import scala.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


// This class performs the calculations for IGRINS2
public final class Igrins2Recipe {
    private static final Logger Log = Logger.getLogger(Igrins2Recipe.class.getName());
    private final ItcParameters p;
    private final Igrins2[] _mainInstrument;
    private final Igrins2Parameters igrins2Parameters;
    private final ObservingConditions _obsConditionParameters;
    private final ObservationDetails _obsDetailParameters;
    private final SourceDefinition _sdParameters;
    private final TelescopeDetails _telescope;
    private final CalculationMethod _calcMethod;
    private double exposureTime;
    private int numberExposures;
    private double SnrWavelength;

    // Construct an Igrins2Recipe
    public Igrins2Recipe(final ItcParameters p, final Igrins2Parameters instr)
    {
        this.p                  = p;
        _mainInstrument         = createIgrins2(instr, p.observation());
        igrins2Parameters       = instr;
        _sdParameters           = p.source();
        _obsDetailParameters    = p.observation();
        _obsConditionParameters = p.conditions();
        _telescope              = p.telescope();
        _calcMethod             = p.observation().calculationMethod();
        validateInputParameters();
    }

    private Igrins2[] createIgrins2(final Igrins2Parameters parameters, final ObservationDetails observationDetails) {
        return new Igrins2[] {new Igrins2(parameters, observationDetails, Igrins2Arm.H),
                              new Igrins2(parameters, observationDetails, Igrins2Arm.K)};
    }

    private void validateInputParameters() {
        if (_calcMethod instanceof SpectroscopyS2N) {
            if (_obsDetailParameters.exposureTime() < 1.63) {
                throw new IllegalArgumentException("The minimum exposure time is 1.63 seconds.");
            } else if (_obsDetailParameters.exposureTime() > 1800) {
                throw new IllegalArgumentException("The maximum exposure time is 1800 seconds.");
            }
        }
        Validation.validate(_mainInstrument[0], _obsDetailParameters, _sdParameters);  // other general validations
    }

    public ItcSpectroscopyResult serviceResult(final SpectroscopyResult[] result, final boolean headless) {
        final List<List<SpcChartData>> groups = new ArrayList<>();
        for (SpectroscopyResult r : result){
            final List<SpcChartData> dataSets = new ArrayList<SpcChartData>();
            String b = ((Igrins2) r.instrument()).getWaveBand();
            dataSets.add(Recipe$.MODULE$.createSignalChart(r,
                    "Signal and Sqrt(Background) in 1 pixel: " + b,0));
            dataSets.add(Recipe$.MODULE$.createS2NChart(r,
                    "Intermediate Single-Exposure S/N and Final S/N with Sky Sub: " + b,0));
            groups.add(dataSets);
        }
        return Recipe$.MODULE$.serviceGroupedResult(result, groups, headless);
    }

    public SpectroscopyResult[] calculateSpectroscopy() {

        Igrins2Arm arm;
        int instIndex;
        final double skyAper = 1.0;  // hard-coded in the IGRINS-2 web form
        double readNoise;
        double totalTime;

        Log.fine("calcMethod = " + _calcMethod);

        if (_calcMethod instanceof SpectroscopyS2N) {  // user has specified exposure time and number of exposures
            exposureTime = _obsDetailParameters.exposureTime();
            numberExposures = ((SpectroscopyS2N) _calcMethod).exposures();

        } else if (_calcMethod instanceof SpectroscopyInt) {  // determine the optimal exposure time and number of exposures

            double desiredSNR = ((SpectroscopyInt) _calcMethod).sigma();
            Log.fine(String.format("desiredSNR = %.2f", desiredSNR));

            SnrWavelength = ((SpectroscopyInt) _obsDetailParameters.calculationMethod()).wavelengthAt() * 1000.;
            Log.fine(String.format("Wavelength = %.2f nm", SnrWavelength));

            if (Igrins2Arm.H.getWavelengthStart() <= SnrWavelength && SnrWavelength <= Igrins2Arm.H.getWavelengthEnd()) {
                instIndex = 0;
                arm = Igrins2Arm.H;
            } else if (Igrins2Arm.K.getWavelengthStart() <= SnrWavelength && SnrWavelength <= Igrins2Arm.K.getWavelengthEnd()) {
                instIndex = 1;
                arm = Igrins2Arm.K;
            } else {
                throw new RuntimeException("The wavelength where the S/N is to be measured is not in the output.");
            }
            Log.fine("Arm = " + arm.getName());

            Igrins2 instrument = _mainInstrument[instIndex];

            exposureTime = instrument.getDefaultExposureTime();
            double initialExposureTime = exposureTime;  // remember for when calculating the signal/s and background/s
            numberExposures = 4;
            Log.fine(String.format("exposureTime = %.2f sec", exposureTime));
            Log.fine("numberExposures = " + numberExposures);

            Log.fine("Performing an initial run of the ITC to get the signal and background for this target");
            SpectroscopyResult[] initialResults = new SpectroscopyResult[_mainInstrument.length];
            for (int i = 0; i < _mainInstrument.length; i++) {
                initialResults[i] = calculateSpectroscopy(_mainInstrument[i], exposureTime, numberExposures);
            }

            // Find the peak flux in each arm to determine the maximum exposure time
            double maxExposureTime = instrument.getMaxExposureTime();  // start with the longest allowed time
            for (SpectroscopyResult r : initialResults) {  // reduce timeToHalfMax based on the peak flux in each arm
                double maxFlux = r.instrument().maxFlux();
                Log.fine(String.format("Max recommended flux for this arm = %.0f e-", maxFlux));
                final SpecS2N s = r.specS2N()[0];
                double peakFlux = s.getPeakPixelCount();
                Log.fine(String.format("maxExposureTime for this arm = %.3f sec", maxFlux / 2. / peakFlux * exposureTime));
                maxExposureTime = Math.min(maxExposureTime, maxFlux / 2. / peakFlux * exposureTime);
            }
            Log.fine(String.format("maxExposureTime = %.3f seconds", maxExposureTime));

            if (maxExposureTime < instrument.getMinExposureTime()) throw new RuntimeException(String.format(
                    "This target is too bright for this configuration.\n" +
                            "The detector will reach half of the linearity limit in %.2f seconds.", maxExposureTime));

            SpecS2N specS2N = initialResults[instIndex].specS2N()[0];

            VisitableSampledSpectrum backgroundSpectrum = specS2N.getTotalBackgroundSpectrum();
            double background = backgroundSpectrum.getY(SnrWavelength);
            Log.fine(String.format("Background @ %.1f nm = %.2f e- (summed over aperture)", SnrWavelength, background));

            VisitableSampledSpectrum signalSpectrum = specS2N.getTotalSignalSpectrum();
            double signal = signalSpectrum.getY(SnrWavelength);
            Log.fine(String.format("Signal @ %.1f nm = %.2f e- (summed over aperture)", SnrWavelength, signal));

            int numberPixels = specS2N.getSlitLengthPixels();
            Log.fine("Pixels in aperture = " + numberPixels);

            double darkNoise = specS2N.getTotalDarkNoise();
            Log.fine(String.format("Dark Current = %.2f e- (summed over aperture)", darkNoise));

            // Look up the S/N calculated by the ITC for comparison:
            VisitableSampledSpectrum finalS2NSpectrum = specS2N.getFinalS2NSpectrum();
            Log.fine(String.format("S/N @ %.1f nm = %.3f", SnrWavelength, finalS2NSpectrum.getY(SnrWavelength)));

            // Calculate the S/N and verify that the answer is the same:
            readNoise = instrument.getReadNoise(exposureTime) * instrument.getReadNoise(exposureTime) * numberPixels;
            Log.fine(String.format("Predicted S/N = %.3f", calculateSNR(signal, background, darkNoise, readNoise, skyAper, numberExposures)));

            int iterations = 0;
            int oldNumberExposures = -1;
            double oldExposureTime = -1;
            while ( (exposureTime != oldExposureTime || numberExposures != oldNumberExposures) && iterations < 10) {
                Log.fine("Iteration " + iterations + " ----------------");
                oldNumberExposures = numberExposures;
                oldExposureTime = exposureTime;
                readNoise = instrument.getReadNoise(exposureTime) * instrument.getReadNoise(exposureTime) * numberPixels;
                exposureTime = calculateExposureTime(signal / initialExposureTime,
                        background / initialExposureTime, darkNoise / initialExposureTime,
                        readNoise, skyAper, desiredSNR / Math.sqrt(numberExposures));
                totalTime = exposureTime * numberExposures;
                numberExposures = (int) Math.ceil(totalTime / maxExposureTime);
                if (numberExposures % 4 != 0) { numberExposures += 4 - numberExposures % 4;}  // make a multiple of 4
                exposureTime = totalTime / numberExposures;
                Log.fine(String.format("Iteration %d: -> %d x %.3f sec = %.3f sec (RN=%.2f e-)",
                        iterations, numberExposures, exposureTime, totalTime, instrument.getReadNoise(exposureTime)));
                iterations += 1;
            }

            Log.fine("Converged");
            if (exposureTime > 10.) {
                exposureTime = Math.ceil(exposureTime);  // round long times up to an integer
            } else {
                exposureTime = (Math.ceil(exposureTime * 10.0)) / 10.0;  // round short times up to 0.1 sec
            }
            Log.fine(String.format("Rounded exposureTime = %.2f", exposureTime));

            if (exposureTime < instrument.getMinExposureTime()) {
                Log.fine("Setting the exposure time to the minimum allowed");
                exposureTime = instrument.getMinExposureTime();
            }
        } else {
            throw new Error("Unsupported calculation method");
        }
        Log.fine(String.format("Exposure Time = %.2f", exposureTime));
        Log.fine("Number of Exposures = " + numberExposures);

        // Run the ITC calculations for each arm
        final SpectroscopyResult[] results = new SpectroscopyResult[_mainInstrument.length];
        for (int i = 0; i < _mainInstrument.length; i++) {
            Log.fine("----- Final run of " + _mainInstrument[i].getArm().getName());
            results[i] = calculateSpectroscopy(_mainInstrument[i], exposureTime, numberExposures);
        }
        return results;
    }

    private SpectroscopyResult calculateSpectroscopy(
            Igrins2 instrument,
            final double exposureTime,
            final int numberExposures)
            {

        Log.fine("Calculating from " + instrument.getWavelengthStart() + " to " + instrument.getWavelengthEnd() + " nm");

        // Module 1b
        // Define the source energy (as function of wavelength).
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifted SED

        Log.fine("Calculating image quality...");
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        final Option<AOSystem> altair;
        if (igrins2Parameters.altair().isDefined()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(),
                    IQcalc.getImageQuality(), _obsConditionParameters.ccExtinction(), igrins2Parameters.altair().get(), 0.0);
            altair = Option.apply(ao);
        } else {
            altair = Option.empty();
        }

        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, _sdParameters, _obsConditionParameters, _telescope, altair);

        // Module 1a
        // The purpose of this section is to calculate the fraction of the source flux contained within an aperture
        // adopted to derive the signal-to-noise ratio. There are several cases depending on the source morphology.
        // Define the source morphology
        // inputs: source morphology specification

        final double im_qual = altair.isDefined() ? altair.get().getAOCorrectedFWHM() : IQcalc.getImageQuality();

        // We are bypassing morphology modules 3a-5a. i.e. the output morphology is same as the input morphology.

        final Slit slit = Slit$.MODULE$.apply(_sdParameters, _obsDetailParameters, instrument, instrument.getSlitWidth(), IQcalc.getImageQuality());
        final SlitThroughput throughput = new SlitThroughput(_sdParameters, slit, im_qual);

        Log.fine(String.format("Read Noise = %.2f e- (per pixel)", instrument.getReadNoise(exposureTime)));

        Log.fine("Creating specS2N...");
        final SpecS2NSlitVisitor specS2N = new SpecS2NSlitVisitor(
                slit,
                instrument.disperser.get(),
                throughput,
                instrument.getSpectralPixelWidth(),
                instrument.getWavelengthStart(),
                instrument.getWavelengthEnd(),
                im_qual,
                instrument.getReadNoise(exposureTime),
                instrument.getDarkCurrent(),
                _obsDetailParameters,
                exposureTime,
                numberExposures);

        Log.fine("Setting source spectrum...");
        specS2N.setSourceSpectrum(calcSource.sed);
        specS2N.setBackgroundSpectrum(calcSource.sky);
        calcSource.sed.accept(specS2N);

        final SpecS2N[] specS2Narr = new SpecS2N[]{specS2N};

        VisitableSampledSpectrum singleS2NSpectrum = specS2N.getExpS2NSpectrum();
        VisitableSampledSpectrum finalS2NSpectrum = specS2N.getFinalS2NSpectrum();
        double singleSnr = singleS2NSpectrum.getY(SnrWavelength);
        double finalSnr = finalS2NSpectrum.getY(SnrWavelength);
        Log.fine(String.format("single S/N @ %.1f nm = %.3f", SnrWavelength, singleSnr));
        Log.fine(String.format("final S/N @ %.1f nm = %.3f", SnrWavelength, finalSnr));

        return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, throughput.throughput(), altair,
                RecipeUtil.instance().signalToNoiseAt(SnrWavelength, singleS2NSpectrum, finalS2NSpectrum),
                Option.apply(AllExposureCalculations.single(new TotalExposure(exposureTime, numberExposures))));
    }

    private double calculateSNR(double signal, double background, double darkNoise, double readNoise, double skyAper, int numberExposures) {
        final double noiseFactor = 1. + (1. / skyAper);
        return Math.sqrt(numberExposures) * signal / Math.sqrt(signal + noiseFactor * (background + darkNoise + readNoise));
    }

    /**
     * Calculate the exposure time required to achieve a desired Signal-to-Noise on a single frame.
     * @param signal The signal per second summed over the pixels in the aperture.
     * @param background The background per second summed over the pixels in the aperture.
     * @param darkNoise The dark current per second summed over the pixels in the aperture.
     * @param readNoise The squared read noise summed over the pixels in the aperture.
     * @param skyAper
     * @param SNR The desired Signal-to-Noise Ratio.
     * @return The exposure time in seconds.
     */
    private double calculateExposureTime(double signal, double background, double darkNoise, double readNoise, double skyAper, double SNR) {
        final double f = 1. + (1. / skyAper);  // noise factor
        // SNR = S / sqrt(S + f(B + D + R)) = st / sqrt(st + f(bt + dt + R)) = x
        // This can be expressed as a quadratic equation:  (ss/xx)t^2 - (s + fb + fd)t - fR = 0
        // and solved using the quadratic formula: t = (-b + sqrt(b^2 - 4ac)) / 2a
        double a = signal * signal / (SNR * SNR);
        double b = -(signal + f * background + f * darkNoise);
        double c = -f * readNoise;
        double exposureTime = (-b + Math.sqrt(b*b - 4.*a*c)) / (2.*a);
        Log.fine(String.format("exposureTime = %.3f", exposureTime));
        return exposureTime;
    }

    public double getExposureTime() { return exposureTime; }

    public int getNumberExposures() { return numberExposures; }

}
