package edu.gemini.shared.skyobject;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.MagnitudeSystem;
import edu.gemini.spModel.core.Wavelength;
import squants.space.LengthConversions;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Celestial object brightness information.  Magnitudes are relative to
 * particular wavelengths of light, and optionally are associated with an error
 * in the measurement.
 */
public final class Magnitude implements Comparable<Magnitude>, Serializable {

    private static final MagnitudeSystem AB   = MagnitudeSystem.AB$.MODULE$;
    private static final MagnitudeSystem Vega = MagnitudeSystem.Vega$.MODULE$;

    /**
     * Common wavelength bands.
     */
    public enum Band {

        // OCSADV-203
        u(AB, 350, "UV"),
        g(AB, 475, "green"),
        r(AB, 630, "red"),
        i(AB, 780, "far red"),
        z(AB, 925, "near-infrared"),

        U(Vega,  365, "ultraviolet"),
        B(Vega,  445, "blue"),
        V(Vega,  551, "visual"),
        UC(Vega, 610, "UCAC"), // unknown FWHM
        R(Vega,  658, "red"),
        I(Vega,  806, "infrared"),
        Y(Vega, 1020),
        J(Vega, 1220),
        H(Vega, 1630),
        K(Vega, 2190),
        L(Vega, 3450),
        M(Vega, 4750),
        N(Vega, 10000),
        Q(Vega, 16000),
        AP(Vega, None.INTEGER, new Some<>("apparent"))
        ;

        /**
         * A Comparator of magnitude bands based upon the associated
         * wavelength.
         */
        public static final Comparator<Band> WAVELENGTH_COMPARATOR =
            new Comparator<Band>() {
                @Override public int compare(Band b1, Band b2) {
                    if (b1.wavelengthMidPoint.isDefined() && b2.wavelengthMidPoint.isDefined()) {
                        double w1 = b1.getWavelengthMidPoint().getValue().toNanometers();
                        double w2 = b2.getWavelengthMidPoint().getValue().toNanometers();
                        return (int) (w1 - w2);
                    } else {
                        return b1.ordinal() - b2.ordinal();
                    }
                }
            };

        public final MagnitudeSystem defaultSystem;
        private final Option<Wavelength> wavelengthMidPoint;
        private final Option<String> description;

        Band(MagnitudeSystem sys, Option<Integer> mid, Option<String> desc) {
            this.defaultSystem      = sys;
            this.wavelengthMidPoint = mid.map(w -> new Wavelength(LengthConversions.nanometer().$times(w)));
            this.description        = desc;
        }

        Band(MagnitudeSystem sys, int mid) {
            this(sys, mid, null);
        }

        Band(MagnitudeSystem sys, int mid, String desc) {
            this.defaultSystem      = sys;
            this.wavelengthMidPoint = new Some<>(new Wavelength(LengthConversions.nanometer().$times(mid)));
            this.description = (desc == null) ? None.STRING : new Some<>(desc);
        }

        public Option<String> getDescription() {
            return description;
        }

        public Option<Wavelength> getWavelengthMidPoint() {
            return wavelengthMidPoint;
        }

    }

    /**
     * Magnitudes with this brightness are undefined.
     */
    public static final double UNDEFINED_MAG = -99.0;  // Yikes :-(

    private final Band band;
    private final double brightness;
    private final Option<Double> error;
    private final MagnitudeSystem system;

    /**
     * Creates with the magnitude band and brightness, leaving the error
     * unset.
     *
     * @param band bandpass associated with the brightness
     * @param brightness absolute brightness
     */
    public Magnitude(Band band, double brightness) {
        //noinspection unchecked
        this(band, brightness, None.INSTANCE, band.defaultSystem);
    }

    /**
     * Creates with the magnitude band, brightness and error.
     *
     * @param band bandpass associated with the brightness
     * @param brightness absolute brightness
     */
    public Magnitude(Band band, double brightness, MagnitudeSystem system) {
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
        this(band, brightness, new Some<>(error), band.defaultSystem);
    }

    /**
     * Creates with the magnitude band, brightness and error.
     *
     * @param band bandpass associated with the brightness
     * @param brightness absolute brightness
     * @param error error in measurement
     * @param system mag system
     */
    public Magnitude(Band band, double brightness, double error, MagnitudeSystem system) {
        this(band, brightness, new Some<>(error), system);
    }

    /**
     * Creates with the magnitude band, brightness, and an optional error.
     *
     * @param band bandpass associated with the brightness
     * @param brightness absolute brightness
     * @param error optional error in measurement
     */
    public Magnitude(Band band, double brightness, Option<Double> error) {
        this(band, brightness, error, band.defaultSystem);
    }

    /**
     * Creates with the magnitude band, brightness, and an optional error.
     *
     * @param band bandpass associated with the brightness
     * @param brightness absolute brightness
     * @param error optional error in measurement
     * @param system mag system
     */
    public Magnitude(Band band, double brightness, Option<Double> error, MagnitudeSystem system) {
        if (band == null) throw new IllegalArgumentException("band is null");
        if (error == null) throw new IllegalArgumentException("error is null");
        if (system == null) throw new IllegalArgumentException("system is null");

        this.band       = band;
        this.brightness = brightness;
        this.error      = error;
        this.system     = system;
    }

    public Band getBand()               { return band; }
    public double getBrightness()       { return brightness; }
    public MagnitudeSystem getSystem()  { return system; }
    public Option<Double> getError()    { return error; }

    /**
     * Gets a new Magnitude object that is identical to this one, but with
     * its brightness adjusted by the given amount.
     */
    public Magnitude add(double brightness) {
        return new Magnitude(band, this.brightness + brightness, error, system);
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
     * @param that other magnitude object
     */
    @Override
    public int compareTo(Magnitude that) {
        int res = MagnitudeSystem.all().indexOf(that.system) - MagnitudeSystem.all().indexOf(this.system);
        if (res != 0) return res;

        res = band.compareTo(that.band);
        if (res != 0) return res;

        res = Double.compare(brightness, that.brightness);
        if (res != 0) return res;

        if (error.isEmpty()) {
            return that.error.isEmpty() ? 0 : -1;
        } else if (that.error.isEmpty()) {
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
