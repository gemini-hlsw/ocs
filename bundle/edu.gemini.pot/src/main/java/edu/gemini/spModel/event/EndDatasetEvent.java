//
// $Id: EndDatasetEvent.java 6654 2005-09-29 22:52:52Z shane $
//

package edu.gemini.spModel.event;

import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioParseException;

import java.text.ParseException;

/**
 * An event that signifies the end of data collection for a particular
 * dataset.
 */
public final class EndDatasetEvent extends ObsExecEvent {

    public static final String DATASET_LABEL_PARAM = "datasetLabel";

    private final DatasetLabel _datasetLabel;

    /**
     * Constructs with the time of the event and the dataset label, which
     * may not be <code>null</code>.
     *
     * @param time time of the event
     * @param label non-null dataset label
     */
    public EndDatasetEvent(long time, DatasetLabel label) {
        super(time, label.getObservationId());
        _datasetLabel = label;
    }

    public EndDatasetEvent(ParamSet paramSet) throws PioParseException {
        super(paramSet);

        String datasetLabelStr = Pio.getValue(paramSet, DATASET_LABEL_PARAM);
        if (datasetLabelStr == null) {
            throw new PioParseException("missing '" + DATASET_LABEL_PARAM + "'");
        }

        try {
            _datasetLabel = new DatasetLabel(datasetLabelStr);
        } catch (ParseException ex) {
            throw new PioParseException("invalid dataset label: " + datasetLabelStr, ex);
        }
    }

    public DatasetLabel getDatasetLabel() {
        return _datasetLabel; // DatasetLabel is immutable
    }

    public void doAction(ExecAction action) {
        action.endDataset(this);
    }

    public ParamSet toParamSet(PioFactory factory) {
        final ParamSet paramSet = super.toParamSet(factory);
        Pio.addParam(factory, paramSet, DATASET_LABEL_PARAM, _datasetLabel.toString());
        return paramSet;
    }

    public boolean equals(Object other) {
        final boolean res = super.equals(other);
        if (!res) return false;

        final EndDatasetEvent that = (EndDatasetEvent) other;
        if (!_datasetLabel.equals(that._datasetLabel)) return false;

        return true;
    }

    public int hashCode() {
        int res = super.hashCode();
        res = 37 * res + _datasetLabel.hashCode();
        return res;
    }

    public String getKind() {
        return "EndDataset";
    }

    public String getName() {
        return "End Dataset";
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EndDatasetEvent{");
        sb.append("timestamp=").append(getTimestamp()).append(", ");
        sb.append("datasetLabel=").append(_datasetLabel);
        sb.append('}');
        return sb.toString();
    }
}
