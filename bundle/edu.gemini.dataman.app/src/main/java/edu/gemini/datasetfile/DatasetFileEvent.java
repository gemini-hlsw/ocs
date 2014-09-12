//
// $Id: DatasetFileEvent.java 144 2005-09-26 21:45:51Z shane $
//

package edu.gemini.datasetfile;

import edu.gemini.spModel.dataset.DatasetLabel;

import java.io.File;

/**
 * An event fired when a dataset is created or updated.
 */
public interface DatasetFileEvent {
    /**
     * Gets the source of the event, a DatasetFileService.
     */
    DatasetFileService getSource();

    /**
     * Gets the actual dataset file that was updated or created.  Will be
     * <code>null</code> if a problem dataset file was discovered, or if a
     * dataset was deleted.
     */
    DatasetFile getDatasetFile();

    /**
     * Gets the label of the dataset associated with the event.
     */
    DatasetLabel getLabel();

    /**
     * Gets the file containing the dataset to which the event applies.
     */
    File getFile();

    /**
     * Gets a message describing the dataset event.  When there is a problem
     * parsing a dataset, this message will contain the details of the problem.
     */
    String getMessage();
}
