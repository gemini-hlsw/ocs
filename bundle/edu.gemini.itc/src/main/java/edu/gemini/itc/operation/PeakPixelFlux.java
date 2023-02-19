package edu.gemini.itc.operation;

import edu.gemini.itc.base.DatFile;
import edu.gemini.itc.base.ITCConstants;
import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.SourceDefinition;
import java.util.logging.Logger;

public final class PeakPixelFlux {
    private static final Logger Log = Logger.getLogger(PeakPixelFlux.class.getName());
    private final double im_qual;
    private final double pixel_size;
    private final double exp_time;
    private final double summed_source;
    private final double summed_background;
    private final double dark_current;
    private final double[][] data;

    public static double calculate(final Instrument instrument,
                                   final SourceDefinition _sdParameters,
                                   final ObservationDetails _obsDetailParameters,
                                   final SourceFraction SFcalc,
                                   final double im_qual,
                                   final double sed_integral,
                                   final double sky_integral) {
        final PeakPixelFlux ppfc  = new PeakPixelFlux(im_qual, instrument.getPixelSize(), _obsDetailParameters.exposureTime(), sed_integral, sky_integral, instrument.getDarkCurrent());
        if (!_sdParameters.isUniform()) {
            return ppfc.getFluxInPeakPixel();
        } else {
            return ppfc.getFluxInPeakPixelUSB(SFcalc.getSourceFraction(), SFcalc.getNPix());
        }
    }

    public static double calculate(final Instrument instrument,
                                   final SourceDefinition _sdParameters,
                                   final double exposureTime,
                                   final SourceFraction SFcalc,
                                   final double im_qual,
                                   final double sed_integral,
                                   final double sky_integral) {

        final PeakPixelFlux ppfc = new PeakPixelFlux(im_qual, instrument.getPixelSize(), exposureTime, sed_integral, sky_integral, instrument.getDarkCurrent());

        if (!_sdParameters.isUniform()) {
            return ppfc.getFluxInPeakPixel();
        } else {
            return ppfc.getFluxInPeakPixelUSB(SFcalc.getSourceFraction(), SFcalc.getNPix());
        }
    }

    public static double calculateWithHalo(final Instrument instrument,
                                           final SourceDefinition  _sdParameters,
                                           final ObservationDetails _obsDetailParameters,
                                           final SourceFraction SFcalc,
                                           final double im_qual,
                                           final double orig_im_qual,
                                           final double halo_integral,
                                           final double sed_integral,
                                           final double sky_integral) {
        final PeakPixelFlux ppfc = new PeakPixelFlux(im_qual, instrument.getPixelSize(), _obsDetailParameters.exposureTime(), sed_integral, sky_integral, instrument.getDarkCurrent());
        if (!_sdParameters.isUniform()) {

            final double peak_pixel_count = ppfc.getFluxInPeakPixel();
            final PeakPixelFlux ppfc_halo = new PeakPixelFlux(orig_im_qual, instrument.getPixelSize(), _obsDetailParameters.exposureTime(), halo_integral, sky_integral, instrument.getDarkCurrent());
            return peak_pixel_count + ppfc_halo.getFluxInPeakPixel();

        } else  {
            return ppfc.getFluxInPeakPixelUSB(SFcalc.getSourceFraction(), SFcalc.getNPix());
        }
    }

    private PeakPixelFlux(final double im_qual,
                         final double pixel_size,
                         final double exp_time,
                         final double summed_source,
                         final double summed_background,
                         final double dark_current) {

        final String fileName = ITCConstants.CALC_LIB + ITCConstants.FLUX_IN_PEAK_PIXEL_FILENAME + ITCConstants.DATA_SUFFIX;
        this.im_qual = im_qual;
        this.pixel_size = pixel_size;
        this.exp_time = exp_time;
        this.summed_source = summed_source;
        this.summed_background = summed_background;
        this.dark_current = dark_current;
        this.data = DatFile.arrays().apply(fileName);
    }

    private double getFluxInPeakPixel() {
        final double im_qual_in_pix = im_qual / pixel_size;
        final double frac_in_peak = getY(im_qual_in_pix);
        final double source_fraction_in_peak;
        if (frac_in_peak > 1000) {
            source_fraction_in_peak = 0;
        } else {
            source_fraction_in_peak = frac_in_peak;
        }
        final double source_in_peak = summed_source * source_fraction_in_peak * exp_time;
        final double background_in_peak = summed_background * exp_time * pixel_size * pixel_size;
        final double total_in_peak = source_in_peak + background_in_peak + dark_current * exp_time;
        Log.fine(String.format("Flux in peak = %.0f e-", total_in_peak));
        return total_in_peak;
    }

    private double getFluxInPeakPixelUSB(final double source_fraction, final double enclosedPixels) {
        final double source_in_peak = summed_source * source_fraction * exp_time / enclosedPixels;
        final double background_in_peak = summed_background * exp_time * pixel_size * pixel_size;
        return source_in_peak + background_in_peak + dark_current * exp_time;
    }

    /**
     * @return y value at specified x using linear interpolation.
     * Silently returns zero if x is out of spectrum range.
     */
    private double getY(final double x) {
        if (x < getStart() || x > getEnd()) return 0;
        final int low_index = getLowerIndex(x);
        final int high_index = low_index + 1;
        final double y1 = getY(low_index);
        final double y2 = getY(high_index);
        final double x1 = getX(low_index);
        final double x2 = getX(high_index);
        final double slope = (y2 - y1) / (x2 - x1);
        return (slope * (x - x1) + y1);
    }

    /**
     * @return starting x value
     */
    private double getStart() {
        return data[0][0];
    }

    /**
     * @return ending x value
     */
    private double getEnd() {
        return data[0][data[0].length - 1];
    }

    /**
     * Returns x value of specified data point.
     */
    private double getX(final int index) {
        return data[0][index];
    }

    /**
     * Returns y value of specified data point.
     */
    private double getY(final int index) {
        return data[1][index];
    }


    /**
     * Returns the index of the data point with largest x value less than x
     */
    private int getLowerIndex(final double x) {
        // In a general spectrum we don't have an idea which bin a particular
        // x value is in.  The only solution is to search for it.
        // Could just walk through, but do a binary search for it.
        int low_index = 0;
        int high_index = data[0].length;
        if (high_index - low_index <= 1) return low_index;
        while (high_index - low_index > 1) {
            int index = (high_index + low_index) / 2;
            if (getX(index) < x)
                low_index = index;
            else
                high_index = index;
        }
        return low_index;
    }
}
