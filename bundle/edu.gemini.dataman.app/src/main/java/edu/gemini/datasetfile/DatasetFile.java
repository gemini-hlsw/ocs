//
// $Id: DatasetFile.java 281 2006-02-13 17:52:21Z shane $
//

package edu.gemini.datasetfile;

import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.spModel.obsclass.ObsClass;


import java.io.File;
import java.util.Date;

/**
 * Represents a dataset file and provides easy access to the pertinent header
 * information.  Bridges the spModel representation of the dataset and the
 * storage of associated information in the FITS header.
 */
public interface DatasetFile {

    /**
     * Gets the fundamental dataset information (its label, the filename, and
     * the timestamp).
     *
     * @return Dataset identification information associated with this file
     */
    Dataset getDataset();

    /**
     * Gets the actual File object.
     *
     * @return File whose path points to the dataset file
     */
    File getFile();

    /**
     * Gets the obs class associated with the dataset.
     */
    ObsClass getObsClass();

    /**
     * Gets the parsed dataset file name.
     */
    DatasetFileName getDatasetFileName();

    /**
     * Gets the release date for this dataset.  This is determined by the
     * dataset's RELEASE header item.
     *
     * @return Gemini specified date at which the dataset's proprietary period
     * ends
     */
    Date getRelease();

    /**
     * Gets the flag indicating whether the header is to remain private until
     * the release date.  This is determined by the PROP_MD header item.  If
     * this item is not present, <code>false</code> is assumed.
     *
     * @return <code>true</code> if the header should remain private until
     * the release date; <code>false</code> otherwise
     */
    boolean isHeaderPrivate();

    /**
     * Gets the current QA state information for the dataset.  Maps to the
     * RAWGEMQA and RAWPIREQ FITS header items.
     *
     * @return QA state of this dataset
     */
    DatasetQaState getQaState();

    /**
     * Gets the last modification time of the file at the time this object
     * was created.
     *
     * @return associated File modification time when this object was created
     */
    long lastModified();

    /**
     * Initializes a {@link DatasetFileUpdate} based upon the current
     * state of this DatasetFile.
     *
     * @return DatasetFileUpdateTemplate initialized with the QA state and
     * release date of this DatasetFile
     */
    DatasetFileUpdate toUpdate();
}
