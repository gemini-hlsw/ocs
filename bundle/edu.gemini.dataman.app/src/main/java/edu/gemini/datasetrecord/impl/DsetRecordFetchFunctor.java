//
// $Id: DsetRecordFetchFunctor.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.impl;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBParallelFunctor;
import edu.gemini.pot.spdb.IDBFunctor;
import edu.gemini.spModel.dataset.DatasetExecRecord;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obsrecord.ObsExecRecord;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Functor used to fetch a dataset record.
 */
final class DsetRecordFetchFunctor extends DBAbstractFunctor implements IDBParallelFunctor {
    private static final Logger LOG = Logger.getLogger(DsetRecordFetchFunctor.class.getName());

    private DatasetLabel _label;
    private DatasetExecRecord _result;

    public DsetRecordFetchFunctor(DatasetLabel label) {
        if (label == null) throw new NullPointerException();
        _label = label;
    }

    public DatasetExecRecord getRecord() {
        return _result;
    }

    @SuppressWarnings("unchecked")
    public void execute(IDBDatabaseService idbDatabase, ISPNode ispNode, Set<Principal> principals) {
        LOG.info("DatasetRecordFetchFunctor.execute()");
        final SPObservationID obsId = _label.getObservationId();
        final ISPObservation obs = idbDatabase.lookupObservationByID(obsId);
        if (obs == null) return;

        final ISPObsExecLog lc = obs.getObsExecLog();
        if (lc == null) return;
        final ObsExecRecord rec = ((ObsExecLog) lc.getDataObject()).getRecord();
        _result = rec.getDatasetExecRecord(_label);
        LOG.info("DatasetRecordFetchFunctor.execute() done");
    }

    public void mergeResults(Collection<IDBFunctor> collection) {
        DatasetExecRecord result = null;
        for (IDBFunctor f : collection) {
            DsetRecordFetchFunctor ff = (DsetRecordFetchFunctor) f;
            result = ff.getRecord();
            if (result != null) break;
        }
        _result = result;
    }
}
