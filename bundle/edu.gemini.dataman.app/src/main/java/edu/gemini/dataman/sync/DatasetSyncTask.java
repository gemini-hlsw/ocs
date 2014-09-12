//
// $Id: DatasetSyncTask.java 710 2006-12-21 14:09:00Z shane $
//

package edu.gemini.dataman.sync;

import edu.gemini.dataman.context.DatamanContext;
import edu.gemini.dataman.update.FileUpdateCommand;
import edu.gemini.dataman.update.RecordUpdateCommand;
import edu.gemini.dataman.util.DatamanLoggers;
import edu.gemini.dataman.util.DatasetCommandProcessor;
import edu.gemini.datasetfile.DatasetFile;
import edu.gemini.datasetfile.DatasetFileException;
import edu.gemini.datasetfile.DatasetFileService;
import edu.gemini.datasetrecord.DatasetRecordService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.dataset.DatasetExecRecord;
import edu.gemini.spModel.dataset.DatasetFileState;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.GsaState;

import java.io.File;
import java.io.IOException;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
final class DatasetSyncTask implements Callable<Boolean> {
    private static final Logger LOG = Logger.getLogger(DatasetSyncTask.class.getName());

    private DatamanContext _ctx;
    private boolean _cancelled;

    // We don't want to send out 12000 emails if there is some problem that
    // causes an IOException for every one.  So just record one of them
    // and the count of how many problems there were and log it at the end.
    private IOException _lastIoEx;
    private String _lastIoExFile;
    private int _ioExCount;
    private final Set<Principal> _user;

    // Log of messages about datasets that couldn't be parsed.
    private StringBuilder _problemDatasets = new StringBuilder();

    DatasetSyncTask(DatamanContext ctx, Set<Principal> user) {
        _ctx = ctx;
        _user = user;
    }

    synchronized void cancel() {
        _cancelled = true;
    }

    private synchronized boolean _isCancelled() {
        return _cancelled;
    }

    /**
     * @return <code>true</code> if the sync ran to completion;
     * <code>false</code> if unable to complete because necessary services
     * disappeared or the task was cancelled
     */
    public Boolean call() {
        long startTime = System.currentTimeMillis();

        LOG.info("Starting DatasetSync operation.");
        boolean result = false;
        try {
            result = _internalCall();
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, t.getMessage(), t);
        }

