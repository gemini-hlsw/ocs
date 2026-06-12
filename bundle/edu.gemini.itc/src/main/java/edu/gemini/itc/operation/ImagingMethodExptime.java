package edu.gemini.itc.operation;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.shared.CalculationMethod;
import edu.gemini.itc.shared.ImagingIntegrationTime;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.SourceDefinition;
import java.util.logging.Logger;

public final class ImagingMethodExptime extends ImagingS2NCalculation {
    private final CalculationMethod calcMethod;
    private final Instrument instrument;
    private final SourceDefinition _sdParameters;
    private final SourceFraction srcFrac;
    private final double maxFlux;
    private final double imageQuality;
    private int numberExposures;
    private final double minExpTime;
    private static final Logger Log = Logger.getLogger(ImagingMethodExptime.class.getName());

    public ImagingMethodExptime(ObservationDetails obs,
                                final Instrument instrument,
                                final SourceFraction srcFrac,
                                final double sed_integral,
                                final double sky_integral,
                                final SourceDefinition _sdParameters,
                                final double imageQuality) {
        super(obs, instrument, srcFrac, sed_integral, sky_integral);

        this.instrument = instrument;
        this.calcMethod = obs.calculationMethod();
        this._sdParameters = _sdParameters;
        this.srcFrac = srcFrac;
        this.numberExposures = 1;
        this.exposure_time = obs.exposureTime();  // 60s in ITCgmos.html
        this.coadds = 1;   // obs.calculationMethod().coaddsOrElse(1);
        this.read_noise = instrument.getReadNoise();
        this.pixel_size = instrument.getPixelSize();
        this.maxFlux = instrument.maxFlux();
        this.imageQuality = imageQuality;
        this.minExpTime = instrument.getMinExposureTime();
    }

    // Constructor using a read mode different from specified in the web form
    public ImagingMethodExptime(ObservationDetails obs,
                                final Instrument instrument,
                                final SourceFraction srcFrac,
                                final double sed_integral,
                                final double sky_integral,
                                final SourceDefinition _sdParameters,
                                final double imageQuality,
                                final double read_noise,
                                final double minExpTime) {
        super(obs, instrument, srcFrac, sed_integral, sky_integral);

        this.instrument = instrument;
        this.calcMethod = obs.calculationMethod();
        this._sdParameters = _sdParameters;
        this.srcFrac = srcFrac;
        this.numberExposures = 1;
        this.exposure_time = 10. * minExpTime;
        this.coadds = 1;
        this.read_noise = read_noise;
        this.pixel_size = instrument.getPixelSize();
        this.maxFlux = instrument.maxFlux();
        this.imageQuality = imageQuality;
        this.minExpTime = minExpTime;
    }

    public void calculate() {

        super.calculate();  // perform an initial S/N calculation using the default values from the HTML form

        Log.fine(String.format("Initial S/N = %.2f from %d x %.2f second exposures", totalSNRatio(), numberExposures, exposure_time));

        double peakFlux = PeakPixelFlux.calculate(instrument, _sdParameters, exposure_time, srcFrac, imageQuality, sed_integral, sky_integral);
        Log.fine(String.format("Peak Pixel Flux = %.0f e-", peakFlux));

        Log.fine(String.format("Max acceptable flux = %.0f e-", maxFlux));

        double timeToHalfMax = maxFlux / 2. / peakFlux * exposure_time;  // time to reach half of the maximum (our goal)
        Log.fine(String.format("timeToHalfMax = %.3f seconds", timeToHalfMax));
        Log.fine(String.format("minExpTime = %.3f seconds", minExpTime));

        if (timeToHalfMax < minExpTime) {
            throw new RuntimeException(String.format("This target is too bright for this configuration.\n" +
                    "The detector well is half filled in %.2f seconds.", timeToHalfMax));
        }

        double maxExposureTime;
        switch (instrument.getName()) {
            case "Flamingos2": maxExposureTime = 300.0; break;
            case "GMOS-N":     maxExposureTime = 600.0; break;
            case "GMOS-S":     maxExposureTime = 600.0; break;
            case "GNIRS":      maxExposureTime = 300.0; break;
            default:           maxExposureTime = 300.0; break;
        }
        double maxExptime = Math.min(maxExposureTime, timeToHalfMax);
        Log.fine(String.format("maxExptime = %.3f seconds", maxExptime));

        double desiredSNR = ((ImagingIntegrationTime) calcMethod).sigma();
        Log.fine(String.format("desiredSNR = %.2f", desiredSNR));

        // Calculate rates from the initial super.calculate() results
        final double signalPerSec     = var_source / exposure_time;
        final double backgroundPerSec = var_background / exposure_time;
        final double darkPerSec       = var_dark / exposure_time;

        // Iterate to find the number of exposures and exposure time that will give the requested S/N.
        // exposure_time is calculated exactly then numberExposures is updated from the resulting total time.
        numberExposures = 1;
        final int MAX_ITERATIONS = 10;
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            Log.fine("-------------------------------- iter " + iter + " --------------------------------");
            exposure_time = ExposureTimeCalculator.calculate(signalPerSec, backgroundPerSec, darkPerSec,
                    var_readout, skyAper, desiredSNR / Math.sqrt(numberExposures));
            Log.fine(String.format("Iteration %d: %d x %.3f sec", iter, numberExposures, exposure_time));
            int n = (int) Math.ceil(exposure_time * numberExposures / maxExptime);
            if (n == numberExposures) {
                Log.fine("------------------------------------------------------------------------");
                break;
            }
            numberExposures = n;
        }
        Log.fine(String.format("numberExposures = %d", numberExposures));

        if (numberExposures > 10000) {
            throw new RuntimeException(String.format("This target is too faint for this configuration.\n" +
                    "%d exposures would be required to achieve the requested S/N.", numberExposures));
        }

        if (exposure_time < minExpTime) {
            Log.fine("Increasing exposure time to the minimum allowed");
            exposure_time = minExpTime;
        }

        // GMOS and F2 require integer exposure times, so round up:
        if (instrument.getName().matches("GMOS-[SN]|Flamingos_2")) {
            exposure_time = Math.ceil(exposure_time);
        } else {
            exposure_time = roundExposureTime(exposure_time);
        }
        Log.fine(String.format("Rounding exposureTime up to %.3f sec", exposure_time));

        // TODO: for instruments that can coadd - calculate the number of coadds

        super.calculate();
        Log.fine(String.format("Final: S/N = %.4f from %d x %.3f sec", totalSNRatio(), numberExposures, exposure_time));
    }

    /**
     *  Round exposure times up to nice values.
    */
    private static final double[][] ROUNDING_RULES = {
        {600, 30},
        {100, 10},
        {10,   1},
        {1,    0.1},
        {0.1,  0.01},
        {0.01, 0.001}
    };

    private static double roundExposureTime(double exposureTime) {
        for (double[] rule : ROUNDING_RULES) {
            if (exposureTime > rule[0]) {
                return Math.ceil(exposureTime / rule[1]) * rule[1];
            }
        }
        return exposureTime;
    }

    @Override public int numberSourceExposures() { return numberExposures; }

}
