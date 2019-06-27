//
// $Id: DatasetCompleteEvent.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.pot.sp.SPObservationID;

/**
 * Specialized <tt>{@link SessionEvent}</tt> used to indicate that a dataset
 * has been collected and completed.
 */
public final class DatasetCompleteEvent extends SessionEvent {

    // The dataset label
    private final String _dataLabel;

    // The matching filename
    private final String _fileName;

    /**
     * Specialized <tt>{@link SessionEvent}</tt> used to indicate that a dataset
     * has been completed and saved.
     *
     * @param src the source of the event
     * @param observationID the observation the dataset belongs to
     * @param dataLabel the dataset label for the completed observation
     * @param fileName the filename associated with this dataset
     */
    public DatasetCompleteEvent(Object src, SPObservationID observationID, String dataLabel, String fileName) {
        super(src, observationID, EventMsg.DATASET_COMPLETE);
        if (dataLabel == null) throw new NullPointerException();
        _dataLabel = dataLabel;
        _fileName = fileName;
    }

    /**
     * Returns the completed dataset label
     * @return the data label associated with this event as a String
     */
    public String getDataLabel() {
        return _dataLabel;
    }

    /**
     * Returns the completed dataset filename
     * @return the file name associated with this event as a String
     */
    public String getFileName() {
        return _fileName;
    }
}