        if (LOG.isLoggable(Level.INFO)) {
            StringBuilder buf = new StringBuilder();
            if (_isCancelled()) {
                buf.append("Sync cancelled after: ");
            } else if (result) {
                buf.append("Sync completed after: ");
            } else {
                buf.append("Sync stopped after: ");
            }
            buf.append(System.currentTimeMillis() - startTime).append(" ms");
            LOG.info(buf.toString());
        }
        return result;
    }

    private boolean _internalCall() {
        // First get a list of program ids in the database.  And establish
        // which if any of the databases we can use.
        List<SPProgramID> progIds = null;
        IDBDatabaseService workingDb = null;

        Set<IDBDatabaseService> dbs = _ctx.getDatabases();
        if (dbs == null) return false;

        for (IDBDatabaseService db : dbs) {
            if (_isCancelled()) return false;
            progIds = ProgramIdFetchFunctor.getProgramIds(db, _user);
            workingDb = db;
        }
        if (progIds == null) {
            LOG.warning("DatasetSync found an empty database");
            return !_isCancelled();
        }

        // Scan all the dataset records, and match them against corresponding
        // files in the working directory.
        Set<String> filenames = _syncProgramDatasets(progIds, workingDb);
        if (filenames == null) return false;

        // Find new dataset files in the working directory and add them to the
        // ODB.
        if (!_addNewDatasets(filenames)) return false;

        // Report the bad dataset exceptions if there were any.
        _reportProblemDatasets();

        // Report the IOExceptions, if there were any.
        _reportIoExceptions();

        return !_isCancelled();
    }

    private boolean _addNewDatasets(Set<String> fileNames) {
        // Get all the fileNames
        File workDir    = _ctx.getConfig().getWorkDir();
        File[] datasets = workDir.listFiles(_ctx.getConfig().getFileFilter());

        if ((datasets == null) || (datasets.length == 0)) {
            return true; // no datasets all all
        }

        // Add datasets that we haven't discovered before.
        for (File dataset : datasets) {
            String datasetName = dataset.getName();
            if (!fileNames.contains(datasetName)) {
                if (_isCancelled()) return false;
                DatasetFileService  fileSrv = _ctx.getDatasetFileService();
                if (fileSrv == null) return false;

                DatasetFile dsetFile;
                try {
                    dsetFile = fileSrv.fetch(datasetName);
                    if (dsetFile == null) {
                        LOG.warning("Dataset '" + datasetName + "' was deleted during sync");
                        return true;
                    }

                    if (!RecordUpdateCommand.scheduleUpdate(_ctx, dsetFile)) {
                        return false;
                    }

                } catch (InterruptedException ex) {
                    LOG.warning("Sync interrupted while reading dataset '" + datasetName + "'.");
                    return false;

                } catch (IOException ex) {
                    _lastIoEx = ex;
                    _lastIoExFile = datasetName;
                    ++_ioExCount;

                } catch (DatasetFileException e) {
                    _problemDatasets.append(datasetName).append(" :");
                    _problemDatasets.append(e.getMessage()).append('\n');
                }
            }
        }

        return true;
    }

    /**
     * Syncs the {@link edu.gemini.spModel.dataset.DatasetExecRecord}s in the ODB with the dataset files in
     * the working directory.
     *
     * @return Set of the dataset file names that were encountered in the
     * database; <code>null</code> if the sync could not be completed
     */
    private Set<String> _syncProgramDatasets(List<SPProgramID> progIds,
                                         IDBDatabaseService db) {

        Set<String> fileNames = new TreeSet<String>();

        // Sync a few program ids at a time.
        for (int i=0; i<progIds.size(); i+=50) {
            if (_isCancelled()) return null;

            final int end = Math.min(i+50, progIds.size());
            final List<SPProgramID> curIds = progIds.subList(i, end);

            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Syncing programs: " + curIds);
            }

            final Collection<DatasetRecordSyncState> sstates;
            sstates = SyncStateFetchFunctor.getSyncStates(db, curIds, _user);

            for (DatasetRecordSyncState drss : sstates) {
                DatasetFileService fileSrv = _ctx.getDatasetFileService();
                if (fileSrv == null) return null; // file service went away
                if (_isCancelled()) return null;

                String filename = drss.getFilename();
                fileNames.add(filename);

                try {
                    DatasetFile dsetFile = fileSrv.fetch(filename);

                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Sync\n\tODB Record: " + drss +
                                     "\n\tFile......: " + dsetFile);
                    }

                    // Check for missing files in the working storage.
                    if (dsetFile == null) {
                        if (!DatasetFileState.MISSING.equals(drss.getFileState()) &&
                            !DatasetFileState.TENTATIVE.equals(drss.getFileState())) {
                            RecordUpdateCommand cmd;
                            cmd = new RecordUpdateCommand(_ctx, drss.getLabel());
                            cmd.setDatasetFileState(DatasetFileState.MISSING);
                            if (!cmd.scheduleUpdate()) return null;
                        }
                    } else {
                        // sync database record with dataset file
                        if (!_syncDataset(drss, dsetFile)) return null;
                    }
                } catch (InterruptedException ex) {
                    LOG.warning("Interrupted while syncing dataset '" +
                                drss.getLabel() + "'.");
                    return null;

                } catch (IOException ex) {
                    _lastIoEx     = ex;
                    _lastIoExFile = filename;
                    ++_ioExCount;
                } catch (DatasetFileException e) {
                    _problemDatasets.append(drss.getFilename()).append(" :");
                    _problemDatasets.append(e.getMessage()).append('\n');

                    // Update the database record
                    RecordUpdateCommand cmd;
                    cmd = new RecordUpdateCommand(_ctx, drss.getLabel());
                    cmd.setDatasetFileState(DatasetFileState.BAD);
                    if (!cmd.scheduleUpdate()) return null;
                }
            }
        }

        return fileNames;
    }

    private boolean _syncDataset(DatasetRecordSyncState drss, DatasetFile dsetFile) {
        DatasetLabel label = drss.getLabel();


        long fileTime   = dsetFile.lastModified();
        long recordTime = drss.getSyncTime();

        // If the QA states differ
        if (!drss.getQaState().equals(dsetFile.getQaState())) {
            // 3 possibilities, depending upon the sync time

            // 1) file sync time > record sync time -> update dataset record
            // This means the file has been updated more recently than the
            // dataset record.
            if (fileTime > recordTime) {
                return RecordUpdateCommand.scheduleUpdate(_ctx, dsetFile);
            }

            // 2) file sync time == record sync time -> update file
            // This means that the record in the ODB has been updated more
            // recently than the file.
            if (fileTime == recordTime) {
                String name = dsetFile.getFile().getName();
                return FileUpdateCommand.scheduleUpdate(_ctx, label, name,
                                                        drss.getQaState());
            }

            // file sync time < record sync time -> error
            _warnSyncTime(label, recordTime, fileTime);

        } else if (recordTime != fileTime) {
            // file sync time > record sync time -> update dataset record
            if (fileTime > recordTime) {
                return RecordUpdateCommand.scheduleUpdate(_ctx, dsetFile);
            }

            // file sync time < record sync time -> error
            _warnSyncTime(label, recordTime, fileTime);

        } else {

            // Everything matches up, but it is possible that the dataset was
            // in the process of being copied when the database went down.
            // Catch "temporary" dataflow states and reset them if ncessary.
            // Resetting it will cause the file to be copied, verified, etc.
            // again.  Specifically don't include TRANSFERRING in this category,
            // because the GsaVigilante handles updating datasets that it
            // believes are being transferred.
            GsaState state = drss.getGsaState();
            if ((state == GsaState.COPYING) || (state == GsaState.VERIFYING)) {
                return _resetStuckDataset(drss, dsetFile);
            }
        }

        // nothing need be done
        return true;
    }

    private boolean _resetStuckDataset(DatasetRecordSyncState drss, DatasetFile dsetFile) {
        // Only do this if not in the middle of processing.  If in the
        // middle of processing then we would expect temporary dataset
        // states.
        if (DatasetCommandProcessor.INSTANCE.isProcessing(drss.getLabel())) {
            return true;
        }

        // It is possible that in the time between when we make the
        // decision to reset and we check whether it is processing, that
        // the processing could terminate.  So check the state and sync time
        // one more time.  Then we'd have 3 times in order:
        //
        //     t1 - dataset record has a temporary state
        //     t2 - known not to be processing dataset record
        //     t3 - dataset has same temporary state at same time
        //
        DatasetRecordService drs = _ctx.getDatasetRecordService();
        if (drs == null) return false;

        try {
            DatasetExecRecord rec = drs.fetch(drss.getLabel());
            if (rec == null) {
                LOG.warning("Dataset record '" + drss.getLabel() +
                            "' disappeared during a sync");
                return true;  // wasn't expecting that ...
            }
            GsaState state_t1 = drss.getGsaState();
            GsaState state_t3 = rec.getGsaState();
            if (!state_t1.equals(state_t3)) return true; // moved on

            long time_t1 = drss.getSyncTime();
            long time_t3 = rec.getSyncTime();
            if (time_t1 != time_t3) return true;  // moved on

        } catch (InterruptedException ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
            return true;
        }

        RecordUpdateCommand cmd;
        cmd = new RecordUpdateCommand(_ctx, dsetFile);
        cmd.setGsaState(GsaState.PENDING);
        return cmd.scheduleUpdate();
    }

    private void _reportProblemDatasets() {
        if (_problemDatasets.length() == 0) return;
        String msg = "Problem(s) reading datasets: \n" + _problemDatasets.toString();
        DatamanLoggers.DATASET_PROBLEM_LOGGER.warning(msg);
    }

    private void _reportIoExceptions() {
        if (_ioExCount == 0) return;
        final StringBuilder buf = new StringBuilder();
        buf.append("Problem syncing dataset '").append(_lastIoExFile);
        if (_ioExCount == 1) {
            buf.append('\'');
        } else {
            buf.append("' (also ").append(_ioExCount-1);
            buf.append(" other IOException(s))");
        }
        LOG.log(Level.SEVERE, buf.toString(), _lastIoEx);
    }

    private static void _warnSyncTime(DatasetLabel label, long recTime, long fileTime) {
        final StringBuilder buf = new StringBuilder();
        buf.append("Database record for '");
        buf.append(label);
        buf.append("' has sync time ").append(recTime);
        buf.append(", which is later than the file mod time of ");
        buf.append(fileTime);
        LOG.warning(buf.toString());
    }

}
