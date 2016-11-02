package edu.gemini.spModel.guide;

import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.CoordinateDiff;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.skycalc.Offset;
import edu.gemini.spModel.obs.SchedulingBlock;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.Collection;
import java.util.Set;

/**
 * Generic representation of a patrol field for a guide probe. The patrol field is the area that can be covered
 * by a guide probe. In general the patrol fields have a fuzzy boundary represented by a "safe" inner area and
 * an outer limit. Inside the safe area guide stars can be used for sure, guide stars outside the safe area but
 * inside the outer limits may be unusable (a warning is issued by the phase 2 checks) and guide stars outside
 * the outer limits can not be used at all (phase 2 checks will issue an error).
 * Some instruments have obstacles in the light path (like probe arms or pickup mirrors) which will block certain
 * areas in the patrol field. Guide stars in blocked areas can not be used (phase 2 checks will issue an error).
 * All dimensions are in arc seconds and the positions are relative to the instruments coordinate system. For
 * detailed descriptions of the instruments and their patrol fields please refer to:
 * http://www.gemini.edu/sciops/instruments/?q=sciops/instruments.
 */
public class PatrolField {
    private final Area blocked;
    private final Area fov;
    private final Area fovIn;
    private final Area fovOut;

    public PatrolField(Shape fov){
        this.blocked = new Area();
        this.fov = new Area(fov);
        this.fovIn = new Area(fov);
        this.fovOut = new Area(fov);
    }

    public PatrolField(Shape fov, Shape blocked){
        this.blocked = new Area(blocked);
        this.fov = new Area(fov);
        this.fov.subtract(this.blocked);
        this.fovIn = new Area(this.fov);
        this.fovOut = new Area(this.fov);
    }

    public PatrolField(Shape fov, Shape fovIn, Shape fovOut){
        this.blocked = new Area();
        this.fov = new Area(fov);
        this.fovIn = new Area(fovIn);
        this.fovOut = new Area(fovOut);
    }

    private PatrolField(PatrolField orig, AffineTransform transformation) {
        this.blocked = orig.getBlockedArea().createTransformedArea(transformation);
        this.fov = orig.getArea().createTransformedArea(transformation);
        this.fovIn = orig.getSafe().createTransformedArea(transformation);
        this.fovOut = orig.getOuterLimit().createTransformedArea(transformation);
    }

    /**
     * Gets the area that can be covered by the guide probe minus any areas blocked by obstacles in the light path.
     * Dimensions are in arcseconds and the positions are relative to the instruments coordinate system.
     * @return
     */
    public Area getArea() { return (Area) fov.clone(); }

    /**
     * Gets the "safe" area inside minus any blocked areas.
     * this is expected to be slightly smaller (~-2 arc-seconds) than {@see getArea()}.
     * @return
     */
    public Area getSafe() { return (Area) fovIn.clone(); }

    /**
     * Gets the furthest area that can be possibly be considered to be covered by the probe minus any blocked areas.
     * This is expected to be slightly larger (~+2 arc-seconds) than {@see getArea()}.
     */
    public Area getOuterLimit() { return (Area) fovOut.clone(); }

    /**
     * Gets the area of the patrol field that is blocked by any obstacles in the path of light.
     * This area is meant to be used for drawing purposes, it is not directly used in the AGS algorithms.
     * @return
     */
    public Area getBlockedArea() { return (Area) blocked.clone(); }


    /**
     * Gets a transformed version of this patrol field.
     * This method can be used to apply any transformation that needs to be taken into account in the instruments
     * coordinate system (e.g. for adding IFU offsets in case of GMOS).
     * @param transformation
     * @return
     */
    public PatrolField getTransformed(AffineTransform transformation) {
        return new PatrolField(this, transformation);
    }


    /**
     * Returns the BoundaryPosition of the candidateCoordinates.
     *
     * @param candidateCoordinates : The coordinates that are being checked against the GuideProbes patrol field.
     * @param baseCoordinates : The center of the viewport (probably the science target).
     * @param positionAngle : The rotation of the viewport.
     * @param sciencePositions : A set of offsets.
     * @return
     */
    public BoundaryPosition checkBoundaries(Coordinates candidateCoordinates, Coordinates baseCoordinates, Angle positionAngle, Set<Offset> sciencePositions) {
        // Calculate the difference between the coordinate and the observation's
        // base position.
        final CoordinateDiff diff = new CoordinateDiff(baseCoordinates, candidateCoordinates);

        // Get offset and switch it to be defined in the same coordinate
        // system as the shape.
        final Offset dis = diff.getOffset();
        final double p = -dis.p().toArcsecs().getMagnitude();
        final double q = -dis.q().toArcsecs().getMagnitude();

        // Get a rotation to transform the shape to the position angle if one has been specified.
        final AffineTransform xform = new AffineTransform();
        xform.rotate(-positionAngle.toRadians().getMagnitude());

        Area a = new Area(calculateUnion(sciencePositions, getBlockedArea()));;
        a.transform(xform);
        if (a.contains(p,q)) return BoundaryPosition.outside;

        a = new Area(calculateIntersection(sciencePositions, getSafe()));
        a.transform(xform);
        if (a.contains(p, q)) return BoundaryPosition.inside;

        a = new Area(calculateIntersection(sciencePositions, getArea()));
        a.transform(xform);
        if (a.contains(p, q)) return BoundaryPosition.innerBoundary;

        a = new Area(calculateIntersection(sciencePositions, getOuterLimit()));
        a.transform(xform);
        if (a.contains(p, q)) return BoundaryPosition.outerBoundary;

        return BoundaryPosition.outside;
    }


