//
// $
//

package edu.gemini.shared.skyobject.coords;

import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.DDMMSS;
import edu.gemini.skycalc.HHMMSS;

import java.io.Serializable;

/**
 * Standard RA (HMS) and declination (degrees) coordinate system.
 */
public final class HmsDegCoordinates implements SkyCoordinates {
    /**
     * Reference time for which the coordinates are valid.
     */
    public static final class Epoch implements Comparable, Serializable {

        /**
         * Common units in which epochs are expressed.
         */
        public enum Type {
            JULIAN() {
                public String displayName() { return "J"; }
            },
            ;

            /**
             * Returns the common abbreviation for the type.
             */
            public abstract String displayName();
        }

        public static final Epoch J2000 = new Epoch(Type.JULIAN,    2000);

        private final Type type;
        private final double year;

        public Epoch(Type type, double year) {
            if (type == null) throw new IllegalArgumentException("type is null");
            this.type = type;
            this.year = year;
        }

        public Type getType() { return type; }
        public double getYear() { return year; }

        @Override
        public int compareTo(Object o) {
            Epoch that = (Epoch) o;
            int res =  type.compareTo(that.type);
            if (res != 0) return res;
            if (year < that.year) return -1;
            if (year > that.year) return  1;
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Epoch that = (Epoch) o;
            if (year != that.year) return false;
            return type == that.type;
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();

            long bits = Double.doubleToLongBits(year);
            result = 31 * result + (int)(bits ^ (bits >>> 32));

            return result;
        }

        @Override
        public String toString() { return type.displayName() + year; }
    }

    /**
     * A mutable constructor of HmsDegCoordinates.  Builder calls are meant to
     * be chained together to simplify client code.  For example,
     *
     * <code>
     * HmsDegCoordinates c = (new Builder(ra, dec).epoch(Epoch.J2000).build();
     * </code>
     *
     * Here the <code>epoch</code> method updates the epoch and returns a
     * reference to <code>this</code> Builder.
     */
    public static final class Builder {
        private Angle ra;
        private Angle dec;
        private Epoch epoch = Epoch.J2000;
        private Angle pmRa  = new Angle(0, Angle.Unit.MILLIARCSECS);
        private Angle pmDec = new Angle(0, Angle.Unit.MILLIARCSECS);

        /**
         * Constructs with the two required pieces of information, the RA and
         * declination.  The epoch defaults to {@link Epoch#J2000} unless
         * later set via the {@link #epoch} method.
         *
         * @param ra right ascension
         * @param dec declination
         */
        public Builder(Angle ra, Angle dec) {
            if (ra  == null) throw new IllegalArgumentException("ra is null");
            if (dec == null) throw new IllegalArgumentException("dec is null");
            this.ra  = ra;
            this.dec = dec;
        }

        /**
         * Sets the right ascension as an {@link Angle}.
         *
         * @param ra new right ascension
         *
         * @return <code>this</code> Builder
         */
        public Builder ra(Angle ra) {
            if (ra == null) throw new IllegalArgumentException("ra is null");
            this.ra = ra;
            return this;
        }

        /**
         * Sets the declination as an {@link Angle}.
         *
         * @param dec new declination
         *
         * @return <code>this</code> Builder
         */
        public Builder dec(Angle dec) {
            if (dec == null) throw new IllegalArgumentException("dec is null");
            this.dec = dec;
            return this;
        }

        /**
         * Sets the epoch.
         *
         * @param epoch new epoch
         *
         * @return <code>this</code> Builder
         */
        public Builder epoch(Epoch epoch) {
            if (epoch == null) throw new IllegalArgumentException("epoch is null");
            this.epoch = epoch;
            return this;
        }

        /**
         * Sets the proper motion in RA, which is expressed as the angular
         * change in position understood to be one year.
         *
         * @param pmRa new proper motion in RA
         *
         * @return <code>this</code> Builder
         */
        public Builder pmRa(Angle pmRa) {
            if (pmRa == null) throw new IllegalArgumentException("pmRa is null");
            this.pmRa = pmRa;
            return this;
        }

