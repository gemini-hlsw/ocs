package edu.gemini.itc.operation;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.shared.ImagingS2N;
import edu.gemini.itc.shared.ObservationDetails;

public final class ImagingS2NMethodACalculation extends ImagingS2NCalculation {

    private final int number_exposures;
    private final double frac_with_source;
    private final int coadds;

    public ImagingS2NMethodACalculation(final ObservationDetails obs,
                                        final Instrument instrument,
                                        final SourceFraction srcFrac,
                                        final double sed_integral,
                                        final double sky_integral) {
        super(obs, instrument, srcFrac, sed_integral, sky_integral);
        this.read_noise         = instrument.getReadNoise();
        this.pixel_size         = instrument.getPixelSize();

        // Currently SpectroscopySN is the only supported calculation method for spectroscopy.
        if (!(obs.calculationMethod() instanceof ImagingS2N)) throw new Error("Unsupported calculation method");
        this.number_exposures   = ((ImagingS2N) obs.calculationMethod()).exposures();
        this.frac_with_source   = obs.calculationMethod().sourceFraction();
        this.exposure_time      = obs.calculationMethod().exposureTime();
        this.coadds             = obs.calculationMethod().coaddsOrElse(1);
    }

    public void calculate() {
        super.calculate();

        final double epsilon = 0.2;
        final double number_source_exposures = numberSourceExposures();
        final int iNumExposures = (int) (number_source_exposures + 0.5);
        final double diff = number_source_exposures - iNumExposures;
        if (Math.abs(diff) > epsilon) {
            throw new IllegalArgumentException(
                    "Fraction with source value produces non-integral number of source exposures with source (" +
                            number_source_exposures + " vs. " + iNumExposures + ").");
        }

    }

    @Override public double numberSourceExposures() {
        return number_exposures * frac_with_source * coadds;
    }

}

