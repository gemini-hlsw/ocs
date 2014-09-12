//
// $Id: DatasetQaStateSum.java 6382 2005-06-13 23:45:57Z shane $
//

package edu.gemini.spModel.dataset;

import java.io.Serializable;

/**
 * An association of a {@link DatasetQaState} value and a count.  This value
 * represents the number of times that the DatasetQaState appears in a
 * collection of DatasetQaState values. This is an immutable value object.
 */
public final class DatasetQaStateSum implements Comparable, Serializable {
    private static final long serialVersionUID = 1L;

    private DatasetQaState _state;
    private int _count;

    /**
     * Constructs with the {@link DatasetQaState} and its count.
     *
     * @param state the dataset qa state, which may not be <code>null</code>
     * @param count number of times this qa state appear s
     */
    public DatasetQaStateSum(DatasetQaState state, int count) {
        if (state == null) throw new NullPointerException();
        if (count < 0) throw new IllegalArgumentException("" + count);

        _state = state;
        _count = count;
    }

    /**
     * Gets the count associated with the DatasetQaState.
     */
    public int getCount() {
        return _count;
    }

    /**
     * Adds the given amount to the count.  If the result would be negative,
     * it is set to 0 since negative counts are not supported.  A new
     * DatasetQaStateSum is returned as a result of this operation.
     */
    public DatasetQaStateSum add(int count) {
        count += _count;
        if (count < 0) count = 0;
        return new DatasetQaStateSum(_state, count);
    }

    /**
     * Gets the QA state.
     */
    public DatasetQaState getDatasetQaState() {
        return _state;
    }

    public int compareTo(Object other) {
        DatasetQaStateSum that = (DatasetQaStateSum) other;
        int res = _state.compareTo(that._state);
        if (res != 0) return res;
        return _count - that._count;
    }

    public boolean equals(Object other) {
        if (!(other instanceof DatasetQaStateSum)) return false;

        DatasetQaStateSum that = (DatasetQaStateSum) other;
        if (_state != that._state) return false;
        if (_count != that._count) return false;

        return true;
    }

    public int hashCode() {
        return 37*_count + _state.hashCode();
    }
}
