package edu.gemini.catalog.api;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.util.immutable.*;

import java.io.Serializable;

/**
 * Describes limits for magnitude values.
 * See OT-19.
 */
@Deprecated
public final class MagnitudeLimits implements Serializable {
    public static interface Limit extends Serializable {
        double getBrightness();
        boolean contains(Magnitude mag);
    }

    protected abstract static class BaseLimit<T extends Limit> implements Limit {
        protected final double brightness;
        protected BaseLimit(double brightness) {
            this.brightness = brightness;
        }
        public double getBrightness() { return brightness; }
        public Magnitude toMagnitude(Magnitude.Band band) { return new Magnitude(band, brightness);}

        public T adjust(double adjustment) {
            return make(brightness + adjustment);
        }

        protected abstract T make(double b);

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o.getClass().equals(this.getClass()))) return false;

            BaseLimit baseLimit = (BaseLimit) o;
            return (Double.compare(baseLimit.brightness, brightness) == 0);
        }

        @Override public int hashCode() {
            long temp = brightness != +0.0d ? Double.doubleToLongBits(brightness) : 0L;
            return (int) (temp ^ (temp >>> 32));
        }

        @Override public String toString() { return String.valueOf(brightness); }
    }

    public static final class FaintnessLimit extends BaseLimit<FaintnessLimit> implements Comparable<FaintnessLimit> {
        public FaintnessLimit(double brightness) { super(brightness); }

        @Override public FaintnessLimit make(double adjustment) {
            return new FaintnessLimit(brightness);
        }

        public boolean contains(Magnitude mag) { return mag.getBrightness() <= brightness; }

        @Override public int compareTo(FaintnessLimit that) {
            return Double.compare(getBrightness(), that.getBrightness());
        }
    }

    public static final class SaturationLimit extends BaseLimit<SaturationLimit> implements Comparable<SaturationLimit> {
        public SaturationLimit(double brightness) { super(brightness); }

        @Override public SaturationLimit make(double adjustment) {
            return new SaturationLimit(brightness);
        }

        public boolean contains(Magnitude mag) { return mag.getBrightness() >= brightness; }
        @Override public int compareTo(SaturationLimit that) {
            return Double.compare(getBrightness(), that.getBrightness());
        }
    }

    public static MagnitudeLimits empty(Magnitude.Band band) {
        return new MagnitudeLimits(band, new FaintnessLimit(0.0), new SaturationLimit(0.0));
    }

    private final Magnitude.Band band;
    private final FaintnessLimit faintnessLimit;
    private final Option<SaturationLimit> saturationLimit;

    public MagnitudeLimits(Magnitude.Band band, FaintnessLimit faintnessLimit, Option<SaturationLimit> saturationLimit) {
        this.band            = band;
        this.faintnessLimit  = faintnessLimit;
        this.saturationLimit = saturationLimit;
    }

    public MagnitudeLimits(Magnitude.Band band, FaintnessLimit faintnessLimit, SaturationLimit saturationLimit) {
        this(band, faintnessLimit, new Some<SaturationLimit>(saturationLimit));
    }

    public MagnitudeLimits(Magnitude faint) {
        this(faint.getBand(), new FaintnessLimit(faint.getBrightness()), None.<SaturationLimit>instance());
    }

    public MagnitudeLimits copy(Magnitude.Band b) {
        return new MagnitudeLimits(b, this.faintnessLimit, this.saturationLimit);
    }

    public MagnitudeLimits copy(FaintnessLimit fl) {
        return new MagnitudeLimits(this.band, fl, this.saturationLimit);
    }

    public MagnitudeLimits copy(SaturationLimit sl) {
        return new MagnitudeLimits(this.band, this.faintnessLimit, sl);
    }

    public MagnitudeLimits copy(Option<SaturationLimit> sl) {
        return new MagnitudeLimits(this.band, this.faintnessLimit, sl);
    }

    public Magnitude.Band getBand() {
        return band;
    }

    public FaintnessLimit getFaintnessLimit() {
        return faintnessLimit;
    }

    public Option<SaturationLimit> getSaturationLimit() {
        return saturationLimit;
    }

    public Magnitude faint() {
        return faintnessLimit.toMagnitude(band);
    }

    public Option<Magnitude> saturation() {
        return saturationLimit.map(new MapOp<SaturationLimit, Magnitude>() {
            @Override public Magnitude apply(SaturationLimit l) {
                return l.toMagnitude(band);
            }
        });
    }

    public MagnitudeLimits mapMagnitudes(MapOp<Magnitude, Magnitude> op) {
        Magnitude newFaint = op.apply(faint());
        Option<Magnitude> newSatOpt = saturation().map(op);

        FaintnessLimit fl = new FaintnessLimit(newFaint.getBrightness());
        Option<SaturationLimit> sl = newSatOpt.map(new MapOp<Magnitude, SaturationLimit>() {
            @Override public SaturationLimit apply(Magnitude m) {
                return new SaturationLimit(m.getBrightness());
            }
        });

        return new MagnitudeLimits(newFaint.getBand(), fl, sl);
    }

    /**
     * Returns a predicate that accepts SkyObjects with a magnitude contained
     * by this MagnitudesLimits object.
     */
    public PredicateOp<SkyObject> skyObjectFilter() {
        return new PredicateOp<SkyObject>() {
            @Override public Boolean apply(SkyObject candidate) {
                return candidate.getMagnitude(getBand()).map(new MapOp<Magnitude, Boolean>() {
                    @Override public Boolean apply(Magnitude mag) {
                        return contains(mag);
                    }
                }).getOrElse(false);
            }
        };
    }

    /**
     * Determines whether the magnitude limits include the given magnitude
     * value.
     */
    public boolean contains(final Magnitude mag) {
        // bands must match
        if (mag.getBand() != band) return false;

        // must not be too faint
        if (!faintnessLimit.contains(mag)) return false;

        // nor too bright
        return saturationLimit.forall(new PredicateOp<SaturationLimit>() {
            @Override
            public Boolean apply(SaturationLimit sl) {
                return sl.contains(mag);
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final MagnitudeLimits that = (MagnitudeLimits) o;
        if (band != that.band) return false;
        if (!faintnessLimit.equals(that.faintnessLimit)) return false;
        return saturationLimit.equals(that.saturationLimit);
    }

    @Override
    public int hashCode() {
        int result = band.hashCode();
        result = 31 * result + faintnessLimit.hashCode();
        result = 31 * result + saturationLimit.hashCode();
        return result;
    }

    @Override public String toString() {
        return band.toString() + "(" + faintnessLimit + "," +
                (saturationLimit.isEmpty() ? ".." : saturationLimit.getValue()) + ")";
    }

    /**
     * Returns a combination of two MagnitudeLimits (this and that) such that
     * the faintness limit is the faintest of the two and the saturation limit
     * is the brightest of the two.  In other words, the widest possible range
     * of magnitude bands.
     */
    public Option<MagnitudeLimits> union(MagnitudeLimits that) {
        // There is no way to combine magnitude limits for distinct bands.
        if (!getBand().equals(that.getBand())) return None.instance();

        final FaintnessLimit f1 = this.getFaintnessLimit();
        final FaintnessLimit f2 = that.getFaintnessLimit();
        final FaintnessLimit faint = (f1.compareTo(f2) < 0) ? f2 : f1;

        final Option<SaturationLimit> s1 = this.getSaturationLimit();
        final Option<SaturationLimit> s2 = that.getSaturationLimit();
        final Option<SaturationLimit> sat =
                (s1.isEmpty() || s2.isEmpty()) ? None.<SaturationLimit>instance() :
                        ( s1.getValue().compareTo(s2.getValue()) < 0 ? s1 : s2 );

        return new Some<>(new MagnitudeLimits(this.getBand(), faint, sat));
    }
}
