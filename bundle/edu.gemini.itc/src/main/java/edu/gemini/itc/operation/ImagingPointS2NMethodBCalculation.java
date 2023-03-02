package edu.gemini.itc.operation;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.shared.ImagingInt;
import edu.gemini.itc.shared.ObservationDetails;

public final class ImagingPointS2NMethodBCalculation extends ImagingS2NCalculation {

    private final double frac_with_source;
    private final double req_s2n;
    private int int_req_source_exposures;
    private double req_number_exposures;

    public ImagingPointS2NMethodBCalculation(final ObservationDetails obs,
                                             final Instrument instrument,
                                             final SourceFraction srcFrac,
                                             final double sed_integral,
                                             final double sky_integral) {
        super(obs, instrument, srcFrac, sed_integral, sky_integral);
        this.frac_with_source = obs.sourceFraction();
        this.exposure_time = obs.exposureTime();
        this.coadds = obs.calculationMethod().coaddsOrElse(1);
        this.read_noise = instrument.getReadNoise();
        this.pixel_size = instrument.getPixelSize();
        this.req_s2n = (obs.calculationMethod() instanceof ImagingInt) ? ((ImagingInt) obs.calculationMethod()).sigma() : 0.0;

    }

    public void calculate() {
        super.calculate();

        final double req_source_exposures = (req_s2n / signal) * (req_s2n / signal) *
                (signal + noiseFactor * sourceless_noise * sourceless_noise) / coadds;

        int_req_source_exposures =
                new Double(Math.ceil(req_source_exposures)).intValue();

        req_number_exposures =
                int_req_source_exposures / frac_with_source;

    }

    @Override public int numberSourceExposures() {
        return int_req_source_exposures;
    }

    public double reqNumberExposures() {
        return req_number_exposures;
    }

}

