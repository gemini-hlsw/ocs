package edu.gemini.spModel.dataset;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioParseException;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;
import java.util.Date;
import java.text.ParseException;

/**
 * The Dataset class represents a raw dataset produced by an instrument.  This
 * class is immutable.
 */
public final class Dataset implements Comparable, Serializable {
    public static final Dataset[] EMPTY_ARRAY = new Dataset[0];

    public static final String PARAM_SET           = "dataset";
    public static final String DATASET_LABEL_PARAM = "datasetLabel";
    public static final String DHS_FILENAME_PARAM  = "dhsFilename";
    public static final String TIMESTAMP_PARAM     = "timestamp";

    /**
     * The label of ths dataset.
     */
    private final DatasetLabel _label;

    /**
     * The associated DHS file label.
     */
    private final String _dhsFilename;

    /**
     * When the dataset was created.
     */
    private final long _timestamp;

    /**
     * Constructs with all required information.  This class is immutable
     * so no changes can be made after construction.
     *
     * @param label dataset label
     * @param dhsFilename the corresponding file
     *
     * @throws NullPointerException if any field is <code>null</code>
     */
    public Dataset(DatasetLabel label, String dhsFilename, long timestamp) {
        if (label       == null) throw new NullPointerException("label");
        if (dhsFilename == null) throw new NullPointerException("dhsFilename");
        _label       = label;
        _dhsFilename = dhsFilename;
        _timestamp   = timestamp;
    }

    public Dataset(ParamSet paramSet) throws PioParseException {
        // Parse out the dataset label.
        final String datasetLabelStr = Pio.getValue(paramSet, DATASET_LABEL_PARAM);
        if (datasetLabelStr == null) {
            throw new PioParseException("missing '" + DATASET_LABEL_PARAM + "'");
        }
        try {
            _label = new DatasetLabel(datasetLabelStr);
        } catch (ParseException ex) {
            throw new PioParseException("invalid dataset label: " + datasetLabelStr, ex);
        }

        // Parse the DHS filename.
        _dhsFilename = Pio.getValue(paramSet, DHS_FILENAME_PARAM);
        if (_dhsFilename == null) {
            throw new PioParseException("missing '" + DHS_FILENAME_PARAM + "'");
        }

        // Parse the timestamp
        _timestamp = Pio.getLongValue(paramSet, TIMESTAMP_PARAM, -1);
        if (_timestamp == -1) {
            throw new PioParseException("missing or illegal '" + TIMESTAMP_PARAM + "'");
        }
    }

    public ParamSet toParamSet(PioFactory factory) {
        final ParamSet paramSet = factory.createParamSet(PARAM_SET);

        Pio.addParam(factory, paramSet, DATASET_LABEL_PARAM, _label.toString());
        Pio.addParam(factory, paramSet, DHS_FILENAME_PARAM, _dhsFilename);
        Pio.addLongParam(factory, paramSet, TIMESTAMP_PARAM, _timestamp);

        return paramSet;
    }

    /**
     * Provides a natural ordering.
     */
    public int compareTo(Object obj) {
        final Dataset that = (Dataset) obj;

        int res = _label.compareTo(that._label);
        if (res != 0) return res;

        if (_timestamp != that._timestamp) {
            return (_timestamp < that._timestamp) ? -1 : 1;
        }

        return _dhsFilename.compareTo(that._dhsFilename);
    }

    /**
     * Provides semantic equality.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Dataset)) return false;

        final Dataset that = (Dataset) obj;
        return _label.equals(that._label) &&
               (_timestamp == that._timestamp) &&
               _dhsFilename.equals(that._dhsFilename);
    }

    /**
     * Computes a hash code value that agrees with the definition of equals.
     */
    public int hashCode() {
        int res = _label.hashCode();
        res = 37 * res + ((int)(_timestamp^(_timestamp>>>32)));
        res = 37 * res + _dhsFilename.hashCode();
        return res;
    }

    public String toString() {
        return getClass().getName() + " [" +
                "time=" + new Date(_timestamp) +
                ", label=" + _label +
                ", dhsFilename=" + _dhsFilename +
             "]";
    }

    /**
     * Gets the dataset label formed from the observation id and dataset
     * index: <code>obs id-index</code>.
     */
    public DatasetLabel getLabel() {
        return _label;
    }

    /**
     * Gets the observation id.
     */
    public SPObservationID getObservationId() {
        return _label.getObservationId();
    }

    /**
     * Gets the index of the dataset.
     */
    public int getIndex() {
        return _label.getIndex();
    }

    /**
     * Gets the DHS filename for the dataset.
     */
    public String getDhsFilename() {
        return _dhsFilename;
    }

    /**
     * Gets the time at which the dataset was created.
     */
    public long getTimestamp() {
        return _timestamp;
    }
}
