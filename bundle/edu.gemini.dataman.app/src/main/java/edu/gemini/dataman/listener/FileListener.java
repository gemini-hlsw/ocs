//
// $Id: FileListener.java 281 2006-02-13 17:52:21Z shane $
//

package edu.gemini.dataman.listener;

import edu.gemini.dataman.context.DatamanContext;
import edu.gemini.dataman.update.RecordUpdateCommand;
import edu.gemini.dataman.util.DatamanLoggers;
import edu.gemini.dataman.xfer.XferService;
import edu.gemini.datasetfile.*;
import edu.gemini.datasetrecord.DatasetRecordService;
import edu.gemini.spModel.core.ProgramType$;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.dataflow.GsaAspect;
import edu.gemini.spModel.dataset.DatasetFileState;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.GsaState;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public final class FileListener implements DatasetFileListener {
    private static final Logger LOG = Logger.getLogger(FileListener.class.getName());

    private DatamanContext _ctx;

    public FileListener(DatamanContext ctx) {
        _ctx = ctx;
    }

    private boolean _updateRelease(DatasetFile file) {

        // Make sure the required services are available.  If either the
        // record or file services aren't available, then we can't continue
        DatasetRecordService recordService = _ctx.getDatasetRecordService();
        if (recordService == null) return false;
        DatasetFileService fileService = _ctx.getDatasetFileService();
        if (fileService == null) return false;

        // Get the GsaAspect to use, checking first to see if the program
        // overrides the default value;
        DatasetLabel label = file.getDataset().getLabel();
        SPProgramID progId = label.getObservationId().getProgramID();
        GsaAspect gsa;
        try {
            gsa = recordService.fetchGsaAspect(progId);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
            return false;
        }
        if (gsa == null) {
            gsa = GsaAspect.getDefaultAspect(ProgramType$.MODULE$.readOrNull(progId));
        }

        LOG.fine("need to calculate release date");
        Date releaseDate = ReleaseDateCalculator.calculate(file, gsa);
        LOG.info("Set release to: " + releaseDate);
        String fileName = file.getFile().getName();

        try {
            fileService.updateRelease(fileName, releaseDate, gsa.isHeaderPrivate());
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            return false;
        } catch (DatasetFileException ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
            DatamanLoggers.DATASET_PROBLEM_LOGGER.log(Level.SEVERE, ex.getMessage());
            return false;
        } catch (InterruptedException ex) {
            LOG.log(Level.WARNING, "Interrupted while locking " + file, ex);
            return false;
        }

        return true;
    }

    public void datasetAdded(DatasetFileEvent evt) {
        _handleDatasetUpdate(evt, true);
    }

    public void datasetModified(DatasetFileEvent evt) {
        _handleDatasetUpdate(evt, false);
    }

    private void _handleDatasetUpdate(DatasetFileEvent evt, boolean isNew) {
        DatasetFile dsetFile = evt.getDatasetFile();
        if (isNew) {
            LOG.info("New dataset in working storage: " + dsetFile);
        } else {
            LOG.info("Dataset dsetFile modified: " + dsetFile);
        }

        // Figure out the program type and observation class.
        DatasetLabel label = dsetFile.getDataset().getLabel();

        // --------------------------------------------------------------------
        // The logic for the initial GSA transfer is a bit confusing.  The idea
        // is that the initial transfer is done right away as soon as the
        // dataset rolls out of the instrument.  We only want to do the initial
        // transfer once though.  After that, the dataset can be modified many
        // times and it shouldn't trigger a new "initial" transfer.  For that
        // reason, we always guard the initial transfer with the "isNew" check.
        //
        // Furthermore, every modification to a dataset that has a final
        // QA state should result in a new transfer to the GSA.  So if a new
        // file shows up and it already has a final QA state, we don't do the
        // special initial transfer, knowing that the normal transfer will
        // soon happen.
        // --------------------------------------------------------------------

        // If there is no value for RELEASE, we will add the RELEASE keyword.
        // That will trigger another file modification event, and another pass
        // through this code.
        boolean isFinalQa = dsetFile.getQaState().isFinal();
        if (dsetFile.getRelease() == null) {
            _updateRelease(dsetFile);

            if (isNew && !isFinalQa) {
                XferService.initialXferToGsa(_ctx, label, dsetFile.getFile());
            }
            return;
        }

        // Update the dataset record. If now in a final QA state, then
        // transition to GsaState PENDING so that the GsaVigilante will later
        // pick it up and move it to the GSA.
        GsaState newGsaState =
                (isFinalQa && XferService.shouldXferToGsa(label)) ? GsaState.PENDING : GsaState.NONE;

        RecordUpdateCommand cmd = new RecordUpdateCommand(_ctx, dsetFile);
        cmd.setGsaState(newGsaState);
        cmd.setDatasetFileState(DatasetFileState.OK);
        cmd.scheduleUpdate();

        // Otherwise, if the file showed up new and yet already had the
        // RELEASE keyword, we'll get to this point and need to do the initial
        // transfer.
        if (isNew && !isFinalQa) {
            XferService.initialXferToGsa(_ctx, label, dsetFile.getFile());
        }

        // Send any updated file to the base facility.
        // NOTE: The check for "isFinalQa" adds the requirement that only QA'ed
        // datasets are sent to the base facility.  This is likely wrong but
        // matches the dataman behavior from the initial installation.  A
        // change to this behavior requires a task request from science.
        if (isFinalQa) {
            XferService.xferToBase(_ctx, label, dsetFile.getFile());
        }
    }

    public void badDatasetFound(DatasetFileEvent evt) {
        String msg = evt.getMessage();
        LOG.log(Level.WARNING, msg);
        DatamanLoggers.DATASET_PROBLEM_LOGGER.log(Level.WARNING, msg);

        // Update the DatasetRecord.
        DatasetLabel label = evt.getLabel();
        if (label == null) return;
        RecordUpdateCommand cmd = new RecordUpdateCommand(_ctx, label);
        cmd.setDatasetFileState(DatasetFileState.BAD);
        cmd.scheduleUpdate();
    }

    public void datasetDeleted(DatasetFileEvent evt) {
        String msg = evt.getMessage();
        LOG.log(Level.WARNING, msg);

        // Update the DatasetRecord.
        DatasetLabel label = evt.getLabel();
        if (label == null) return;
        RecordUpdateCommand cmd = new RecordUpdateCommand(_ctx, label);
        cmd.setDatasetFileState(DatasetFileState.MISSING);
        cmd.scheduleUpdate();
    }
}
