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
    public static final double MIN_EXPTIME = 1.63;  // seconds

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

        if (_calcMethod instanceof SpectroscopyS2N) {  // the user has specified exposure time and number of exposures
            exposureTime = _obsDetailParameters.exposureTime();
            numberExposures = ((SpectroscopyS2N) _calcMethod).exposures();





        } else if (_calcMethod instanceof SpectroscopyInt) {  // determine the optimal exposure time and number of exposures
            exposureTime = 600.0;  // these are arbitrary starting points
            numberExposures = 4;

            double desiredSNR = ((SpectroscopyInt) _calcMethod).sigma();
            Log.fine(String.format("desiredSNR = %.2f", desiredSNR));

            double wavelength = ((SpectroscopyInt) _obsDetailParameters.calculationMethod()).wavelength();
            Log.fine(String.format("Wavelength = %.2f nm", wavelength));

            if (Igrins2Arm.H.getWavelengthStart() <= wavelength && wavelength <= Igrins2Arm.H.getWavelengthEnd()) {
                instIndex = 0;
                arm = Igrins2Arm.H;
            } else if (Igrins2Arm.K.getWavelengthStart() <= wavelength && wavelength <= Igrins2Arm.K.getWavelengthEnd()) {
                instIndex = 1;
                arm = Igrins2Arm.K;
            } else {
                throw new RuntimeException("The wavelength where the S/N is to be measured is not in the output.");
            }
            Log.fine("Arm = " + arm.getName());

            Igrins2 instrument = _mainInstrument[instIndex];

            double maxFlux = arm.get_maxRecommendedFlux();
            Log.fine(String.format("maxFlux = %.0f e-", maxFlux));

            readNoise = instrument.getReadNoise(exposureTime);  // per pixel
            Log.fine(String.format("Read Noise = %.2f e- (per pixel)", readNoise));

            // Perform an initial run of the ITC to get the signal and background for this target at this wavelength

            double initialExposureTime = 600.;  // the initial input values are arbitrary
            int initialNumberExposures = 4;
            Log.fine(String.format("initialExposureTime = %.2f sec", initialExposureTime));
            Log.fine("initialNumberExposures = " + initialNumberExposures);

            SpectroscopyResult result = calculateSpectroscopy(instrument, initialExposureTime, initialNumberExposures);
            SpecS2N specS2N = result.specS2N()[0];

            double peakFlux = specS2N.getPeakPixelCount();
            Log.fine(String.format("peakFlux = %.0f e-", peakFlux));

            VisitableSampledSpectrum backgroundSpectrum = specS2N.getTotalBackgroundSpectrum();
            double background = backgroundSpectrum.getY(wavelength);
            Log.fine(String.format("Background @ %.1f nm = %.2f e- (summed over aperture)", wavelength, background));

            VisitableSampledSpectrum signalSpectrum = specS2N.getTotalSignalSpectrum();
            double signal = signalSpectrum.getY(wavelength);
            Log.fine(String.format("Signal @ %.1f nm = %.2f e- (summed over aperture)", wavelength, signal));

            int numberPixels = specS2N.getSlitLengthPixels();
            Log.fine("Pixels in aperture = " + numberPixels);

            Log.fine(String.format("Read Noise = %.2f e- (per pixel)", instrument.getReadNoise(initialExposureTime)));
            readNoise = instrument.getReadNoise(initialExposureTime) * instrument.getReadNoise(exposureTime) * numberPixels;
            Log.fine(String.format("Read Noise = %.2f e- (squared and summed over aperture)", readNoise));
            //Log.fine(String.format("Read Noise = %.3f e- (from specS2N)", specS2N.getTotalReadNoise()));

            double darkNoise = specS2N.getTotalDarkNoise();
            Log.fine(String.format("Dark Current = %.2f e- (summed over aperture)", darkNoise));

            // Look up the S/N calculated by the ITC for comparison:
            VisitableSampledSpectrum finalS2NSpectrum = specS2N.getFinalS2NSpectrum();
            Log.fine(String.format("S/N @ %.1f nm = %.3f e-", wavelength, finalS2NSpectrum.getY(wavelength)));

            // Calculate the S/N and verify that the answer is the same:
            Log.fine(String.format("Predicted S/N = %.3f e-", calculateSNR(signal, background, darkNoise, readNoise, skyAper, initialNumberExposures)));

            double timeToHalfMax = maxFlux / 2. / peakFlux * initialExposureTime;  // time to reach half the maximum flux
            Log.fine(String.format("timeToHalfMax = %.2f seconds", timeToHalfMax));

            if (timeToHalfMax < 1.0) throw new RuntimeException(String.format(
                    "This target is too bright for this configuration.\n" +
                            "The detector will reach %.0f ADU in %.2f seconds.", maxFlux/2., timeToHalfMax));

            double maxExptime = Math.min(300, (int) timeToHalfMax);
            Log.fine(String.format("maxExptime = %.2f seconds", maxExptime));

            // Step through each of the read modes and see which works best.
            List<ReadMode> readModes = Arrays.asList(ReadMode.FAINT_OBJECT_SPEC, ReadMode.MEDIUM_OBJECT_SPEC, ReadMode.BRIGHT_OBJECT_SPEC);
            for (ReadMode rm : readModes) {
                readMode = rm;
                Log.fine("=== Checking " + readMode.toString() + " read mode ===");

                readNoise = readMode.readNoise() * readMode.readNoise() * numberPixels;
                Log.fine(String.format("Read Noise = %.2f e- (squared and summed)", readNoise));
                Log.fine(String.format("S/N = %.3f", calculateSNR(signal, background, darkNoise, readNoise, skyAper, initialNumberExposures)));

                exposureTime = calculateExposureTime(signal / initialExposureTime,
                        background / initialExposureTime, darkNoise / initialExposureTime,
                        readNoise, skyAper, desiredSNR / Math.sqrt(initialNumberExposures));
                totalTime = exposureTime * initialNumberExposures;
                Log.fine(String.format("totalTime = %.2f sec", totalTime));

                // Calculate the optimal number of exposures:
                numberExposures = (int) Math.ceil(totalTime / maxExptime);
                if (numberExposures % 4 != 0) {
                    numberExposures += 4 - numberExposures % 4;  // make a multiple of 4
                }
                Log.fine(String.format("numberExposures = %d", numberExposures));

                // Calculate the new exposure time with this number of exposures:
                exposureTime = calculateExposureTime(signal / initialExposureTime,
                        background / initialExposureTime, darkNoise / initialExposureTime,
                        readNoise, skyAper, desiredSNR / Math.sqrt(numberExposures));

                if (exposureTime < readMode.recomendedExpTimeSec()) {  // continue to the next read mode
                    Log.fine(String.format("This is shorter than recommended (%.1f sec)", readMode.recomendedExpTimeSec()));
                } else {  // Accept this read mode
                    break;
                }
            }

            Log.fine("=== The read mode, exposure time, and number of exposures have been decided ===");
            Log.fine("readMode = " + readMode.toString());
            Log.fine(String.format("numberExposures = %d", numberExposures));
            if (exposureTime < readMode.minimumExpTimeSec()) {
                Log.fine("Reducing exposure time to the minimum allowed for the read mode");
                exposureTime = readMode.minimumExpTimeSec();
            }
            Log.fine(String.format("exposureTime = %.3f", exposureTime));
        }

        Log.fine("Exposure Time = " + exposureTime);
        Log.fine("Number Exposures = " + numberExposures);

















        final SpectroscopyResult[] results = new SpectroscopyResult[_mainInstrument.length];
        for (int i = 0; i < _mainInstrument.length; i++) {
            results[i] = calculateSpectroscopy(_mainInstrument[i], exposureTime, numberExposures);
        }

       if (_calcMethod instanceof SpectroscopyS2N) {
           return results;
       } else if (_calcMethod instanceof SpectroscopyInt) {
           return iterate(results);
       } else {
           throw new Error("Unsupported calculation method");
       }
    }




    private SpectroscopyResult[] iterate(final SpectroscopyResult[] initialResults) {

        // 1. Process both arms to get the peak flux and derive the maximum exposure time.
        // 2. Figure out which detector includes the wavelength of interest.
        // 3. Iteratively determine exposureTime & numberExposures that will give the requested S/N at wavelength.
        // 4. Process all the CCDs using the final exposureTime & numberExposures and return the result.

        final double wavelength = ((SpectroscopyInt) _obsDetailParameters.calculationMethod()).wavelength() * 1000.0;  // where to measure S/N
        int inst;   // instrument array index that includes the requested wavelength
        Igrins2Arm arm;
        if (Igrins2Arm.H.getWavelengthStart() <= wavelength && wavelength <= Igrins2Arm.H.getWavelengthEnd()) {
            inst = 0;
            arm = Igrins2Arm.H;
        } else if (Igrins2Arm.K.getWavelengthStart() <= wavelength && wavelength <= Igrins2Arm.K.getWavelengthEnd()) {
            inst = 1;
            arm = Igrins2Arm.K;
        } else {
            throw new RuntimeException("The wavelength where the S/N is to be measured is not in the output.");
        }
        Log.fine(String.format("Wavelength = %.2f nm is on the %s arm", wavelength, arm.getName()));

        Log.fine("Checking the initial results...");
        double timeToHalfMax = 600.0;  // start with the longest allowed time
        double snr = -1.0;

        for (SpectroscopyResult r : initialResults) {  // reduce timeToHalfMax based on the peak flux in each arm
            double maxFlux = r.instrument().maxFlux();
            Log.fine("max recommended flux for this arm = " + maxFlux);
            final SpecS2N s = r.specS2N()[0];
            double peakFlux = s.getPeakPixelCount();
            Log.fine("timeToHalfMax for this arm = " + maxFlux / 2. / peakFlux * exposureTime);
            timeToHalfMax = Math.min(timeToHalfMax, maxFlux / 2. / peakFlux * exposureTime);

            if (((Igrins2) r.instrument()).getArm() == arm) {  // the requested wavelength is here
                final VisitableSampledSpectrum fins2n = s.getFinalS2NSpectrum();
                snr = fins2n.getY(wavelength);
                Log.fine(String.format("S/N @ %.2f nm = %.2f", wavelength, snr));
            }
        }
        Log.fine(String.format("timeToHalfMax = %.2f seconds", timeToHalfMax));

        if (timeToHalfMax < MIN_EXPTIME) throw new RuntimeException(String.format(
                "This target is too bright for this configuration.\n" +
                "The optimal exposure time is %.2f seconds which is less than the minimum %.2f.", timeToHalfMax, MIN_EXPTIME));

        double desiredSNR = ((SpectroscopyInt) _calcMethod).sigma();
        Log.fine(String.format("desiredSNR = %.2f", desiredSNR));

        int iterations = 0;
        double maxExptime = timeToHalfMax;
        int oldNumberExposures = 0;
        double oldExposureTime = 0.0;
        final SpectroscopyResult[] finalResults = new SpectroscopyResult[_mainInstrument.length];

        while ((numberExposures != oldNumberExposures || exposureTime != oldExposureTime) && iterations <= 5) {
            iterations += 1;
            Log.fine(String.format("--- ITERATION %d (%d != %d) or (%.3f != %.3f) ---",
                    iterations, oldNumberExposures, numberExposures, oldExposureTime, exposureTime));
            oldNumberExposures = numberExposures;
            oldExposureTime = exposureTime;
            // Estimate the time required to achieve the requested S/N assuming S/N scales as sqrt(t):
            double totalTime = exposureTime * numberExposures * (desiredSNR / snr) * (desiredSNR / snr);
            Log.fine(String.format("totalTime = %.2f", totalTime));
            numberExposures = (int) Math.ceil(totalTime / maxExptime);
            Log.fine(String.format("numberExposures = %d (raw)", numberExposures));

            if (numberExposures % 4 != 0) { numberExposures += 4 - numberExposures % 4; }  // force multiple of 4 exposures
            Log.fine(String.format("numberExposures = %d (as multiple of 4)", numberExposures));

            if ((totalTime / numberExposures) > 5.0) {
                exposureTime = Math.ceil(totalTime / numberExposures);  // round long exposure times up to an integer
            } else {
                exposureTime = (Math.ceil(totalTime / numberExposures * 10.0)) / 10.0; // round short times up to 0.1 sec
            }
            if (exposureTime < MIN_EXPTIME) { exposureTime = MIN_EXPTIME; }
            Log.fine(String.format("exposureTime = %.3f", exposureTime));

            if ((numberExposures != oldNumberExposures || exposureTime != oldExposureTime) && iterations <= 5) {
                // Try one more iteration to see if we can get closer to the requested S/N

                // only recalculate the arm with the wavelength where the S/N is being measured
                final SpectroscopyResult newResult = calculateSpectroscopy(_mainInstrument[inst], exposureTime, numberExposures, snr);
                final VisitableSampledSpectrum fins2n = newResult.specS2N()[0].getFinalS2NSpectrum();
                snr = fins2n.getY(wavelength);
                Log.fine(String.format("Iteration %d: %d x %.2f sec -> S/N @ %.2f nm = %.2f",
                        iterations, numberExposures, exposureTime, wavelength, snr));

            } else {
                if (iterations > 5) {
                    Log.fine("--- STOPPING and calculating the results for all detectors");
                } else {
                    Log.fine("--- NO CHANGE since the last iteration, so calculating final results");
                }
                for (int i = 0; i < _mainInstrument.length; i++) {
                    finalResults[i] = calculateSpectroscopy(_mainInstrument[i], exposureTime, numberExposures, snr);
                }
            }
        }
        return finalResults;
    }


    private SpectroscopyResult calculateSpectroscopy(
            Igrins2 instrument,
            final double exposureTime,
            final int numberExposures)
            {

        Log.fine("Calculating from " + instrument.getWavelengthStart() + " to " + instrument.getWavelengthEnd() + " nm");

        double readNoise = instrument.getReadNoise(exposureTime);  // per pixel
        Log.fine(String.format("Read Noise = %.2f e- (per pixel)", readNoise));

        // Calculate the total ReadNoise in the aperture:

        // How many pixels are in the aperture???



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
        return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, throughput.throughput(), altair,
                //Option.apply(new ExposureCalculation(exposureTime, numberExposures, snr)
                Option.empty());
    }

    public double getExposureTime() { return exposureTime; }
    public int getNumberExposures() { return numberExposures; }

}
