//
// $Id: DsetRecordUpdateRequest.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.impl.store;

import edu.gemini.datasetrecord.DatasetRecordTemplate;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.Dataset;

import java.io.Serializable;

/**
 * Encapsulation of request information for updating (or creating) a
 * DatasetRecord.
 */
final class DsetRecordUpdateRequest implements Serializable {
    private DatasetLabel _label;
    private Dataset _dataset;
    private DatasetRecordTemplate _request;
    private DatasetRecordTemplate _precond;

    /**
     * Creates a request that may only be used for updating an existing
     * DatasetRecord (since the Dataset itself is not supplied).  Each non-null
     * value in the provided <code>request</code> template is applied to the
     * existing DatasetRecord provided that all values (if any) in the supplied
     * <code>precond</code> precondition are currently set on the record.
     *
     * @param label label of the dataset whose corresponding record should be
     * updated
     *
     * @param request changes that should be made to the existing record
     *
     * @param precond values that the existing record must have in order to
     * complete the update
     */
    public DsetRecordUpdateRequest(DatasetLabel label,
                                   DatasetRecordTemplate request,
                                   DatasetRecordTemplate precond) {
        if (label == null) {
            throw new NullPointerException("Missing dataset label");
        }
        if (request == null) {
            throw new NullPointerException("Missing change request");
        }
        _label   = label;
        _request = request;
        _precond = precond;
    }

    /**
     * Creates a request that may only be used for updating an existing
     * DatasetRecord or creating a new one.  Each non-null
     * value in the provided <code>request</code> template is applied to the
     * existing DatasetRecord provided that all values (if any) in the supplied
     * <code>precond</code> precondition are currently set on the record.
     *
     * @param dataset dataset information to use in creating the DatasetRecord
     * if necessary
     *
     * @param request changes that should be made to the existing record
     *
     * @param precond values that the existing record must have in order to
     * complete the update
     */
    public DsetRecordUpdateRequest(Dataset dataset,
                                   DatasetRecordTemplate request,
                                   DatasetRecordTemplate precond) {
        this(dataset.getLabel(), request, precond);
        _dataset = dataset;
    }

    public DatasetLabel getLabel() {
        return _label;
    }

    public Dataset getDataset() {
        return _dataset;
    }

    public DatasetRecordTemplate getRequest() {
        return _request;
    }

    public DatasetRecordTemplate getPrecond() {
        return _precond;
    }
}
