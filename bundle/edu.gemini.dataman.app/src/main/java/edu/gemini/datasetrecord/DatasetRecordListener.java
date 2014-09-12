//
// $Id: DatasetRecordListener.java 58 2005-09-01 02:11:39Z shane $
//

package edu.gemini.datasetrecord;

import java.io.Serializable;

/**
 * An interface implemented by clients that wish to be notified of events
 * related to {@link edu.gemini.spModel.dataset.DatasetExecRecord}s in the
 * database.
 *
 * <p>When used in an OSGi environment, the client should publish a service
 * implementing this interface.  The
 * {@link DatasetRecordService} will automatically be
 * updated to deliver events to this listener.  If not used in an OSGi
 * environment, the client is responsible for adding itself as a listener
 * on the {@link DatasetRecordService implementation}.
 */
public interface DatasetRecordListener extends Serializable {

    /**
     * Listener method called for each update to
     * {@link edu.gemini.spModel.dataset.DatasetExecRecord}s in the database.
     *
     * @param evt event object describing the update that has occured.
     */
    void datasetModified(DatasetRecordEvent evt);
}
