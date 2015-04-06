package edu.gemini.itc.operation;

import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.Instrument;

public final class ImagingS2NMethodACalculation extends ImagingS2NCalculation {

    private final int number_exposures;
    private final double frac_with_source;
    private double exp_s2n, final_s2n;

    public ImagingS2NMethodACalculation(final ObservationDetails obs,
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
    }

    public void calculate() {
        super.calculate();

        double epsilon = 0.2;
        double number_source_exposures = number_exposures * frac_with_source;
        int iNumExposures = (int) (number_source_exposures + 0.5);
        double diff = number_source_exposures - iNumExposures;
        if (Math.abs(diff) > epsilon) {
            throw new IllegalArgumentException(
                    "Fraction with source value produces non-integral number of source exposures with source (" +
                            number_source_exposures + " vs. " + iNumExposures + ").");
        }

        exp_s2n = signal / noise;


        final_s2n = Math.sqrt(number_source_exposures) * signal /
                Math.sqrt(signal + noiseFactor * sourceless_noise *
                        sourceless_noise);

    }

    public double singleSNRatio() {
        return exp_s2n;
    }

    public double totalSNRatio() {
        return final_s2n;
    }

    public String getTextResult(FormatStringWriter device) {
        StringBuffer sb = new StringBuffer(super.getTextResult(device));
        sb.append("Intermediate S/N for one exposure = " +
                device.toString(exp_s2n) + "\n\n");
        sb.append("S/N for the whole observation = "
                + device.toString(final_s2n) +
                " (including sky subtraction)\n\n");

        sb.append("Requested total integration time = " +
                device.toString(exposure_time * number_exposures) +
                " secs, of which " + device.toString(exposure_time *
                number_exposures *
                frac_with_source) +
                " secs is on source.\n");
        return sb.toString();
    }


}

