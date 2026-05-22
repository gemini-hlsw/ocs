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
    }

    public void calculate() {

        // 1. Calculate the maximum exposure time
        // 2. Calculate the total integration time required to achieve the requested S/N.
        // 3. Calculate the number of exposures, if more than one are required.
        // 4. Calculate the exposure time.

        super.calculate();  // perform an initial S/N calculation using the default values from the HTML form
        double snr = totalSNRatio();
        Log.fine(String.format("Total S/N = %.2f from %d x %.2f second exposures", snr, numberExposures, exposure_time));

        double peakFlux = PeakPixelFlux.calculate(instrument, _sdParameters, exposure_time, srcFrac, imageQuality, sed_integral, sky_integral);
        Log.fine(String.format("Peak Pixel Flux = %.0f e-", peakFlux));

        Log.fine(String.format("Max acceptable flux = %.0f e-", maxFlux));

        double timeToHalfMax = maxFlux / 2. / peakFlux * exposure_time;  // time to reach half of the maximum (our goal)
        Log.fine(String.format("timeToHalfMax = %.3f seconds", timeToHalfMax));
        Log.fine(String.format("minExpTime = %.3f seconds", instrument.getMinExposureTime()));

        if (timeToHalfMax < instrument.getMinExposureTime()) {
            Log.fine("Minimum exposure time = " + instrument.getMinExposureTime());
            throw new RuntimeException(String.format(
                    "This target is too bright for this configuration.\n" +
                            "The detector well is half filled in %.2f seconds.", timeToHalfMax));
        }

        // This should be instrument-specific.  GMOS: 1200, F2: , GNIRS:
        double maxExptime = Math.min(1200, timeToHalfMax);
        Log.fine(String.format("maxExptime = %.3f seconds", maxExptime));

        double desiredSNR = ((ImagingIntegrationTime) calcMethod).sigma();
        Log.fine(String.format("desiredSNR = %.2f", desiredSNR));

        double totalTime = exposure_time * numberExposures * (desiredSNR / snr) * (desiredSNR / snr);
        Log.fine(String.format("Required totalTime = %.5f seconds", totalTime));

        numberExposures = (int) Math.ceil(totalTime / maxExptime);
        Log.fine(String.format("numberExposures = %d", numberExposures));

        exposure_time = totalTime / numberExposures;
        if (exposure_time < instrument.getMinExposureTime()) {
            Log.fine("Increasing exposure time to the minimum allowed");
            exposure_time = instrument.getMinExposureTime();
        }

        // GMOS and F2 require integer exposure times, so round up:
        if (instrument.getName().matches("GMOS-[SN]|Flamingos_2")) {
            exposure_time = Math.ceil(exposure_time);
        } else {
            exposure_time = roundExposureTime(exposure_time);
        }
        Log.fine(String.format("Rounding exposureTime up to %.2f sec", exposure_time));

        // TODO: for instruments that can coadd - calculate the number of coadds

        super.calculate();
        Log.fine("totalSNRatio = " + totalSNRatio());
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

    @Override public double getExposureTime() { return exposure_time; }

}
