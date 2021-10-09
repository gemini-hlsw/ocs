//
// $Id: DatasetLabel.java 6329 2005-06-08 04:04:49Z shane $
//
package edu.gemini.spModel.dataset;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPBadIDException;

import java.io.Serializable;
import java.text.ParseException;


/**
 * A dataset label identifies a dataset by its observation id and sequence
 * within that observation.  This class encapsulates this information and
 * the parsing of the label into its constituent parts.
 */
public final class DatasetLabel implements Comparable<DatasetLabel>, Serializable {
    public static final DatasetLabel[] EMPTY_ARRAY = new DatasetLabel[0];

    private static final long serialVersionUID = 1;

    /**
     * The observation id of the dataset.
     */
    private final SPObservationID _observationId;

    /**
     * The index of this dataset.
     */
    private final int _index;

    /**
     * Constructs with the complete dataset label, which is assumed to be
     * formed by the pattern <obsid>-<index>.
     *
     * @param dsetLabel datatset label
     *
     * @throws NullPointerException if dsetLabel is <code>null</code>
     * @throws java.text.ParseException if the dsetLabel isn't valid
     */
    public DatasetLabel(String dsetLabel) throws ParseException {
        if (dsetLabel == null) throw new NullPointerException("dsetLabel");
        dsetLabel = dsetLabel.trim();

        int pos = dsetLabel.lastIndexOf('-');
        if (pos < 0) throw new ParseException("no '-' character", 0);

        String idStr = dsetLabel.substring(0, pos);
        try {
            _observationId = new SPObservationID(idStr);
        } catch (SPBadIDException ex) {
            throw new ParseException("invalid obs id: " + ex.getMessage(), 0);
        }
        if ((pos + 1) >= dsetLabel.length()) {
            throw new ParseException("missing index #", pos + 1);
        }
        String indexStr = dsetLabel.substring(pos + 1);
        try {
            _index = Integer.parseInt(indexStr);
        } catch (NumberFormatException ex) {
            throw new ParseException(indexStr + " is not an int", pos + 1);
        }
    }


    /**
     * Constructs with all required information.  This class is immutable
     * so no changes can be made after construction.
     *
     * @param obsId observation id
     * @param index dataset index
     *
     * @throws NullPointerException if any field is <code>null</code>
     */
    public DatasetLabel(SPObservationID obsId, int index) {
        if (obsId == null) throw new NullPointerException("obsId");

        _observationId = obsId;
        _index = index;
    }

    /**
     * Provides a natural ordering.
     */
    public int compareTo(DatasetLabel that) {
        int res = _observationId.compareTo(that._observationId);
        if (res != 0) return res;

        res = _index - that._index;
        return res;
    }

    /**
     * Provides semantic equality.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof DatasetLabel)) return false;

        DatasetLabel that = (DatasetLabel) obj;

        if (!_observationId.equals(that._observationId)) return false;
        return _index == that._index;
    }

    /**
     * Computes a hash code value that agrees with the definition of equals.
     */
    public int hashCode() {
        return 37 * _index + _observationId.hashCode();
    }

    /**
     * Gets the dataset label string formed from the observation id and
     * dataset index: <code>obsid-index</code>.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder(_observationId.toString());
        buf.append('-');
        if (_index < 100) {
            buf.append("0");
            if (_index < 10) buf.append("0");
        }
        buf.append(_index);
        return buf.toString();
    }

    /**
     * Gets the observation id.
     */
    public SPObservationID getObservationId() {
        return _observationId;
    }

    /**
     * Gets the index of the dataset.
     */
    public int getIndex() {
        return _index;
    }
}
