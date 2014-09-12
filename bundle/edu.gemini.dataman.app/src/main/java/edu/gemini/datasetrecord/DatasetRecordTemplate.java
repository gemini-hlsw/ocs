//
// $Id: DatasetRecordTemplate.java 135 2005-09-14 23:27:17Z shane $
//

package edu.gemini.datasetrecord;

import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.spModel.dataset.GsaState;
import edu.gemini.spModel.dataset.DatasetFileState;

import java.io.Serializable;

/**
 * A template describing an update that should be applied to a
 * {@link edu.gemini.spModel.dataset.DatasetExecRecord} in the database.
 * Non-<code>null</code> fields are used to update the corresponding values
 * in the record.
 */
public interface DatasetRecordTemplate extends Serializable {

    /**
     * Gets the comment associated with the dataset.
     */
    String getComment();
    void setComment(String comment);

    /**
     * Gets the QA state of the dataset record.
     */
    DatasetQaState getQaState();
    void setQaState(DatasetQaState state);

    /**
     * Gets the sync time of the dataset record.
     */
    Long getSyncTime();
    void setSyncTime(Long syncTime);

    /**
     * Gets the dataset file state.
     */
    DatasetFileState getDatasetFileState();
    void setDatasetFileState(DatasetFileState state);

    /**
     * Gets the dataflow state.
     */
    GsaState getGsaState();
    void setGsaState(GsaState state);
}
