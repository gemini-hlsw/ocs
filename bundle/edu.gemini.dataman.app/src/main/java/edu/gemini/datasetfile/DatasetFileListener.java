//
// $Id: DatasetFileListener.java 163 2005-10-01 23:26:20Z shane $
//

package edu.gemini.datasetfile;

/**
 * An interface to be implemented by clients of the {@link DatasetFileService}
 * who wish to receive notification of significant events in dataset files.
 */
public interface DatasetFileListener {
    void datasetDeleted(DatasetFileEvent evt);
    void datasetAdded(DatasetFileEvent evt);
    void datasetModified(DatasetFileEvent evt);
    void badDatasetFound(DatasetFileEvent evt);
}