    /**
     * Returns true if any guide stars are inside the inner boundary at any offset.
     * @param candidateCoordinates : The coordinates that are being checked against the GuideProbes patrol field
     * @param baseCoordinates : The center of the viewport (probably the science target)
     * @param positionAngle : The rotation of the viewport
     * @param sciencePositions : A set of offsets
     * @return true if any guide stars are inside the inner boundary at any offset
     */
    public boolean anyInside(Coordinates candidateCoordinates, Coordinates baseCoordinates, Angle positionAngle, Set<Offset> sciencePositions) {
        // Calculate the difference between the coordinate and the observation's
        // base position.
        CoordinateDiff diff;
        diff = new CoordinateDiff(baseCoordinates, candidateCoordinates);
        // Get offset and switch it to be defined in the same coordinate
        // system as the shape.
        Offset dis = diff.getOffset();
        double p = -dis.p().toArcsecs().getMagnitude();
        double q = -dis.q().toArcsecs().getMagnitude();


        // Get a rotation to transform the shape to the position angle.
        AffineTransform xform = new AffineTransform();
        xform.rotate(-positionAngle.toRadians().getMagnitude());

        Area a = new Area(calculateUnion(sciencePositions, getSafe()));
        a.transform(xform);
        return a.contains(p, q);
    }

    /**
     * Gets the intersection of the guideProbeRange transformed by the offsets
     *
     * @param offsets positions to include in the intersection
     */
    private Area calculateIntersection(Collection<Offset> offsets, Area area) {
        // TODO : check start conditions: null possible? never return null? what to return with 0 or 1 offset positions...
        if ((offsets == null) || (offsets.size() == 0)) {
            return area;
        }
        Area intersection = null;
        for (Offset off : offsets) {
            double x = -off.p().toArcsecs().getMagnitude();
            double y = -off.q().toArcsecs().getMagnitude();
            AffineTransform xform = AffineTransform.getTranslateInstance(x, y);
            if (intersection == null) {
                intersection = new Area(area);
                intersection.transform(xform);
            } else {
                Area tmp = new Area(area);
                tmp.transform(xform);
                intersection.intersect(tmp);
            }
        }

        return intersection;
    }

    /**
     * Gets the union of the guideProbeRange transformed by the offsets
     *
     * @param offsets positions to include in the union
     */
    private Area calculateUnion(Collection<Offset> offsets, Area area) {
        if ((offsets == null) || (offsets.size() == 0)) {
            return area;
        }
        Area union = null;
        for (Offset off : offsets) {
            double x = -off.p().toArcsecs().getMagnitude();
            double y = -off.q().toArcsecs().getMagnitude();
            AffineTransform xform = AffineTransform.getTranslateInstance(x, y);
            if (union == null) {
                union = new Area(area);
                union.transform(xform);
            } else {
                Area tmp = new Area(area);
                tmp.transform(xform);
                union.add(tmp);
            }
        }

        return union;
    }


    public Area offsetIntersection(Collection<Offset> offsets) {
        return calculateIntersection(offsets, this.getArea());
    }
    public Area safeOffsetIntersection(Collection<Offset> offsets) {
        return calculateIntersection(offsets, this.getSafe());
    }
    public Area outerLimitOffsetIntersection(Collection<Offset> offsets) {
        return calculateIntersection(offsets, this.getOuterLimit());
    }


    /**
     * Gets the Area, adjusted by the position angle and offset positions, in
     * which a guide star may be placed and be reachable by the guider at all
     * offset positions.
     *
     * @param ctx observing context
     *
     * @return valid area in which a guide star may be placed for this context
     */
    public Area usableArea(ObsContext ctx) {
        Area a = offsetIntersection(ctx.getSciencePositions());
        AffineTransform xform = new AffineTransform();
        xform.rotate(-ctx.getPositionAngle().toRadians());
        a.transform(xform);
        return a;
    }

    public static final class Validator implements GuideStarValidator {
        private final Area validArea;

        Validator(Area validArea) {
            this.validArea = validArea;
        }

        @Override
        public GuideStarValidation validate(SPTarget guideStar, ObsContext ctx) {
            return
                guideStar.getSkycalcCoordinates(ctx.getSchedulingBlockStart()).flatMap(guideCoordinates ->
                ctx.getBaseCoordinates().map(baseCoordinates -> {
                    // Calculate the difference between the coordinate and the observation's
                    // base position.
                    CoordinateDiff diff;
                    diff = new CoordinateDiff(baseCoordinates, guideCoordinates);
                    // Get offset and switch it to be defined in the same coordinate
                    // system as the shape.
                    Offset dis = diff.getOffset();
                    double p = -dis.p().toArcsecs().getMagnitude();
                    double q = -dis.q().toArcsecs().getMagnitude();
                    return validArea.contains(p, q) ? GuideStarValidation.VALID : GuideStarValidation.INVALID;
                }))
                .getOrElse(GuideStarValidation.UNDEFINED);
        }
    }

    /**
     * Gets this patrol fields guide star validator for the given context.  The
     * validator will pass guide stars that fall in the usable area of the
     * patrol field at all offset positions.
     *
     * @param ctx observing context
     *
     * @return guide star validator that can be used to determine whether the
     * patrol field reaches a given guide star in the given observing context
     */
    public GuideStarValidator validator(ObsContext ctx) {
        return new Validator(usableArea(ctx));
    }

    public static PatrolField fromRadiusLimits(Angle min, Angle max) {
        return (min.getMagnitude() > 0) ?
                new PatrolField(toCircle(max), toCircle(min)) :
                new PatrolField(toCircle(max));
    }
    private static Ellipse2D toCircle(Angle a) { return toCircle(a.toArcsecs().getMagnitude()); }
    private static Ellipse2D toCircle(double r) { return new Ellipse2D.Double(-r, -r, r*2, r*2); }
}
