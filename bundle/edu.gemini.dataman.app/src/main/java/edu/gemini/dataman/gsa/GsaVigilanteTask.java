//
// $Id: GsaVigilanteTask.java 697 2006-12-15 17:55:42Z shane $
//

package edu.gemini.dataman.gsa;

import edu.gemini.dataman.context.DatamanContext;
import edu.gemini.dataman.update.RecordUpdateCommand;
import edu.gemini.dataman.util.DatamanFileUtil;
import edu.gemini.dataman.util.DatasetCommandProcessor;
import edu.gemini.dataman.xfer.XferService;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.GsaState;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A TimerTask that scans the database looking for datasets that have a
 * significant {@link GsaState} in order to move them along in the transfer
 * process if possible.  The states sought and the actions taken are listed
 * below:
 *
 * <ul>
 * <li>{@link GsaState#PENDING}: Verify that the file isn't in the e-transfer
 * system and then copy it to the queueing directory.</li>
 *
 * <li>{@link GsaState#COPY_FAILED}: Reset to PENDING and do the same thing as
 * though the file had been in state PENDING.  This effectively retries the
 * copy.</li>
 *
 * <li>{@link GsaState#QUEUED}: Ask the GSA for the current status of the file
 * and update the GsaState as appropriate.</li>
 *
 * <li>{@link GsaState#TRANSFERRING}: Ask the GSA for the current status of the
 * file and update the GsaState as appropriate.</li>
 *
 * <li>{@link GsaState#TRANSFER_ERROR}: Same as for QUEUED.  The idea is that
 * transfer errors are temporary.  We expect them to go away eventually and
 * keep trying until they do or the user resets the state to PENDING to try
 * a new copy.</li>
 * </ul>
 *
 * The {@link GsaVigilante} is in charge of managing the Timer in which this
 * task runs.
 */
final class GsaVigilanteTask extends TimerTask {
    private static final Logger LOG = Logger.getLogger(GsaVigilanteTask.class.getName());
    private static final Level LEVEL = Level.INFO;

    // The GsaStates that we are interrested in advancing.
    private static final GsaState[] STATES = new GsaState[] {
            GsaState.PENDING,
            GsaState.COPY_FAILED,
            GsaState.QUEUED,
            GsaState.TRANSFERRING,
            GsaState.TRANSFER_ERROR,
    };

    private final DatamanContext _ctx;
    private final Set<Principal> _user;

    GsaVigilanteTask(DatamanContext ctx, Set<Principal> user) {
        _ctx = ctx;
        _user = user;
    }

    // Update the the GSA state from oldState to newState.  When we get to the
    // database, if the record isn't still in oldState, then it will not be
    // updated.
    private void updateGsaState(GsaDatasetInfo info, GsaState oldState, GsaState newState, Level logLevel) {
        if (oldState == newState) return;

        // Log the transition.
        if (LOG.isLoggable(logLevel)) {
            String msg = String.format("Dataset %s (%s): schedule transition from %s to %s",
                    info.getLabel(), info.getFilename(), oldState, newState);
            LOG.log(logLevel, msg);
        }

        // Transition the GSA state.
        RecordUpdateCommand cmd;
        cmd = new RecordUpdateCommand(_ctx, info.getLabel());
        cmd.setGsaStatePrecond(oldState);
        cmd.setGsaState(newState);
        cmd.scheduleUpdate();
    }


    // Gets the map of relevant GsaStates to a list of datasets in that state
    // from the database.
    private Map<GsaState, List<GsaDatasetInfo>> _getGsaStateMap() {
        Set<IDBDatabaseService> s = _ctx.getDatabases();
        Map<GsaState, List<GsaDatasetInfo>> map;
        map = GsaStateFunctor.getStateMap(s, STATES, _user);

        // Log the current states.
        if (LOG.isLoggable(LEVEL)) {
            StringBuilder buf = new StringBuilder();
            for (GsaState state : STATES) {
                String str;
                str = String.format("\t%15s: %s", state.name(), map.get(state));
                buf.append(str).append("\n");
            }
            LOG.log(LEVEL, buf.toString());
        }

        return map;
    }

    // Resets the GsaState of all datasets that were in a COPY_FAILED state,
    // in order to retry the copy.
    private void _resetCopyFailed(List<GsaDatasetInfo> datasets) {
        for (GsaDatasetInfo dataset : datasets) {
            updateGsaState(dataset, GsaState.COPY_FAILED, GsaState.PENDING, Level.INFO);
        }
    }


    // Copies all the pending files to the GSA staging machine.  Or rather,
    // schedules the work to be done with the {@link DatasetCommandProcessor}.
    private void copyPending(GsaState curState, List<GsaDatasetInfo> pendingFiles,
                             Map<String, GsaFileStatus> curStatusMap) {

        // Save some logging messages if there are no files to operate on.
        if (pendingFiles.size() == 0) return;

        LOG.log(LEVEL, "GsaVigilante: start copy pending files....");

        for (GsaDatasetInfo pendingFile : pendingFiles) {
            String filename = pendingFile.getFilename();

            // If we can't find the current status, then there was some error
            // obtaining it.  Log this and skip it. On the next pass through,
            // we'll check on the dataset again.
            GsaFileStatus curStatus = curStatusMap.get(filename);
            if ((curStatus == null) || (curStatus.getState() == GsaFileStatus.State.unknown)) {
                String msg = String.format(
                        "Dataset %s (%s) is pending but cannot get current status from GSA.",
                        pendingFile.getLabel(), filename);
                LOG.log(Level.WARNING, msg);
                continue;
            }

            // If the dataset isn't currently being processed by the GSA, we
            // can copy it now.  Otherwise, it'll have to wait for a future
            // pass and will remain PENDING.
            if (!curStatus.getState().isTerminal()) {
                // Do nothing since the GSA can't handle multiple versions of
                // the same file in the transfer process at the same time.
                LOG.log(LEVEL, "GsaVigilante: " + filename + " currently in GSA e-transfer, skipping for now");
                continue;
            }

            // Alright, ready to scheudle a file copy.
            DatasetLabel label = pendingFile.getLabel();
            File f = new File(_ctx.getConfig().getWorkDir(), pendingFile.getFilename());

            if (!f.exists()) {
                // If missing, then update GSA state to NONE so we don't try
                // to copy again.
                LOG.log(LEVEL, "GsaVigilante: pending file missing " + filename);
                updateGsaState(pendingFile, curState, GsaState.NONE, Level.INFO);
            } else if (!XferService.shouldXferToGsa(label)) {
                // Remove the PENDING state of datasets that shouldn't be sent
                // to the GSA.  This shouldn't happen ....
                LOG.log(LEVEL, "GsaVigilante: pending file is not GSA eligible " + filename);
                updateGsaState(pendingFile, curState, GsaState.NONE, Level.INFO);
            } else {
                LOG.log(LEVEL, "GsaVigilante: schedule copy " + filename);
                XferService.xferToGsa(_ctx, label, f);
            }
        }

        LOG.log(LEVEL, "GsaVigilante: end copy pending files");
    }

    // Gets the CRC value returned from querying the CADC.
    private long getRemoteCrc(GsaFileStatus status) {
        // If accepted, then we have to verify that the CRC matches.
        Long gsaCrc = status.getCrc();
        if (gsaCrc == null) {
            // can't be accepted without a CRC
            String msg = String.format("Accepted file missing CRC: " + status.getFilename());
            LOG.log(Level.SEVERE, msg);
            throw new RuntimeException(msg);
        }
        return gsaCrc;
    }

    // Updates the given file, which is currently set with a GsaState of
    // curGsaState, to GsaState.ACCEPTED.  This is dependent upon the CRC of
    // the remote file matching the locally calculated CRC.
    private void updateToAccepted(GsaDatasetInfo file, GsaState curGsaState,
                                  GsaFileStatus curStatus) {
        // If accepted, then we have to verify that the CRC matches.
        long remoteCrc = getRemoteCrc(curStatus);

        File f = new File(_ctx.getConfig().getWorkDir(), file.getFilename());
        if (!f.exists()) {
            // The file has been unexpectedly removed.  The Dataman
            // will notice this and update the file state to missing.
            updateGsaState(file, curGsaState, GsaState.ACCEPTED, Level.INFO);
            return;
        }

        long localCrc;
        try {
            localCrc = DatamanFileUtil.crc(f);
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            updateGsaState(file, curGsaState, GsaState.TRANSFER_ERROR, Level.WARNING);
            return;
        } catch (IOException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            updateGsaState(file, curGsaState, GsaState.TRANSFER_ERROR, Level.WARNING);
            return;
        }

        if (localCrc == remoteCrc) {
            // accepted and all is well
            updateGsaState(file, curGsaState, GsaState.ACCEPTED, Level.INFO);
        } else {
            // There was apparently a problem transferring the file.
            // We'll have to do it again.
            LOG.log(Level.INFO, "GSA has accepted file with a different CRC: " + file.getLabel() + " (" + file.getFilename() + ")");
            updateGsaState(file, curGsaState, GsaState.PENDING, Level.WARNING);
        }
    }

    // Step through each file, updating its GsaState as appropriate.
    private void update(GsaState curGsaState, List<GsaDatasetInfo> files,
                        Map<String, GsaFileStatus> curStatusMap) {

        // Save some logging messages if there are no files to operate on.
        if (files.size() == 0) return;

        LOG.log(LEVEL, "GsaVigilante: start update of datasets (" + curGsaState + ")....");


        for (GsaDatasetInfo file : files) {
            String filename = file.getFilename();

            // If we can't find the current status, then there was some error
            // obtaining it.  Mark the dataset as in an error state and move
            // on.  On the next pass through, we'll check on the dataset
            // again.
            GsaFileStatus curStatus = curStatusMap.get(filename);
            if ((curStatus == null) || (curStatus.getState() == GsaFileStatus.State.unknown)) {
                updateGsaState(file, curGsaState, GsaState.TRANSFER_ERROR, Level.WARNING);
                continue;
            }

            // Figure out what GsaState corresponds to the given GsaFileStatus.
            switch (curStatus.getState()) {
                case notFound:
                    // Somehow the file disappeared in the GSA!
                    updateGsaState(file, curGsaState, GsaState.PENDING, Level.WARNING);
                    break;
                case queued:
                    updateGsaState(file, curGsaState, GsaState.QUEUED, Level.INFO);
                    break;
                case processing:
                    updateGsaState(file, curGsaState, GsaState.TRANSFERRING, Level.INFO);
                    break;
                case rejected:
                    updateGsaState(file, curGsaState, GsaState.REJECTED, Level.INFO);
                    break;
                case accepted:
                    updateToAccepted(file, curGsaState, curStatus);
                    break;

                default:
                    String msg;
                    msg = String.format("Dataset %s (%s) has unexpected GsaFileStatus.State: %s",
                           file.getLabel(), file.getFilename(),curStatus.getState());
                    LOG.log(Level.SEVERE, msg);
                    throw new RuntimeException(msg);
            }
        }

        LOG.log(LEVEL, "GsaVigilante: end update of datasets");
    }

    // Gets the collection of all the filenames that were in the collection
    // of interesting files returned from the database.
    private Set<String> getFilenames(Collection<List<GsaDatasetInfo>> infoListCollection) {
        Set<String> filenames = new HashSet<String>();
        for (List<GsaDatasetInfo> lst : infoListCollection) {
            for (GsaDatasetInfo info : lst) {
                filenames.add(info.getFilename());
            }
        }
        return filenames;
    }

    // Removes any entries in the current GsaState map for datasets that are
    // currently being processed.  If the actions initiated by the vigilante
    // fall behind, then it can queue up a bunch of actions to do that aren't
    // finished when it next scans.  So we want to avoid trying to process any
    // dataset that has pending jobs.  In the subsequent scan, if these datasets
    // are still in the same GSA state as before, then we'll have another
    // crack at them.
    private void trimGsaStateMap(Map<GsaState, List<GsaDatasetInfo>> stateMap) {
        for (List<GsaDatasetInfo> list : stateMap.values()) {
            ListIterator<GsaDatasetInfo> lit = list.listIterator();
            while (lit.hasNext()) {
                GsaDatasetInfo dataset = lit.next();
                if (DatasetCommandProcessor.INSTANCE.isProcessing(dataset.getLabel())) {
                    lit.remove();
                }
            }
        }
    }

    private void _scan() {
        LOG.log(LEVEL, "******** Starting GsaVigilante scan *****************");

        // Get all the datasets with interesting GsaStates.  Interesting means
        // we need to check on its current status in the GSA and possibly
        // advance it to another status or reset it to try again.
        Map<GsaState, List<GsaDatasetInfo>> gsaStateMap = _getGsaStateMap();

        // Throw out results for any datasets that are currently being processed
        // by the dataset command processor.  We'll check these again on the
        // next scan and see if they are quiet then.
        trimGsaStateMap(gsaStateMap);

        // Reset datasets which failed to copy.
        _resetCopyFailed(gsaStateMap.get(GsaState.COPY_FAILED));

        // Get the current file status as seen by the GSA.
        Set<String> filenames = getFilenames(gsaStateMap.values());
        Map<String, GsaFileStatus> curStatusMap;
        curStatusMap = GsaFileStatus.query(_ctx.getConfig(), filenames);

        // Now we process everything for which we have all the information
        // we need to take the next step.  PENDING or COPY_FAILED (which have
        // been reset to PENDING above) are copied down to the queue directory.
        for (GsaState state : new GsaState[] {GsaState.PENDING, GsaState.COPY_FAILED}) {
            copyPending(state, gsaStateMap.get(state), curStatusMap);
        }

        // Old QUEUED, TRANSFERRING, and TRANSFER_ERROR are checked to see if
        // we can update their state.
        for (GsaState state : new GsaState[] {GsaState.QUEUED, GsaState.TRANSFERRING, GsaState.TRANSFER_ERROR} ) {
            update(state, gsaStateMap.get(state), curStatusMap);
        }

        LOG.log(LEVEL, "******** Finished GsaVigilante scan *****************");
    }

    public void run() {
        try {
            _scan();
        } catch (Throwable ex) {
            LOG.log(Level.SEVERE, "Problem running GsaVigilante", ex);
        }
    }
}
