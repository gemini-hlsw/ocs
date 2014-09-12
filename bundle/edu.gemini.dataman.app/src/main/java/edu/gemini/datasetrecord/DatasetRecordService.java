//
// $Id: DatasetRecordService.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord;

import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.dataflow.GsaAspect;
import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.dataset.DatasetExecRecord;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetRecord;

/**
 * The DatasetRecordService provides a simplified interface for working with
 * the observing database.  Namely, methods for fetchiing, updating, and
 * watching for changes are provided.
 */
public interface DatasetRecordService {

    /**
     * Fetches the {@link edu.gemini.spModel.dataset.DatasetExecRecord} associated with the given
     * <code>label</code>, if it exists.
     *
     * @param label label of the DatasetRecord to fetch
     *
     * @return DatasetRecord corresponding to the <code>label</code> if it
     * exists; <code>null</code> otherwise
     */
    DatasetExecRecord fetch(DatasetLabel label)
            throws InterruptedException;

    GsaAspect fetchGsaAspect(SPProgramID progId)
            throws InterruptedException;

    /**
     * Updates the {@link edu.gemini.spModel.dataset.DatasetExecRecord} identified by the given
     * <code>label</code>.  If there is no such record, nothing is done.
     *
     * <p>Fields of the DatasetRecord are updated according to the
     * <code>update</code> template. It contains non-<code>null</code>
     * fields for each DatasetRecord field that should be changed.
     *
     * <p>The <code>precondition</code> is an optional template that describes
     * the state of the {@link edu.gemini.spModel.dataset.DatasetExecRecord} as it must exist before the
     * update is applied.  Each non-<code>null</code> field of the
     * <code>precondition</code> is checked against the current state of the
     * {@link edu.gemini.spModel.dataset.DatasetExecRecord} before the update is applied.  If any value
     * does not match the current state of the record, then the update is not
     * applied.
     *
     * @param label label of the {@link edu.gemini.spModel.dataset.DatasetExecRecord} to update
     *
     * @param update a template containing the updates to apply
     *
     * @param precondition an optional template containing the preconditions
     * that must be met in order to apply the update; if <code>null</code>
     * there are no preconditions
     *
     * @return the {@link edu.gemini.spModel.dataset.DatasetExecRecord} as it exists after the update;
     * <code>null</code> if the matching DatasetRecord could not be found
     */
    DatasetRecord update(DatasetLabel label, DatasetRecordTemplate update,
                         DatasetRecordTemplate precondition)
            throws InterruptedException;

    /**
     * Updates the {@link edu.gemini.spModel.dataset.DatasetExecRecord} identified by the given
     * <code>label</code>.  If there is no such record, an attempt is made
     * to create the record based upon the information supplied.
     *
     * <p>Fields of the DatasetRecord are updated according to the
     * <code>update</code> template. It contains non-<code>null</code>
     * fields for each DatasetRecord field that should be changed.  If the
     * record must be created, <code>null</code> fields in the
     * <code>update</code> are left unset.
     *
     * <p>The <code>precondition</code> is an optional template that describes
     * the state of the {@link edu.gemini.spModel.dataset.DatasetExecRecord} as it must exist before the
     * update is applied.  Each non-<code>null</code> field of the
     * <code>precondition</code> is checked against the current state of the
     * {@link edu.gemini.spModel.dataset.DatasetExecRecord} before the update is applied.  If any value
     * does not match the current state of the record, then the update is not
     * applied.  If the record must be created, the precondition is ignored.
     *
     * @param dataset core dataset information to use in the even that the
     * record must be created
     *
     * @param update a template containing the updates to apply
     *
     * @param precondition an optional template containing the preconditions
     * that must be met in order to apply the update; if <code>null</code>
     * there are no preconditions
     *
     * @return the {@link edu.gemini.spModel.dataset.DatasetExecRecord} as it exists after the update;
     * <code>null</code> if the matching DatasetRecord could not be found and
     * could not be created
     */
    DatasetRecord updateOrCreate(Dataset dataset, DatasetRecordTemplate update,
                                 DatasetRecordTemplate precondition)
            throws InterruptedException;

    /**
     * Adds a listener to be notified when there are {@link edu.gemini.spModel.dataset.DatasetExecRecord}
     * updates.
     */
    void addListener(DatasetRecordListener listener);

    /**
     * Removes a listener so that it will no longer receive notifications.
     */
    void removeListener(DatasetRecordListener listener);
}
