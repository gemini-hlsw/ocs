//
// $Id: DsetRecordChangeBuilder.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.impl.trigger;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.obslog.ObsQaLog;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Determines the dataset record updates that occurred (if any) when an
 * observing log component is updated.
 */
final class DsetRecordChangeBuilder implements Serializable {
    private static final long serialVersionUID = 1l;

    private static final Logger LOG = Logger.getLogger(DsetRecordChangeBuilder.class.getName());

    private DsetRecordChangeBuilder() {
    }

    private static SortedMap<DatasetLabel, DatasetRecord> _hash(Collection<DatasetRecord> recs) {
        final SortedMap<DatasetLabel, DatasetRecord> res = new TreeMap<DatasetLabel, DatasetRecord>();
        for (DatasetRecord rec : recs) res.put(rec.getLabel(), rec);
        return res;
    }

    /**
     * Creates a Collection of {@link DsetRecordChange} if the given
     * <code>change</code> warrants it. In other words, if the change is an
     * update to an {@link edu.gemini.spModel.obsrecord.ObsExecRecord} that
     * resulted in a difference to its
     * {@link edu.gemini.spModel.dataset.DatasetRecord}s.
     *
     * @param change arbitrary change that occurred in the database
     *
     * @return a newly minted collection of {@link edu.gemini.spModel.dataset.DatasetRecord if the given
     * <code>change</code> was to an {@link edu.gemini.spModel.obsrecord.ObsExecRecord } and contained updates
     * to one or more {@link edu.gemini.spModel.dataset.DatasetExecRecord}s
     */
    public static Collection<DsetRecordChange> create(SPCompositeChange change) {
        // Make sure this is an instance of the data object being updated.
        final String propName = SPUtil.getDataObjectPropertyName();
        if (!propName.equals(change.getPropertyName())) {
            LOG.log(Level.FINE, "* Wasn't a data object update");
            return null;
        }

        final ISPNode node = change.getModifiedNode();
        final ISPContainerNode parent = node.getParent();
        if (!(parent instanceof ISPObservation)) {
            LOG.log(Level.FINE, "* Wasn't a change to an observation log.");
            return null;
        }
        final ISPObservation obs = (ISPObservation) parent;

        final ObsLog oldLog;
        final ObsLog newLog;
        if (node instanceof ISPObsQaLog) {
            final ObsQaLog oldQaLog = (ObsQaLog) change.getOldValue();
            final ObsQaLog newQaLog = (ObsQaLog) change.getNewValue();
            final ISPObsExecLog ispObsExecLog = obs.getObsExecLog();
            if (ispObsExecLog == null) {
                LOG.log(Level.WARNING, "* Change to obs QA log without a matching obs exec log");
                return null;
            }
            final ObsExecLog execLog = (ObsExecLog) ispObsExecLog.getDataObject();

            oldLog = new ObsLog((ISPObsQaLog) node, oldQaLog, ispObsExecLog, execLog);
            newLog = new ObsLog((ISPObsQaLog) node, newQaLog, ispObsExecLog, execLog);

        } else if (node instanceof ISPObsExecLog) {
            final ObsExecLog oldExecLog = (ObsExecLog) change.getOldValue();
            final ObsExecLog newExecLog = (ObsExecLog) change.getNewValue();
            final ISPObsQaLog ispObsQaLog = obs.getObsQaLog();
            if (ispObsQaLog == null) {
                LOG.log(Level.WARNING, "* Change to obs exec log without a matching obs qa log");
                return null;
            }
            final ObsQaLog qaLog = (ObsQaLog) ispObsQaLog.getDataObject();

            oldLog = new ObsLog(ispObsQaLog, qaLog, (ISPObsExecLog) node, oldExecLog);
            newLog = new ObsLog(ispObsQaLog, qaLog, (ISPObsExecLog) node, newExecLog);
        } else {
            LOG.log(Level.FINE, "* Wasn't a change to an observation log.");
            return null;
        }
        final List<DatasetRecord> oldRecords = oldLog.getAllDatasetRecords();
        final List<DatasetRecord> newRecords = newLog.getAllDatasetRecords();


        // Compare the QA States for each dataset.  If any of them differ, then
        // the trigger should be fired.
        final List<DsetRecordChange> updates = new ArrayList<DsetRecordChange>();

        final SortedMap<DatasetLabel, DatasetRecord> oldMap = _hash(oldRecords);

        for (DatasetRecord newRec : newRecords) {
            final DatasetRecord oldRec = oldMap.remove(newRec.getLabel());
            if (!newRec.equals(oldRec)) {
                updates.add(new DsetRecordChange(oldRec, newRec));
            }
        }

        // Anything left in the oldMap will be datasets that are not in the
        // new version of the observing record.  In other words, deleted
        // datasets.
        for (DatasetRecord oldRec : oldMap.values()) {
            updates.add(new DsetRecordChange(oldRec, null));
        }

        if (updates.size() == 0) return null;
        return updates;
    }
}
