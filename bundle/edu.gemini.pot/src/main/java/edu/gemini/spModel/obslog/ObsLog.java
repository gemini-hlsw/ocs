package edu.gemini.spModel.obslog;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.dataset.*;
import edu.gemini.spModel.obsrecord.ObsQaRecord;
import edu.gemini.spModel.obsrecord.ObsExecRecord;
import edu.gemini.spModel.obsrecord.ObsVisit;

import java.util.List;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * (ISPObsQaLog, ISPObsExecLog).
 */
public final class ObsLog {
    private static final Logger LOG = Logger.getLogger(ObsLog.class.getName());

    /**
     * Extracts the observing log information from the given observation if
     * it is defined.
     *
     * @return an observing log if defined, <code>null</code> otherwise
     */
    public static ObsLog getIfExists(ISPObservation obs) {
        final ISPObsQaLog qaLog;
        final ISPObsExecLog execLog;
        obs.getProgramReadLock();
        try {
            qaLog   = obs.getObsQaLog();
            execLog = obs.getObsExecLog();
            return (qaLog == null) || (execLog == null) ? null : new ObsLog(qaLog, execLog);
        } finally {
            obs.returnProgramReadLock();
        }
    }

    public static boolean isEmpty(ISPObservation obs) {
        final ObsLog obsLog = getIfExists(obs);
        return (obsLog == null) || obsLog.isEmpty();
    }

    /**
     * Defines an update operation to apply to an observation's observing log.
     */
    public interface UpdateOp {
        void apply(ISPObservation obs, ObsLog log);
    }

    /**
     * Safely updates the observation's Observing Log, holding a write lock so
     * it is not modified.
     *
     * @param db database containing the observation whose log is to be updated
     * @param obsId reference to the observation to update
     * @param op updates to apply to the log
     */
    public static void update(IDBDatabaseService db, SPObservationID obsId, UpdateOp op) {
        final ISPProgram prog = db.lookupProgramByID(obsId.getProgramID());
        if (prog == null) {
            LOG.warning("Could not find program " + obsId.getProgramID() + " so could not update obs log for " + obsId);
            return;
        }

        final SPNodeKey progKey = prog.getProgramKey();

        SPNodeKeyLocks.instance.writeLock(progKey);
        try {
            final ISPFactory factory = db.getFactory();
            final ISPObservation obs = db.lookupObservationByID(obsId);
            if (obs == null) {
                throw new RuntimeException("Could not find observation " + obsId);
            }

            final ISPObsQaLog qaLog;
            final ISPObsExecLog execLog;

            final ISPObsQaLog qaTmp = obs.getObsQaLog();
            if (qaTmp == null) {
                qaLog = factory.createObsQaLog(obs.getProgram(), null);
                obs.setObsQaLog(qaLog);
            } else {
                qaLog = qaTmp;
            }

            final ISPObsExecLog execTmp = obs.getObsExecLog();
            if (execTmp == null) {
                execLog = factory.createObsExecLog(obs.getProgram(), null);
                obs.setObsExecLog(execLog);
            } else {
                execLog = execTmp;
            }

            final ObsLog log = new ObsLog(qaLog, execLog);

            op.apply(obs, log);

            qaLog.setDataObject(log.qaLogDataObject);
            execLog.setDataObject(log.execLogDataObject);

        } catch (SPException ex) {
            LOG.log(Level.SEVERE, "Could not create ObsLogNodes", ex);
            throw new RuntimeException("Could not create ObsLogNodes", ex);
        } finally {
            SPNodeKeyLocks.instance.writeUnlock(progKey);
        }
    }

    public final ObsQaLog qaLogDataObject;
    public final ObsExecLog execLogDataObject;

    public ObsLog(ISPObsQaLog qaLog, ISPObsExecLog execLog) {
        this(qaLog, (ObsQaLog) qaLog.getDataObject(), execLog, (ObsExecLog) execLog.getDataObject());
    }

    public ObsLog(ISPObsQaLog qaLog, ObsQaLog qaLogDataObject,
                  ISPObsExecLog execLog, ObsExecLog execLogDataObject) {
        if (qaLog == null) throw new NullPointerException();
        if (execLog == null) throw new NullPointerException();
        if (qaLogDataObject == null) throw new NullPointerException();
        if (execLogDataObject == null) throw new NullPointerException();
        this.qaLogDataObject = qaLogDataObject;
        this.execLogDataObject = execLogDataObject;
    }

    public ObsQaRecord getQaRecord() {
        return qaLogDataObject.getRecord();
    }

    public ObsExecRecord getExecRecord() {
        return execLogDataObject.getRecord();
    }

    public ObsVisit[] getVisits(Option<Instrument> instrument) {
        return getExecRecord().getVisits(instrument, getQaRecord());
    }

    public ObsVisit[] getVisits(Option<Instrument> instrument, long startTime, long endTime) {
        return getExecRecord().getVisits(instrument, getQaRecord(), startTime, endTime);
    }

    public scala.Option<DataflowStatus> getMinimumDisposition() {
        return getExecRecord().getMinimumDisposition(getQaRecord());
    }

    public SortedSet<DatasetLabel> getDatasetLabels() {
        return getExecRecord().getDatasetLabels();
    }

    public DatasetRecord getDatasetRecord(DatasetLabel label) {
        final DatasetQaRecord   qa = getQaRecord().apply(label);
        final DatasetExecRecord ex = getExecRecord().getDatasetExecRecord(label);
        return new DatasetRecord(qa, ex);
    }

    public List<DatasetRecord> getAllDatasetRecords() {
        return getQaRecord().datasetRecordsJava(getExecRecord().getAllDatasetExecRecords());
    }

    public boolean isEmpty() {
        return qaLogDataObject.getRecord().qaMap().isEmpty() &&
                execLogDataObject.isEmpty();
    }
}
