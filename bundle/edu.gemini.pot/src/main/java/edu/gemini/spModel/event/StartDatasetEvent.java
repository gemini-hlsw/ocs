//
// $Id: StartDatasetEvent.java 6272 2005-06-02 05:39:33Z shane $
//

package edu.gemini.spModel.event;

import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioParseException;
import edu.gemini.spModel.pio.PioFactory;

/**
 * An event that signifies the beginning of data collection for a particular
 * dataset.
 */
public final class StartDatasetEvent extends ObsExecEvent {
    public static final String DATASET_PARAMSET = "dataset";

    private final Dataset _dataset;

    public StartDatasetEvent(long time, Dataset dataset) {
        super(time, dataset.getLabel().getObservationId());
        _dataset = dataset;
    }

    public StartDatasetEvent(ParamSet paramSet) throws PioParseException {
        super(paramSet);

        final ParamSet datasetParamSet;
        datasetParamSet = paramSet.getParamSet(DATASET_PARAMSET);
        if (datasetParamSet == null) {
            throw new PioParseException("missing '" + DATASET_PARAMSET + "'");
        }
        _dataset = new Dataset(datasetParamSet);
    }

    public Dataset getDataset() {
        return _dataset; // dataset is immutable
    }

    public void doAction(ExecAction action) {
        action.startDataset(this);
    }

    public ParamSet toParamSet(PioFactory factory) {
        final ParamSet paramSet = super.toParamSet(factory);
        paramSet.addParamSet(_dataset.toParamSet(factory));
        return paramSet;
    }

    public boolean equals(Object other) {
        final boolean res = super.equals(other);
        if (!res) return false;

        final StartDatasetEvent that = (StartDatasetEvent) other;
        if (!_dataset.equals(that._dataset)) return false;

        return true;
    }

    public int hashCode() {
        int res = super.hashCode();
        res = 37 * res + _dataset.hashCode();
        return res;
    }

    public String getKind() {
        return "StartDataset";
    }

    public String getName() {
        return "Start Dataset";
    }

    @Override
    public String toStringProperties() {
        return String.format(
            "%s, dataset=%s",
            super.toStringProperties(),
            _dataset
        );
    }
}
