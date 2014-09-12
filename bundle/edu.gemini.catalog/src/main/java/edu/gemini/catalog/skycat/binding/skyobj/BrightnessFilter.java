//
// $
//

package edu.gemini.catalog.skycat.binding.skyobj;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.PredicateOp;

/**
 * A simple PredicateOp used to filter out magnitude values that signal
 * the lack of information.  Some catalogs use, for example, -99.9 to mark
 * the absence of information while others might use 99.9, etc.
 */
public final class BrightnessFilter implements PredicateOp<Magnitude> {

    /**
     * Comparison operator to use.
     */
    public static enum Op {
        /** Greater than. */
        gt() {
            public boolean compare(double magnitude, double limit) {
                return magnitude > limit;
            }
        },
        /** Greater than or equals. */
        ge() {
            public boolean compare(double magnitude, double limit) {
                return magnitude >= limit;
            }
        },
        /** Less than. */
        lt() {
            public boolean compare(double magnitude, double limit) {
                return magnitude < limit;
            }
        },
        /** Less than or equals. */
        le() {
            public boolean compare(double magnitude, double limit) {
                return magnitude <= limit;
            }
        },
        ;

        /**
         * Executes the comparison.
         *
         * @param magnitude magnitude value (rhs)
         * @param limit limit (lhs)
         *
         * @return <code>true</code> if the magnitude value should be
         * considered within the limit according to this comparison operator.
         */
        public abstract boolean compare(double magnitude, double limit);
    }

    private final Op op;
    private final double limit;

    /**
     * Constructs with the comparison operator to use and the limit that will
     * be compared against magnitudes in the {@link #apply} method.
     *
     * @param op comparison operator
     * @param limit limit against which brightness values are compared
     */
    public BrightnessFilter(Op op, double limit) {
        this.op = op;
        this.limit = limit;
    }

    /**
     *
     * @param magnitude
     * @return
     */
    @Override
    public Boolean apply(Magnitude magnitude) {
        return op.compare(magnitude.getBrightness(), limit);
    }
}
