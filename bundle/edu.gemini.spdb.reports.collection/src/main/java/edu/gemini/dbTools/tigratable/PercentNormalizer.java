//
// $Id: PercentNormalizer.java 5128 2004-09-21 13:59:07Z shane $
//

package edu.gemini.dbTools.tigratable;

/**
 * A utility used to round an array of double percentages into integers where
 * the values add up to 100.  It selects the "best" value to round if
 * necessary.
 */
class PercentUtil {
    private static class Percent {
        private int rounded;
        private final double diff;
        private boolean adjusted;

        public Percent(final double percent) {
            this.rounded = (int) Math.round(percent);
            this.diff    = percent - rounded;
        }
    }

    /**
     * Calculates integer percentages of each value in the given array, rounding
     * as necessary to make the sum of percentages returned add up to 100.
     *
     * @param values values that make up the whole
     *
     * @return array of integer percentages of the total for each of the given
     * values
     */
    static int[] getPercents(final int[] values) {
        if (values.length == 0) return values;

        // Get the total.
        int total = 0;
        for (final int val : values) {
            if (val < 0) throw new IllegalArgumentException("" + val);
            total += val;
        }

        // Record the percentages and the initial rounded values.
        int roundedTotal = 0;
        final Percent[] pA = new Percent[values.length];
        for (int i=0; i<values.length; ++i) {
            final double percent = ((double) values[i])/total * 100.0;
            pA[i] = new Percent(percent);
            roundedTotal += pA[i].rounded;
        }

        while (roundedTotal < 100) {
            // Find the largest diff between the real value and the rounded
            // value.  This represents the best candidate to round up.
            int sel = 0;
            double maxDiff = 0.0;
            for (int i=0; i<pA.length; ++i) {
                final Percent p = pA[i];
                if (p.adjusted) continue;
                if (p.diff > maxDiff) {
                    maxDiff = p.diff;
                    sel = i;
                }
            }
            ++roundedTotal;
            ++pA[sel].rounded;
            pA[sel].adjusted = true;
        }

        while (roundedTotal > 100) {
            // Find the smallest diff between the real value and the rounded
            // value.  The represents the best candidate to round down.
            int sel = 0;
            double minDiff = Double.MAX_VALUE;
            for (int i=0; i<pA.length; ++i) {
                final Percent p = pA[i];
                if (p.adjusted) continue;
                if (p.diff < minDiff) {
                    minDiff = p.diff;
                    sel = i;
                }
            }
            --roundedTotal;
            --pA[sel].rounded;
            pA[sel].adjusted = true;
        }

        final int[] res = new int[pA.length];
        for (int i=0; i<pA.length; ++i) {
            res[i] = pA[i].rounded;
        }
        return res;
    }

//    private static void _assertEquals(int[] expected, int[] actual) {
//        if (expected.length != actual.length) {
//            throw new RuntimeException("expected " + expected.length +
//                                       " values, got " + actual.length);
//        }
//        for (int i=0; i<expected.length; ++i) {
//            if (expected[i] != actual[i]) {
//                throw new RuntimeException("expected[" + i + "]=" + expected[i] +
//                                           ", actual[" + i + "]=" + actual[i]);
//            }
//        }
//    }
//
//    private static void _validate(int[] values, int[] expected) {
//        int[] actual = normalize(values);
//        _assertEquals(expected, actual);
//    }
//
//    public static void main(String[] args) {
//        int[] values = { 3, 3, 3 };
//        int[] expected = { 34, 33, 33 };
//        _validate(values, expected);
//
//        values   = new int[] { 6, 6, 6, 6, 976 };
//        expected = new int[] { 0, 0, 1, 1, 98 };
//        _validate(values, expected);
//    }
}
