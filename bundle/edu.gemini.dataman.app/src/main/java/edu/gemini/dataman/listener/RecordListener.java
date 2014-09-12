//
// $Id: RecordListener.java 218 2005-10-18 20:59:53Z shane $
//

package edu.gemini.dataman.listener;

import edu.gemini.dataman.context.DatamanContext;
import edu.gemini.dataman.update.FileUpdateCommand;
import edu.gemini.datasetrecord.DatasetRecordEvent;
import edu.gemini.datasetrecord.DatasetRecordListener;
import edu.gemini.spModel.dataset.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public final class RecordListener implements DatasetRecordListener {
    private static final Logger LOG = Logger.getLogger(RecordListener.class.getName());

    private DatamanContext _ctx;

    public RecordListener(DatamanContext ctx) {
        _ctx = ctx;
    }

    public void datasetModified(DatasetRecordEvent evt) {
        final DatasetRecord oldRec = evt.getOldVersion();
        final DatasetRecord newRec = evt.getNewVersion();
        if (newRec == null) {
            if (oldRec == null) {
                LOG.log(Level.SEVERE, "Received an update with no old or new version");
            } else {
                LOG.warning("Deleted DatasetRecord '" + oldRec.getLabel() + '\'');
            }
            return;
        }

        final DatasetLabel label = newRec.getLabel();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("* dataset record update received: " + label);
        }

        // Determine whether the matching file needs to be updated.  If so
        // schedule an update.  This will cause the file update to be noticed
        // by the FileListener, which will update the record, which will cause
        // another call to datasetModified() ...
        final DatasetQaState qaState = newRec.qa.qaState;
        final String filename = newRec.exec.dataset.getDhsFilename();
//        if ((oldRec == null) || !oldRec.qa.qaState.equals(newRec.qa.qaState)) {
            if (!DatasetFileState.OK.equals(newRec.exec.fileState)) {
                LOG.fine("Ignoring update for tentative/missing/bad dataset: " + label);
                return;
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Updating QA qaState of dataset '" + filename + "' to: " +
                         qaState);
            }
            FileUpdateCommand.scheduleUpdate(_ctx, label, filename, qaState);
//        }
    }
}
