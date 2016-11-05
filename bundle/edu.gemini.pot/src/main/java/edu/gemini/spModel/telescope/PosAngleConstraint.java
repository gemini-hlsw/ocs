package edu.gemini.spModel.telescope;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.core.Angle;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        public String description() {
            return "Allow 180\u00ba flip";
        }
    },

    /** The unbounded case, where we want to allow for any angle accessible
     * by the guide probe.
     */
    UNBOUNDED() {
        /**
         * Steps through position angles from the start angle through 360 degrees
         * by step size.
         */
        @Override
        public ImList<Angle> steps(Angle start, Angle stepSize) {
            final int stepSizeDegrees = (int) stepSize.toDegrees();
            final int numSteps = (stepSizeDegrees == 0) ? 1 : 360 / stepSizeDegrees;

            final List<Angle> angles = IntStream.range(0, numSteps).mapToObj(i -> start.$plus(stepSize.$times(i))).collect(Collectors.toList());
            return DefaultImList.create(angles);
        }

        @Override
        public String description() {
            return "Best position angle";
        }

        @Override
        public boolean isCalculated() {
            return true;
        }
    },

    /** Parallactic angle allows provided pos angle, or pos angle plus 180 deg. */
    PARALLACTIC_ANGLE() {
        @Override
        public String description() { return "Average parallactic"; }

        @Override
        public boolean isCalculated() {
            return true;
        }
    },

    /** Parallactic angle is displayed, but overridden by a FIXED_180 value specified by user. */
    PARALLACTIC_OVERRIDE() {
        @Override
        public String description() { return "Parallactic override"; }
    }
    ;

    /**
     * Steps through position angles from the start angle through 360 degrees
     * by step size, but obeying the position angle constraint.  For example,
     * the FIXED constraint would provide just a single angle, the start
     * angle.  An UNKNOWN or UNBOUNDED constraint will step through the entire range.
     * As the range for multiple types is fixed / fixed + 180, that is by default used.
     */
    public ImList<Angle> steps(Angle start, Angle requestedStepSize) {
        return DefaultImList.create(start, start.flip());
    }

    /**
     * Description of the constraint for use in creating GUI combo boxes.
     */
    public abstract String description();

    /**
     * Is this value automatically calculated?
     */
    public boolean isCalculated() {
        return false;
    }

    @Override
    public String toString() { return description(); }
}
