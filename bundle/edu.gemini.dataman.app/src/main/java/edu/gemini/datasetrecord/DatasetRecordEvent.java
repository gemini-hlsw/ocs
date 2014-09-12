//
// $Id: DatasetRecordEvent.java 58 2005-09-01 02:11:39Z shane $
//

package edu.gemini.datasetrecord;


import edu.gemini.spModel.dataset.DatasetRecord;

import java.io.Serializable;

/**
 * An event describing a change to a
 * {@link edu.gemini.spModel.dataset.DatasetExecRecord} in the database.
 * Delivered to all {@link DatasetRecordListener}s.
 */
public interface DatasetRecordEvent extends Serializable {

    /**
     * Gets the service that generated the event.
     */
    DatasetRecordService getSource();

    /**
     * The old version of the dataset record, if any.  If the old version is
     * <code>null</code> then the dataset is new in the ODB.
     *
     * @return old version of the dataset record, or <code>null</code> if none
     */
    DatasetRecord getOldVersion();

    /**
     * The new version of the dataset record, if any.  If the new version is
     * <code>null</code> then the dataset has been deleted from the ODB.
     *
     * @return new version of the dataset record, or <code>null</code> if none
     */
    DatasetRecord getNewVersion();
}
