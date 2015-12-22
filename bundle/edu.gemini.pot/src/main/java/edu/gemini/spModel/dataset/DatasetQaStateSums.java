package edu.gemini.spModel.dataset;

import java.io.Serializable;

/**
 * A summary of a collection of {@link DatasetQaState} values.  This class
 * holds the count of each number of the various types of DatasetQaState.
 * This is an immutable value object.
 */
public final class DatasetQaStateSums implements Comparable<DatasetQaStateSums>, Serializable {
    private static final long serialVersionUID = 1L;

    private int[] _counts = new int[DatasetQaState.values().length];

    /**
     * A constant reference to a DatasetQaStateSums object with all 0 counts.
     */
    public static final DatasetQaStateSums ZERO_SUMS = new DatasetQaStateSums();

    /**
     * Constructs with all charges equal to <code>0</code>.
     */
    public DatasetQaStateSums() {
    }

    /**
     * Constructs with the given set of {@link DatasetQaStateSum} values.  If
     * any {@link DatasetQaState} is represented more than once in the
     * <code>counts</code> array, only the last value is used.  Any
     * {@link DatasetQaState}s not represented in <code>counts</code> are set
     * to zero.
     */
    public DatasetQaStateSums(DatasetQaStateSum[] sums) {
        if (sums == null) return;

        for (DatasetQaStateSum s : sums) {
            _counts[s.getDatasetQaState().ordinal()] = s.getCount();
        }
    }

    /**
     * Gets the total number of datasets, regardless of QA state.
     */
    public int getTotalDatasets() {
        int res = 0;
        for (int _count : _counts) {
            res += _count;
        }
        return res;
    }

    /**
     * Gets the count associated with the given {@link DatasetQaState}.
     */
    public int getCount(DatasetQaState state) {
        return _counts[state.ordinal()];
    }

    /**
     * Gets the {@link DatasetQaStateSum} associated with the given
     * <code>DatasetQaState</code>.
     */
    public DatasetQaStateSum getDatasetQaStateSum(DatasetQaState state) {
        return new DatasetQaStateSum(state, getCount(state));
    }

    /**
     * Creates a new instance of DatasetQaStateSums that is the same as this
     * instance, except that the count for <code>state</code> is
     * increased (or decreased) by the
     * specified in the  <code>count</code> parameter.  Zero is used if the
     * resulting count is otherwise negative.
     *
     * @param count amount to add to the count associated with
     * <code>state</code>
     * @param state dataset qa state whose count should be increased (or
     * decreased)
     *
     * @return a new DatasetQaStateSums instance whose count for
     * <code>state</code> has been increased or decreased by <code>count</code>
     */
    public DatasetQaStateSums addCount(int count, DatasetQaState state) {
        DatasetQaStateSums res = new DatasetQaStateSums();
        System.arraycopy(_counts, 0, res._counts, 0, _counts.length);
        res._counts[state.ordinal()] += count;
        return res;
    }

    /**
     * Creates a new instance of DatasetQaStateSums where the count
     * associated with the {@link DatasetQaState} of <code>sum</code> has been
     * adjusted by the count of the <code>sum</code> argument.
     *
     * @param sum sum to apply to this instance
     *
     * @return a new DatasetQaStateSums instance whose time for
     * <code>sum.getDatastQaState()</code> has been increased or decreased by
     * the count included in the <code>sum</code> argument
     */
    public DatasetQaStateSums addCount(DatasetQaStateSum sum) {
        return addCount(sum.getCount(), sum.getDatasetQaState());
    }

    /**
     * Creates a new DatasetQaStateSums instance in which all the counts have
     * been been adjusted by the corresponding values in <code>sums</code>.
     * For example, the amount for the {@link DatasetQaState} PASS is
     * increased or decreased according to <code>sums</code> count
     * for the PROGRAM.
     *
     * @return a new DatasetQaStateSums instance whose counts are the result of
     * adding corresponding counts in this instance and in
     * <code>sums</code>
     */
    public DatasetQaStateSums addCounts(DatasetQaStateSums sums) {
        DatasetQaStateSums res = new DatasetQaStateSums();
        for (int i=0; i<_counts.length; ++i) {
            res._counts[i] = _counts[i] + sums._counts[i];
        }
        return res;
    }

    @Override
    public int compareTo(DatasetQaStateSums that) {
        for (int i=0; i<_counts.length; ++i) {
            int res = _counts[i] - that._counts[i];
            if (res != 0) return res;
        }
        return 0;
    }

    public boolean equals(Object o) {
        if (!(o instanceof DatasetQaStateSums)) return false;
        DatasetQaStateSums that = (DatasetQaStateSums) o;

        for (int i=0; i<_counts.length; ++i) {
            if (_counts[i] != that._counts[i]) return false;
        }
        return true;
    }

    public int hashCode() {
        int res = 0;
        for (int count : _counts) {
            res = 37 * res + count;
        }
        return res;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("DatasetQaStateSums [");
        DatasetQaState[] states = DatasetQaState.values();
        for (int i=0; i<states.length; ++i) {
            DatasetQaState state = states[i];
            buf.append(state.name()).append(" = ").append(_counts[i]).append(", ");
        }
        buf.replace(buf.length()-2, buf.length()-1, "]");

        return buf.toString();
    }
}