        /**
         * Sets the proper motion in declination, which is expressed as the
         * angular change in position understood to be one year.
         *
         * @param pmDec new proper motion in declination
         *
         * @return <code>this</code> Builder
         */
        public Builder pmDec(Angle pmDec) {
            if (pmDec == null) throw new IllegalArgumentException("pmDec is null");
            this.pmDec = pmDec;
            return this;
        }

        /**
         * Creates the HmsDegCoordinates that has been configured in this
         * builder.
         *
         * @return new HmsDegCoordinates using the current values set in this
         * builder
         */
        public HmsDegCoordinates build() {
            return new HmsDegCoordinates(this);
        }
    }

    private final Angle ra;
    private final Angle dec;
    private final Epoch epoch;
    private final Angle pmRa;
    private final Angle pmDec;

    private HmsDegCoordinates(Builder b) {
        ra    = b.ra;
        dec   = b.dec;
        epoch = b.epoch;
        pmRa  = b.pmRa;
        pmDec = b.pmDec;
    }

    /**
     * Returns a reference to <code>this</code>, since there is nothing to
     * convert.
     *
     * @param date the time at which the coordinates will be valid
     *
     * @return <code>this</code>
     */
    @Override
    public HmsDegCoordinates toHmsDeg(long date) { return this; }

    /**
     * Gets the right ascension of this target.
     *
     * @return right ascension
     */
    public Angle getRa() { return ra; }

    /**
     * Gets the declination of this target.
     *
     * @return declination
     */
    public Angle getDec() { return dec; }

    /**
     * The coordinate's reference time.
     *
     * @return epoch at which the coordinates are valid
     */
    public Epoch getEpoch() { return epoch; }

    /**
     * The coordinate's proper motion in RA.
     *
     * @return proper motion in RA, expressed as an angular distance understood
     * to be the change over one year of time
     */
    public Angle getPmRa() { return pmRa; }

    /**
     * The coordinate's proper motion in declination.
     *
     * @return proper motion in declination, expressed as an angular distance
     * understood to be the change over one year of time
     */
    public Angle getPmDec() { return pmDec; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HmsDegCoordinates that = (HmsDegCoordinates) o;

        if (!ra.equals(that.ra)) return false;
        if (!dec.equals(that.dec)) return false;
        if (!epoch.equals(that.epoch)) return false;
        if (!pmRa.equals(that.pmRa)) return false;
        return pmDec.equals(that.pmDec);
    }

    @Override
    public int hashCode() {
        int result = ra.hashCode();
        result = 31 * result + dec.hashCode();
        result = 31 * result + epoch.hashCode();
        result = 31 * result + pmRa.hashCode();
        result = 31 * result + pmDec.hashCode();
        return result;
    }

//    @Override
//    public int compareTo(Object o) {
//        HmsDegCoordinates that = (HmsDegCoordinates) o;
//
//        int res = ra.compareToAngle(that.ra);
//        if (res != 0) return res;
//
//        res = dec.compareToAngle(that.dec);
//        if (res != 0) return res;
//
//        return epoch.compareTo(that.epoch);
//    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("HmsDegCoordinates");
        sb.append("{ra=").append(HHMMSS.valStr(ra.toDegrees().getMagnitude()));
        sb.append(", dec=").append(DDMMSS.valStr(dec.toDegrees().getMagnitude()));
        sb.append(", epoch=").append(epoch);
        sb.append(", pmRa=").append(pmRa.toMilliarcsecs().getMagnitude()).append("mas/yr");
        sb.append(", pmDec=").append(pmDec.toMilliarcsecs().getMagnitude()).append("mas/yr");
        sb.append('}');
        return sb.toString();
    }

    /**
     * Creates a new {@link Builder} configured with the values of this
     * {@link HmsDegCoordinates} object.
     *
     * @return a new mutable {@link Builder} initialized with the values from
     * this HmsDegCoordinates object.
     */
    public Builder builder() {
        return new Builder(ra, dec).epoch(epoch).pmRa(pmRa).pmDec(pmDec);
    }
}
