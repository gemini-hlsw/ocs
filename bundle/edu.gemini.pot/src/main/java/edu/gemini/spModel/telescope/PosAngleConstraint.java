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
    /** No constraint on adjusting pos angle. */
    UNKNOWN() {
        @Override
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
    },

    /** The provided pos angle only. */
    FIXED() {
        @Override
        public ImList<Angle> steps(Angle start, Angle stepSize) {
            return ImCollections.singletonList(start);
        }
    },

    /** The provided pos angle, or the pos angle plus 180 deg. */
    FIXED_180() {
        @Override
        public ImList<Angle> steps(Angle start, Angle stepSize) {
            return DefaultImList.create(start, start.add(Angle.ANGLE_PI));
        }
    },

    /** Any pos angle that is reachable by the guide probe. */
    UNBOUNDED,

    /** A pos angle aligned with the parallactic angle. */
//    PARALLACTIC,

    /** A pos angle aligned with the prallactic angle. */
//    PARALLACTIC_180,
    ;

    /**
     * Steps through position angles from the start angle through 360 degrees
     * by step size, but obeying the position angle constraint.  For example,
     * the FIXED constraint would provide just a single angle, the start
     * angle.  A NONE constraint will step through the entire range.
     */
    public ImList<Angle> steps(Angle start, Angle stepSize) { return ImCollections.emptyList(); }
}
