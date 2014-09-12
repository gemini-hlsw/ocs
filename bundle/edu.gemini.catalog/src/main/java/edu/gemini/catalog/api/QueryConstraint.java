package edu.gemini.catalog.api;

import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.CoordinateDiff;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.PredicateOp;

/**
 * The parameters for an AGS catalog query.
 */
public final class QueryConstraint {
    public final int id; // optional id for this constraint
    public final Coordinates base;
    public final RadiusLimits radiusLimits;
    public final MagnitudeLimits magnitudeLimits;

    public QueryConstraint(Coordinates base, RadiusLimits radiusLimits, MagnitudeLimits magnitudeLimits) {
        this(0, base, radiusLimits, magnitudeLimits);
    }

    public QueryConstraint(int id, Coordinates base, RadiusLimits radiusLimits, MagnitudeLimits magnitudeLimits) {
        this.id              = id;
        this.base            = base;
        this.radiusLimits    = radiusLimits;
        this.magnitudeLimits = magnitudeLimits;
    }

    public QueryConstraint copy(Coordinates base) {
        return new QueryConstraint(this.id, base, this.radiusLimits, this.magnitudeLimits);
    }

    public QueryConstraint copy(RadiusLimits limits) {
        return new QueryConstraint(this.id, this.base, limits, this.magnitudeLimits);
    }

    public QueryConstraint copy(MagnitudeLimits limits) {
        return new QueryConstraint(this.id, this.base, this.radiusLimits, limits);
    }

    public boolean isSupersetOf(QueryConstraint that) {
        return isWithinMagnitudeLimits(that) && isWithinRadiusLimits(that);
    }

    public boolean isSubsetOf(QueryConstraint that) {
        return that.isSupersetOf(this);
    }

    public boolean isWithinRadiusLimits(QueryConstraint that) {
        CoordinateDiff cd = new CoordinateDiff(base, that.base);

        // Angular separation, or distance between the two.
        Angle dist = cd.getDistance();

        // Add the given query's outer radius limit to the distance to get the
        // maximum distance from this base position of any potential guide star.
        Angle max = dist.add(that.radiusLimits.getMaxLimit());

        // See whether the other base position falls out of range of our
        // radius limits.
        return radiusLimits.getMaxLimit().compareToAngle(max) >= 0;
    }

    public boolean isWithinMagnitudeLimits(QueryConstraint that) {
        final Magnitude.Band band = magnitudeLimits.getBand();

        // The band has to match or else we aren't comparing apples to apples.
        if (band != that.magnitudeLimits.getBand()) return false;

        // This faintness limit must be the same as or fainter than the given
        // query's faintness limit.
        MagnitudeLimits.Limit thisLimit = magnitudeLimits.getFaintnessLimit();
        Magnitude m = that.magnitudeLimits.getFaintnessLimit().toMagnitude(band);
        if (!thisLimit.contains(m)) return false;

        // The saturation limit comparison is difficult in Java.  Either
        // magnitude limit could have empty saturation limit.  If this query
        // has no saturation limit, then that counts as "within".  If this
        // query has a saturation limit but that one doesn't, that does not
        // count as within.  If both have a saturation limit, then they can
        // just be compared.
        Option<MagnitudeLimits.SaturationLimit> o = magnitudeLimits.getSaturationLimit();
        final Option<Magnitude> mo = that.magnitudeLimits.getSaturationLimit().map(new MapOp<MagnitudeLimits.SaturationLimit, Magnitude>() {
            @Override public Magnitude apply(MagnitudeLimits.SaturationLimit saturationLimit) {
                return saturationLimit.toMagnitude(band);
            }
        });
        return o.forall(new PredicateOp<MagnitudeLimits.SaturationLimit>() {
            @Override public Boolean apply(final MagnitudeLimits.SaturationLimit saturationLimit) {
                return mo.map(new MapOp<Magnitude, Boolean>() {
                    @Override public Boolean apply(Magnitude magnitude) {
                        return saturationLimit.contains(magnitude);
                    }
                }).getOrElse(false);
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final QueryConstraint that = (QueryConstraint) o;
        if (id != that.id) return false;
        if (!base.equals(that.base)) return false;
        if (!magnitudeLimits.equals(that.magnitudeLimits)) return false;
        return radiusLimits.equals(that.radiusLimits);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + base.hashCode();
        result = 31 * result + radiusLimits.hashCode();
        result = 31 * result + magnitudeLimits.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("QueryConstraint{");
        sb.append("id=").append(id);
        sb.append(", base=").append(base);
        sb.append(", radiusLimits=").append(radiusLimits);
        sb.append(", magnitudeLimits=").append(magnitudeLimits);
        sb.append('}');
        return sb.toString();
    }
}
