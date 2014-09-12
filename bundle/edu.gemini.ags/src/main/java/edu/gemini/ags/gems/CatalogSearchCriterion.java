package edu.gemini.ags.gems;

import edu.gemini.catalog.api.MagnitudeLimits;
import edu.gemini.catalog.api.RadiusLimits;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.CoordinateDiff;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.skyobject.coords.SkyCoordinates;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;


/**
 * Used to query catalogs and filter and categorize query results.
 * See OT-20
 */
public class CatalogSearchCriterion {

    private String name;
    private MagnitudeLimits magLimits;
    private RadiusLimits radiusLimits;
    private Option<Offset> offset = None.instance();
    private Option<Angle> posAngle = None.instance();

    public CatalogSearchCriterion(String name, MagnitudeLimits magLimits, RadiusLimits radiusLimits,
                                  Option<Offset> offset, Option<Angle> posAngle) {
        this.name = name;
        this.magLimits = magLimits;
        this.radiusLimits = radiusLimits;
        this.offset = offset;
        this.posAngle = posAngle;
    }

    public String getName() {
        return name;
    }

    public MagnitudeLimits getMagLimits() {
        return magLimits;
    }

    public RadiusLimits getRadiusLimits() {
        return radiusLimits;
    }

    public Option<Offset> getOffset() {
        return offset;
    }

    public Option<Angle> getPosAngle() {
        return posAngle;
    }

    /**
     * If offset and pos angle are specified, then we want the coordinates of the
     * offset position when rotated for the position angle.
     *
     * @param base the base position
     * @return the adjusted base position (base + (offset position rotated by position angle))
     */
    public SkyCoordinates adjustedBase(SkyCoordinates base) {
        if (!offset.isEmpty() && !posAngle.isEmpty()) {
            double pa = posAngle.getValue().toRadians().getMagnitude();
            if (pa != 0.) {
                Offset off = offset.getValue();
                double p = off.p().getMagnitude();
                double q = off.q().getMagnitude();
                // rotate (p,q) by posAngle about (0,0)
                double tmp = p;
                double cosa = Math.cos(pa);
                double sina = Math.sin(pa);
                p = p * cosa + q * sina;
                q = -tmp * sina + q * cosa;

                // return base + (offset position rotated by position angle)
                HmsDegCoordinates b = base.toHmsDeg(0);
                return new HmsDegCoordinates.Builder(
                        b.getRa().add(new Angle(p, off.p().getUnit())),
                        b.getDec().add(new Angle(q, off.q().getUnit()))).build();
            }
        }
        return base;
    }



    /**
     * If there is an offset but there isn't a posAngle, then we have to adjust the
     * search radius to take into account any position angle. That means the
     * outer limit increases by the distance from the base to the offset and the
     * inner limit decreases by the same distance (never less than 0 though).
     *
     * @return the (possibly ) adjusted radius limits
     */
    public RadiusLimits adjustedLimits() {
        if (!offset.isEmpty() && posAngle.isEmpty()) {
            return radiusLimits.adjust(offset.getValue());
        }
        return radiusLimits;
    }


    /**
     * Sets the position angle to a specific value. This will have an impact on the
     * adjustedBase (to which radius limits are applied)
     *
     * @param pa the position angle
     */
    public CatalogSearchCriterion fixPosAngle(Angle pa) {
        Option<Angle> newPosAngle = new Some<Angle>(new Angle(pa.getMagnitude(), pa.getUnit()));
        return new CatalogSearchCriterion(name, magLimits, radiusLimits, offset, newPosAngle);
    }


    /**
     * Sets the offset to a specific value.
     *
     * @param off the offset from the base position
     */
    public CatalogSearchCriterion applyOffset(Offset off) {
        Angle p = off.p();
        Angle q = off.q();
        Option<Offset> newOffset = new Some<Offset>(new Offset(
                new Angle(p.getMagnitude(), p.getUnit()),
                new Angle(q.getMagnitude(), q.getUnit())));
        return new CatalogSearchCriterion(name, magLimits, radiusLimits, newOffset, posAngle);
    }


    // Local class used to match SkyObjects against the mag and radius limits
    public class Matcher {
        private SkyCoordinates adjBase;
        private RadiusLimits adjLimits;

        // When doing the guide star search, use the adjustedBase and the adjustedLimits.
        public Matcher(SkyCoordinates base) {
            adjBase = adjustedBase(base);
            adjLimits = adjustedLimits();
        }

        /**
         * @param obj the SkyObject to match
         * @return true if the object matches the magnitude and radius limits
         */
        public boolean matches(SkyObject obj) {
            return matches(obj.getMagnitudes()) && matches(obj.getHmsDegCoordinates());
        }

        // Returns true if the coordinates are within range of adjBase using adjLimits
        private boolean matches(HmsDegCoordinates coords) {
            HmsDegCoordinates base = adjBase.toHmsDeg(0L);
            CoordinateDiff diff = new CoordinateDiff(base.getRa(), base.getDec(), coords.getRa(), coords.getDec());
            double dist = diff.getDistance().toArcsecs().getMagnitude();
            double minRadius = adjLimits.getMinLimit().toArcsecs().getMagnitude();
            double maxRadius = adjLimits.getMaxLimit().toArcsecs().getMagnitude();
            return dist >= minRadius && dist <= maxRadius;
        }

        // Returns true if magList contains a magnitude with same band as maglimits and
        // the matching magnitude is in the range of the limits
        private boolean matches(ImList<Magnitude> magList) {
            for(Magnitude mag : magList) {
                 if (matches(mag)) return true;
            }
            return false;
        }

        // Returns true if the given magnitude has the same band as maglimits and
        // is in the range of the limits
        private boolean matches(Magnitude mag) {
            return magLimits.contains(mag);
        }
    }


    /**
     * This can be used as a predicate to filter on a List[SkyObject].
     *
     * @param base the base position
     * @return a new Matcher for the given base position
     */
    public Matcher matcher(SkyCoordinates base) {
        return new Matcher(base);
    }

    @Override
    public String toString() {
        return "CatalogSearchCriterion{" +
                "name='" + name + '\'' +
                ", magLimits=" + magLimits +
                ", radiusLimits=" + radiusLimits +
                ", offset=" + offset +
                ", posAngle=" + posAngle +
                '}';
    }
}
