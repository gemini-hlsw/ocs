package edu.gemini.itc.operation;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.shared.ObservationDetails;

public final class ImagingPointS2NMethodBCalculation extends ImagingS2NCalculation {

    int number_exposures, int_req_source_exposures;
    double frac_with_source, req_s2n, req_source_exposures, req_number_exposures,
            effective_s2n;

    public ImagingPointS2NMethodBCalculation(final ObservationDetails obs,
                                             final Instrument instrument,
                                             final SourceFraction srcFrac,
                                             final double sed_integral,
                                             final double sky_integral) {
        super(obs, instrument, srcFrac, sed_integral, sky_integral);
        this.number_exposures = obs.getNumExposures();
        this.frac_with_source = obs.getSourceFraction();
        this.exposure_time = obs.getExposureTime();
        this.read_noise = instrument.getReadNoise();
        this.pixel_size = instrument.getPixelSize();
        this.req_s2n = obs.getSNRatio();

    }

    public void calculate() {
        super.calculate();
        req_source_exposures = (req_s2n / signal) * (req_s2n / signal) *
                (signal + noiseFactor * sourceless_noise * sourceless_noise);

        int_req_source_exposures =
                new Double(Math.ceil(req_source_exposures)).intValue();

        req_number_exposures =
                int_req_source_exposures / frac_with_source;

        effective_s2n =
                (Math.sqrt(int_req_source_exposures) * signal) /
                        Math.sqrt(signal + noiseFactor * sourceless_noise * sourceless_noise);


    }

    public double effectiveS2N() {
        return effective_s2n;
    }

    public double reqNumberExposures() {
        return req_number_exposures;
    }

}

