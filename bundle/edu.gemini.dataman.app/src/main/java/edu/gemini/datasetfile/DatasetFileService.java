//
// $Id: DatasetFileService.java 85 2005-09-05 21:32:57Z shane $
//

package edu.gemini.datasetfile;

import edu.gemini.spModel.dataset.DatasetQaState;

import java.io.IOException;
import java.util.Date;

/**
 * A service for obtaining and updating {@link DatasetFile}s.
 */
public interface DatasetFileService {

    /**
     * Fetches the {@link DatasetFile} with the given name
     *
     * @param filename name of the file to fetch
     *
     * @return corresponding {@link DatasetFile}, if it exists;
     * <code>null</code> otherwise
     *
     * @throws DatasetFileException if the dataset is readable, but invalid
     * for some reason (missing required keywords such as the dataset label
     * for example)
     *
     * @throws IOException if there is a problem reading the dataset from disk
     *
     * @throws InterruptedException if interrupted while attempting to place
     * a file lock on the file
     */
    DatasetFile fetch(String filename)
            throws IOException, DatasetFileException, InterruptedException;

    /**
     * Updates the dataset using the given <code>update</code> as a template.
     * Non-<code>null</code> fields in the in <code>update</code> are applied
     * to the dataset.
     *
     * @param filename name of the file to update
     * @param update update(s) to apply
     *
     * @return {@link DatasetFile} representing the dataset after the update
     *
     * @throws DatasetFileException if the dataset is readable/writable, but
     * invalid for some reason
     *
     * @throws IOException if there is a problem writing to the dataset on
     * disk
     *
     * @throws InterruptedException if interrupted while attempting to place
     * a file lock on the file
     */
    DatasetFile update(String filename, DatasetFileUpdate update)
            throws IOException, DatasetFileException, InterruptedException;

    /**
     * Updates the dataset to have the given <code>newQaState</code>.  This
     * is a convenience method for calling {@link #update} with a template
     * containing only the {@link DatasetQaState}.
     *
     * @param filename name of the file to update
     * @param newQaState new QA State to apply to the dataset
     *
     * @return {@link DatasetFile} representing the dataset after the update
     *
     * @throws DatasetFileException if the dataset is readable/writable, but
     * invalid for some reason
     *
     * @throws IOException if there is a problem writing to the dataset on
     * disk
     *
     * @throws InterruptedException if interrupted while attempting to place
     * a file lock on the file
     */
    DatasetFile updateQaState(String filename, DatasetQaState newQaState)
            throws IOException, DatasetFileException, InterruptedException;

    /**
     * Updates the dataset to have the given <code>releaseDate</code>.  This
     * is a convenience method for calling {@link #update} with a template
     * containing only the release date.
     *
     * @param filename name of the file to update
     * @param releaseDate new release date to apply to the dataset
     * @param headerPrivate whether the header should be marked private
     *
     * @return {@link DatasetFile} representing the dataset after the update
     *
     * @throws DatasetFileException if the dataset is readable/writable, but
     * invalid for some reason
     *
     * @throws IOException if there is a problem writing to the dataset on
     * disk
     *
     * @throws InterruptedException if interrupted while attempting to place
     * a file lock on the file
     */
    DatasetFile updateRelease(String filename, Date releaseDate, Boolean headerPrivate)
            throws IOException, DatasetFileException, InterruptedException;

    /**
     * Adds a listener of {@link DatasetFile} updates
     */
    void addFileListener(DatasetFileListener listener);

    /**
     * Removes a listener of {@link DatasetFile} updates.
     */
    void removeFileListener(DatasetFileListener listener);
}
