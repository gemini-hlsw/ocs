package edu.gemini.itc.operation;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.shared.CalculationMethod;
import edu.gemini.itc.shared.ImagingExp;
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
        Log.fine(String.format("timeToHalfMax = %.2f seconds", timeToHalfMax));

        if (timeToHalfMax < 1.0) throw new RuntimeException(String.format(
                "This target is too bright for this configuration.\n" +
                "The detector well is half filled in %.2f seconds.", timeToHalfMax));

        int maxExptime = Math.min(1200, (int) timeToHalfMax);  // 1200s is the (GMOS) maximum due to cosmic rays
        Log.fine(String.format("maxExptime = %d seconds", maxExptime));

        double desiredSNR = ((ImagingExp) calcMethod).sigma();
        Log.fine(String.format("desiredSNR = %.2f", desiredSNR));

        double totalTime = exposure_time * numberExposures * (desiredSNR / snr) * (desiredSNR / snr);
        Log.fine(String.format("totalTime = %.2f", totalTime));

        numberExposures = (int) Math.ceil(totalTime / maxExptime);
        Log.fine(String.format("numberExposures = %d", numberExposures));

        exposure_time = Math.ceil(totalTime / numberExposures);
        Log.fine(String.format("exposureTime = %.2f", exposure_time));

        // TODO: for instruments that can coadd - calculate the number of coadds

        super.calculate();
        Log.fine("totalSNRatio = " + totalSNRatio());
    }

    @Override public int numberSourceExposures() { return numberExposures; }

    @Override public double getExposureTime() { return exposure_time; }

}
