package edu.gemini.itc.flamingos2;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.ReadMode;
import scala.Option;

import java.util.Arrays;
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

        final double skyAper = 1.0;  // hard-coded in the F2 web form
        double readNoise;
        double totalTime;
        double wavelength = 0;
        double desiredSNR = 0;

        if (calcMethod instanceof SpectroscopyInt) {
            // Determine exposureTime & numberExposures that will give the requested S/N at wavelength.

            desiredSNR = ((SpectroscopyInt) calcMethod).sigma();
            Log.fine(String.format("desiredSNR = %.2f", desiredSNR));

            wavelength = ((SpectroscopyInt) _obsDetailParameters.calculationMethod()).wavelengthAt();
            Log.fine(String.format("Wavelength = %.2f nm", wavelength));

            double maxFlux = instrument.maxFlux();  // maximum useful flux
            Log.fine(String.format("maxFlux = %.0f e-", maxFlux));

            // Perform an initial run of the ITC to get the signal and background for this target at this wavelength
            readMode = ReadMode.MEDIUM_OBJECT_SPEC;  // arbitrary choice
            Log.fine("readMode = " + readMode.toString());
            double initialExposureTime = readMode.recomendedExpTimeSec();
            int initialNumberExposures = 4;
            Log.fine(String.format("initialExposureTime = %.2f sec", initialExposureTime));
            Log.fine("initialNumberExposures = " + initialNumberExposures);

            SpectroscopyResult result = calculateSpectroscopy(instrument, readMode, initialExposureTime, initialNumberExposures, wavelength);
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

            Log.fine(String.format("Read Noise = %.2f e- (per pixel)", readMode.readNoise()));
            readNoise = readMode.readNoise() * readMode.readNoise() * numberPixels;
            Log.fine(String.format("Read Noise = %.2f e- (squared and summed over aperture)", readNoise));
            //Log.fine(String.format("Read Noise = %.3f e- (from specS2N)", specS2N.getTotalReadNoise()));

            double darkNoise = specS2N.getTotalDarkNoise();
            Log.fine(String.format("Dark Current = %.2f e- (summed over aperture)", darkNoise));

            // Look up the S/N calculated by the ITC for comparison:
            VisitableSampledSpectrum finalS2NSpectrum = specS2N.getFinalS2NSpectrum();
            Log.fine(String.format("S/N @ %.1f nm = %.3f", wavelength, finalS2NSpectrum.getY(wavelength)));

            // Calculate the S/N and verify that the answer is the same:
            Log.fine(String.format("Predicted S/N = %.3f e-", calculateSNR(signal, background, darkNoise, readNoise, skyAper, initialNumberExposures)));

            double maxExposureTime = Math.min(300, maxFlux / 2. / peakFlux * initialExposureTime);
            Log.fine(String.format("maxExposureTime = %.2f seconds", maxExposureTime));

            if (maxExposureTime < 1) throw new RuntimeException(String.format(
                    "This target is too bright for this configuration.\n" +
                            "The detector will reach %.0f ADU in %.2f seconds.", maxFlux/2., maxExposureTime));

            // Step through each of the read modes and see which works best.
            List<ReadMode> readModes = Arrays.asList(ReadMode.FAINT_OBJECT_SPEC, ReadMode.MEDIUM_OBJECT_SPEC, ReadMode.BRIGHT_OBJECT_SPEC);
            for (ReadMode rm : readModes) {
                readMode = rm;
                Log.fine("=== Checking " + readMode.toString() + " read mode ===");

                readNoise = readMode.readNoise() * readMode.readNoise() * numberPixels;
                Log.fine(String.format("Read Noise = %.2f e- (squared and summed)", readNoise));
                Log.fine(String.format("S/N = %.3f", calculateSNR(signal, background, darkNoise, readNoise, skyAper, initialNumberExposures)));

                int iterations = 0;
                int oldNumberExposures = -1;
                double oldExposureTime = -1;
                while ( (exposureTime != oldExposureTime || numberExposures != oldNumberExposures) && iterations < 10) {
                    Log.fine("Iteration " + iterations + " ----------------");
                    oldNumberExposures = numberExposures;
                    oldExposureTime = exposureTime;
                    exposureTime = calculateExposureTime(signal / initialExposureTime,
                            background / initialExposureTime, darkNoise / initialExposureTime,
                            readNoise, skyAper, desiredSNR / Math.sqrt(numberExposures));
                    totalTime = exposureTime * numberExposures;
                    numberExposures = (int) Math.ceil(totalTime / maxExposureTime);
                    if (numberExposures % 4 != 0) { numberExposures += 4 - numberExposures % 4;}  // make a multiple of 4
                    exposureTime = totalTime / numberExposures;
                    Log.fine(String.format("Iteration %d: -> %d x %.3f sec = %.3f sec (RN=%.2f e-)",
                            iterations, numberExposures, exposureTime, totalTime, readMode.readNoise()));
                    iterations += 1;
                }
                Log.fine("Converged");

                if (exposureTime < readMode.recomendedExpTimeSec()) {  // continue to the next read mode
                    Log.fine(String.format("This is shorter than recommended (%.0f sec)", readMode.recomendedExpTimeSec()));
                } else {  // Accept this read mode
                    break;
                }
            }

            Log.fine("=== The read mode, exposure time, and number of exposures have been decided ===");
            Log.fine("readMode = " + readMode.toString());
            Log.fine(String.format("numberExposures = %d", numberExposures));
            if (exposureTime < readMode.minimumExpTimeSec()) {
                Log.fine("Increasing exposure time to the minimum allowed for the read mode");
                exposureTime = readMode.minimumExpTimeSec();
            }

            exposureTime = Math.ceil(exposureTime);  // finally round up to an integer
            Log.fine(String.format("exposureTime = %.0f", exposureTime));
        }

        // Run the ITC to generate the output graphs
        return calculateSpectroscopy(instrument, readMode, exposureTime, numberExposures, wavelength).withExposureCalculation(
                AllExposures.single(new TotalExposure(exposureTime, numberExposures)));
    }

    private double calculateSNR(double signal, double background, double darkNoise, double readNoise, double skyAper, int numberExposures) {
        final double noiseFactor = 1. + (1. / skyAper);
        return Math.sqrt(numberExposures) * signal / Math.sqrt(signal + noiseFactor * (background + darkNoise + readNoise));
    }

    private double calculateExposureTime(double signal, double background, double darkNoise, double readNoise, double skyAper, double SNR) {
        // Calculate the exposure time required to achieve a desired S/N on a single frame.
        // Signal, background, and darkNoise are per second and summed over the pixels in the aperture.
        // readNoise is squared and summed over the pixels in the aperture.

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

    private SpectroscopyResult calculateSpectroscopy(
            final Flamingos2 instrument,
            final ReadMode readMode,
            final double exposureTime,
            final int numberExposures,
            final double wavelength
    ) {

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
        final double singleSnr = specS2N.getExpS2NSpectrum().getY(wavelength);
        final double finalSnr = specS2N.getFinalS2NSpectrum().getY(wavelength);
        Log.fine(String.format("single S/N @ %.1f nm = %.3f", wavelength, singleSnr));
        Log.fine(String.format("final S/N @ %.1f nm = %.3f", wavelength, finalSnr));

        final scala.Option<SignalToNoiseAt> sn = RecipeUtil.instance().signalToNoiseAt(wavelength, specS2N.getExpS2NSpectrum(), specS2N.getFinalS2NSpectrum());
        final AllExposures exp = AllExposures.single(new TotalExposure(exposureTime, numberExposures));
        return new SpectroscopyResult(p, instrument, IQcalc, specS2Narr, slit, throughput.throughput(), Option.empty(), sn, exp);
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

    public int getExposureTime() {
        return (int) exposureTime;
    }

    public int getNumberExposures() {
        return numberExposures;
    }

    public ReadMode getReadMode() {
        return readMode;
    }

}
