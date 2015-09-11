package edu.gemini.spModel.dataset;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.PioParseException;

import java.io.Serializable;

/**
 * There is one DatasetExecRecord for each {@link Dataset} in an observation.
 * It references the {@link Dataset} itself, along with associated attributes.
 */
public final class DatasetExecRecord implements Comparable, Serializable {

    public static final DatasetExecRecord[] EMPTY_ARRAY = new DatasetExecRecord[0];

    public static final String PARAM_SET = "datasetRecord";

    public static final String SYNC_TIME_PARAM          = "syncTime";
    public static final String DATASET_FILE_STATE_PARAM = "datasetFileState";
    public static final String GSA_STATE_PARAM          = "gsaState";

    private static final long serialVersionUID = 5L;

    public final Dataset dataset;
    public final long syncTime;
    public final DatasetFileState fileState;
    public final GsaState gsaState;

    /**
     * Constructs with the associated dataset, setting the attributes to
     * default values:
     * <p/>
     * <ul>
     * <li>{@link DatasetQaState#UNDEFINED Dataset QA State: UNDEFINED}</li>
     * </ul>
     *
     * @param dataset the associated dataset
     */
    public DatasetExecRecord(Dataset dataset) {
        this(dataset, -1, DatasetFileState.TENTATIVE, GsaState.NONE);
    }

    public DatasetExecRecord(Dataset dataset, long syncTime, DatasetFileState fileState, GsaState gsaState) {
        if (dataset == null) throw new NullPointerException();
        if (fileState == null) throw new NullPointerException();
        if (gsaState  == null) throw new NullPointerException();

        this.dataset   = dataset;
        this.syncTime  = syncTime;
        this.fileState = fileState;
        this.gsaState  = gsaState;
    }

    /**
     * Constructs from the given ParamSet.  The ParamSet must define the
     * dataset that this record contains.
     *
     * @param pSet ParamSet describing the DatasetRecord to create
     * @throws PioParseException if <code>pSet</code> is missing or has an
     *                           invalid nested dataset ParamSet
     */
    public DatasetExecRecord(ParamSet pSet) throws PioParseException {
        final ParamSet datasetParamSet = pSet.getParamSet(Dataset.PARAM_SET);
        if (datasetParamSet == null) {
            throw new PioParseException("missing '" + Dataset.PARAM_SET + "'");
        }
        dataset  = new Dataset(datasetParamSet);
        syncTime = Pio.getLongValue(pSet, SYNC_TIME_PARAM, 0);

        String str = Pio.getValue(pSet, DATASET_FILE_STATE_PARAM, DatasetFileState.MISSING.toString());
        final DatasetFileState dfs = DatasetFileState.parseType(str);
        fileState  = (dfs == null) ? DatasetFileState.MISSING : dfs;

        str       = Pio.getValue(pSet, GSA_STATE_PARAM, GsaState.NONE.toString());
        final GsaState gs = GsaState.parseType(str);
        gsaState  = (gs == null) ? GsaState.NONE : gs;
    }

    public Dataset getDataset() {
        return dataset; // just return it since Dataset is immutable
    }

    public DatasetLabel getLabel() {
        return dataset.getLabel();
    }

    public DatasetFileState getDatasetFileState() {
        return fileState;
    }

    public DatasetExecRecord withFileState(DatasetFileState dfs) {
        return (dfs == this.fileState) ? this : new DatasetExecRecord(dataset, syncTime, dfs, gsaState);
    }

    public GsaState getGsaState() {
        return gsaState;
    }

    public DatasetExecRecord withGsaState(GsaState gs) {
        return (gs == this.gsaState) ? this : new DatasetExecRecord(dataset, syncTime, fileState, gs);
    }

    public long getSyncTime() {
        return syncTime;
    }

    public DatasetExecRecord withSyncTime(long syncTime) {
        return (syncTime == this.syncTime) ? this : new DatasetExecRecord(dataset, syncTime, fileState, gsaState);
    }

    public synchronized ParamSet toParamSet(PioFactory factory) {
        final ParamSet paramSet = factory.createParamSet(PARAM_SET);

        Pio.addLongParam(factory, paramSet, SYNC_TIME_PARAM, syncTime);
        Pio.addParam(factory, paramSet, DATASET_FILE_STATE_PARAM, fileState.name());
        Pio.addParam(factory, paramSet, GSA_STATE_PARAM, gsaState.name());

        paramSet.addParamSet(dataset.toParamSet(factory));
        return paramSet;
    }

    public int compareTo(Object o) {
        final DatasetExecRecord that = (DatasetExecRecord) o;

        int res;
        res = dataset.compareTo(that.dataset);
        if (res != 0) return res;

        if (syncTime < that.syncTime) {
            return -1;
        } else if (syncTime > that.syncTime) {
            return 1;
        }

        res = fileState.compareTo(that.fileState);
        if (res != 0) return res;

        res = gsaState.compareTo(that.gsaState);
        return res;
    }

    public boolean equals(Object other) {
        if (other == null) return false;
        if (other.getClass() != getClass()) return false;

        DatasetExecRecord that = (DatasetExecRecord) other;
        if (!dataset.equals(that.dataset)) return false;

        if (syncTime != that.syncTime) return false;
        if (!fileState.equals(that.fileState)) return false;
        return (gsaState.equals(that.gsaState));
    }

    public int hashCode() {
        int res = dataset.hashCode();

        res = 37 * res + (int) (syncTime ^ (syncTime >>> 32));
        res = 37 * res + fileState.hashCode();
        res = 37 * res + gsaState.hashCode();
        return res;
    }
}
