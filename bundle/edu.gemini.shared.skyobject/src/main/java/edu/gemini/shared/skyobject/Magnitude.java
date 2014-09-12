//
// $
//

package edu.gemini.shared.skyobject;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Celestial object brightness information.  Magnitudes are relative to
 * particular wavelengths of light, and optionally are associated with an error
 * in the measurement.
 */
public final class Magnitude implements Comparable, Serializable {

    /**
     * Common wavelength bands.
     */
    public enum Band {
        U( 365, "ultraviolet"),
        B( 445, "blue"),
        V( 551, "visual"),
        UC(610, "UCAC"), // unknown FWHM
        R( 658, "red"),
        I( 806, "infrared"),
        Y(1020),
        J(1220),
        H(1630),
        K(2190),
        L(3450),
        M(4750),
        N(10000),
        Q(16000),
// REL-549: Remove "AB" and "Jy" Band enum values from the model since they are actually "system" options.
//        AB(None.INTEGER, None.STRING),
//        Jy(None.INTEGER, None.STRING),
        ;

        /**
         * A Comparator of magnitude bands based upon the name of the
         * band.  The default ordering is in terms of increasing wavelength.
         * This comparator can be used to sort passbands based upon an
         * alphabetical sorting.
         */
        public static final Comparator<Band> NAME_COMPARATOR =
            new Comparator<Band>() {
                @Override public int compare(Band b1, Band b2) {
                    return b1.name().compareTo(b2.name());
                }
            };

        /**
         * A Comparator of magnitude bands based upon the associated
         * wavelength.
         */
        public static final Comparator<Band> WAVELENGTH_COMPARATOR =
            new Comparator<Band>() {
                @Override public int compare(Band b1, Band b2) {
                    int b1w = b1.wavelengthMidPoint.getOrElse(Integer.MAX_VALUE);
                    int b2w = b2.wavelengthMidPoint.getOrElse(Integer.MAX_VALUE);
                    int res = b1w - b2w;
                    return res==0 ? b1.ordinal() - b2.ordinal() : res;
                }
            };

        private final Option<Integer> wavelengthMidPoint;    // nm
        private final Option<String> description;

        Band(Option<Integer> mid, Option<String> desc) {
            this.wavelengthMidPoint = mid;
            this.description        = desc;
        }

        Band(int mid) {
            this(mid, null);
        }

        Band(int mid, String desc) {
            this.wavelengthMidPoint = new Some<Integer>(mid);
            this.description = (desc == null) ? None.STRING : new Some<String>(desc);
        }

        public Option<Integer> getWavelengthMidPoint() {
            return wavelengthMidPoint;
        }

        public Option<String> getDescription() {
            return description;
        }
    }

    /**
     * REL-549: Magnitude information for targets and guide stars in OT must be stored in value, bandpass, system triples.
     */
    public enum System {
        Vega,
        AB,
        Jy,
        ;

        public static final System DEFAULT = Vega;
    }


    /**
     * Magnitudes with this brightness are undefined.
     */
    public static final double UNDEFINED_MAG = -99.0;  // Yikes :-(

    private final Band band;
    private final double brightness;
    private final Option<Double> error;
    private final System system;

    /**
     * Creates with the magnitude band and brightness, leaving the error
     * unset.
     *
     * @param band bandpass associated with the brightness
     * @param brightness absolute brightness
     */
    public Magnitude(Band band, double brightness) {
        //noinspection unchecked
        this(band, brightness, None.INSTANCE, System.DEFAULT);
    }

    /**
     * Creates with the magnitude band, brightness and error.
     *
     * @param band bandpass associated with the brightness
     * @param brightness absolute brightness
     */
    public Magnitude(Band band, double brightness, System system) {
        this(band, brightness, None.<Double>instance(), system);
    }

    /**
     * Creates with the magnitude band, brightness and error.
     *
     * @param band bandpass associated with the brightness
     * @param brightness absolute brightness
     * @param error error in measurement
     */
    public Magnitude(Band band, double brightness, double error) {
        this(band, brightness, new Some<Double>(error), System.DEFAULT);
    }

    /**
     * Creates with the magnitude band, brightness and error.
     *
     * @param band bandpass associated with the brightness
     * @param brightness absolute brightness
     * @param error error in measurement
     * @param system mag system
     */
    public Magnitude(Band band, double brightness, double error, System system) {
        this(band, brightness, new Some<Double>(error), system);
    }

    /**
     * Creates with the magnitude band, brightness, and an optional error.
     *
     * @param band bandpass associated with the brightness
     * @param brightness absolute brightness
     * @param error optional error in measurement
     */
    public Magnitude(Band band, double brightness, Option<Double> error) {
        this(band, brightness, error, System.DEFAULT);
    }

    /**
     * Creates with the magnitude band, brightness, and an optional error.
     *
     * @param band bandpass associated with the brightness
     * @param brightness absolute brightness
     * @param error optional error in measurement
     * @param system mag system
     */
    public Magnitude(Band band, double brightness, Option<Double> error, System system) {
        if (band == null) throw new IllegalArgumentException("band is null");
        if (error == null) throw new IllegalArgumentException("error is null");
        if (system == null) throw new IllegalArgumentException("system is null");

        this.band       = band;
        this.brightness = brightness;
        this.error      = error;
        this.system     = system;
    }

    public Band getBand()            { return band; }
    public double getBrightness()    { return brightness; }
    public System getSystem()        { return system; }
    public Option<Double> getError() { return error; }

    /**
     * Gets a new Magnitude object that is identical to this one, but with
     * its brightness adjusted by the given amount.
     */
    public Magnitude add(double brightness) {
        return new Magnitude(band, this.brightness + brightness, error);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Magnitude magnitude = (Magnitude) o;

        if (Double.compare(magnitude.brightness, brightness) != 0) return false;
        if (band != magnitude.band) return false;
        if (error != null ? !error.equals(magnitude.error) : magnitude.error != null) return false;
        return system == magnitude.system;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = band != null ? band.hashCode() : 0;
        temp = brightness != +0.0d ? Double.doubleToLongBits(brightness) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (error != null ? error.hashCode() : 0);
        result = 31 * result + system.hashCode();
        return result;
    }

    /**
     * Compares two magnitude objects by system, band, brightness and error (in that
     * order).
     *
     * @param o other magnitude object
     */
    @Override
    public int compareTo(Object o) {
        Magnitude that = (Magnitude) o;

        int res = system.compareTo(that.system);
        if (res != 0) return res;

        res = band.compareTo(that.band);
        if (res != 0) return res;

        res = Double.compare(brightness, that.brightness);
        if (res != 0) return res;

        if (error.isEmpty()) {
            return that.error.isEmpty() ? 0 : -1;
        } else if (that.error.isEmpty()){
            return 1;
        } else {
            return error.getValue().compareTo(that.error.getValue());
        }
    }

    @Override
    public String toString() {
        String errorStr = "(---)";
        if (!error.isEmpty()) {
            errorStr = String.format("(%.2f)", error.getValue());
        }
        return String.format("%s %5.2f %s %s", band, brightness, system, errorStr);
    }
}
