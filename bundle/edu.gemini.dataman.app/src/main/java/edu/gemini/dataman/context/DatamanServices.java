//
// $Id: DatamanServices.java 129 2005-09-14 15:40:53Z shane $
//

package edu.gemini.dataman.context;

import edu.gemini.datasetfile.DatasetFileService;
import edu.gemini.datasetrecord.DatasetRecordService;
import edu.gemini.pot.spdb.IDBDatabaseService;

import java.util.Set;

/**
 * Collection of services that the Dataman app depends upon.  References to
 * the services returned by this interface should not be held onto for long
 * periods since the services may come and go.
 */
public interface DatamanServices {

    /**
     * Gets the DatasetFileService, if available.  The DatasetFileService is
     * used to retrieve and update datasets in the working storage area.  This
     * service may disappear at any moment and therefore the reference returned
     * by this method should not be held onto permanently.
     *
     * @return reference to the DatasetFileService, if available;
     * <code>null</code> otherwise
     */
    DatasetFileService getDatasetFileService();

    /**
     * Gets the DatasetRecordService, if available.  The DatasetRecordService is
     * used to retrieve and update datasets in the working storage area.  This
     * service may disappear at any moment and therefore the reference returned
     * by this method should not be held onto permanently.
     *
     * @return reference to the DatasetRecordService, if available;
     * <code>null</code> otherwise
     */
    DatasetRecordService getDatasetRecordService();

    /**
     * Gets the collection of database references that have been discovered.
     * Databases may come and go without notice so these references should not
     * be held onto permanently.
     *
     * @return Set of database references that have been discovered on the
     * network
     */
    Set<IDBDatabaseService> getDatabases();
}
