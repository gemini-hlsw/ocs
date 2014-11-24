package edu.gemini.spModel.telescope;

import edu.gemini.skycalc.Angle;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.shared.util.immutable.ImList;

import java.util.ArrayList;
import java.util.List;

/**
 * Constraint options on automatic position angle adjustments.
 */
public enum PosAngleConstraint {
    /** The provided pos angle only. */
    FIXED() {
        @Override
        public ImList<Angle> steps(Angle start, Angle stepSize) {
            return ImCollections.singletonList(start);
        }

        @Override
        public String description() {
            return "Fixed";
        }
    },

    /** The provided pos angle, or the pos angle plus 180 deg. */
    FIXED_180() {
        @Override
        public ImList<Angle> steps(Angle start, Angle stepSize) {
            return DefaultImList.create(start, start.add(Angle.ANGLE_PI));
        }

        @Override
        public String description() {
            return "Allow 180\u00ba flip";
        }
    },

    /** The unbounded case, where we want to allow for any angle accessible
     * by the guide probe.
     */
    UNBOUNDED() {
        @Override
        public String description() {
            //return "Allow guide star search to set position angle";
            return "Find best";
        }
    },

    /** Parallactic angle allows provided pos angle, or pos angle plus 180 deg. */
    PARALLACTIC_ANGLE() {
        @Override
        public ImList<Angle> steps(Angle start, Angle stepSize) {
            return DefaultImList.create(start, start.add(Angle.ANGLE_PI));
        }

        @Override
        public String description() { return "Average parallactic"; }
    }
    ;

    /**
     * Steps through position angles from the start angle through 360 degrees
     * by step size, but obeying the position angle constraint.  For example,
     * the FIXED constraint would provide just a single angle, the start
     * angle.  An UNKNOWN or UNBOUNDED constraint will step through the entire range.
     * As the entire range is used by multiple enum values, it is the default.
     */
    public ImList<Angle> steps(Angle start, Angle stepSize) {
        int deg = (int) Math.round(stepSize.toPositive().toDegrees().getMagnitude());
        int stepCount = (deg == 0) ? 1 : 360 / deg;

        Angle cur = start;
        List<Angle> res = new ArrayList<Angle>(stepCount);
        for (int i=0; i<stepCount; ++i) {
            res.add(cur);
            cur = cur.add(stepSize);
        }

        return DefaultImList.create(res);
    }

    /**
     * Description of the constraint for use in creating GUI combo boxes.
     */
    public abstract String description();

    @Override
    public String toString() { return description(); }
}
