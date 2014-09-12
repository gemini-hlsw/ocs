//
// $Id: DatasetFileUpdate.java 73 2005-09-03 19:10:22Z shane $
//

package edu.gemini.datasetfile;

import edu.gemini.spModel.dataset.DatasetQaState;

import java.util.Date;

/**
 * A template describing an update that should be applied to a dataset.
 * Non-<code>null</code> fields are used to update the corresponding dataset
 * FITS header keys.
 */
public interface DatasetFileUpdate {

    /**
     * Gets the release date for the dataset.  Corresponds to the RELEASE FITS
     * keyword.
     */
    Date getRelease();
    void setRelease(Date releaseDate);

    /**
     * Gets the private header (meta data) flag for the dataset.  Corresponds
     * to the PROP_MD FITS keyword.
     */
    Boolean isHeaderPrivate();
    void setHeaderPrivate(Boolean headerPrivate);

    /**
     * Gets the QA state for the dataset.  Corresponds to the GEMRAWQA and
     * GEMPIREQ FITS keywords.
     */
    DatasetQaState getQaState();
    void setQaState(DatasetQaState qaState);
}
