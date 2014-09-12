package edu.gemini.obslog.obslog.executor;

import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.obslog.ObsLog;
import java.util.logging.Logger;
import java.util.logging.Level;



//
// Gemini Observatory/AURA
// $Id: OlUpdateCommentExecutor.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public class OlUpdateCommentExecutor {
    public static final Logger LOG = Logger.getLogger(OlUpdateCommentExecutor.class.getName());

    private OlPersistenceManager _persistenceManager;
    private SPObservationID _observationID;
    private String _dataset;
    private String _comment;

    public OlUpdateCommentExecutor(OlPersistenceManager persistenceManager, SPObservationID observationID, String dataset, String comment) {
        this(persistenceManager, observationID, comment);
        _dataset = dataset;
    }

    public OlUpdateCommentExecutor(OlPersistenceManager persistenceManager, SPObservationID observationID, String comment) {
        if (observationID == null || comment == null) {
            throw new NullPointerException("Null value for comment update constructor");
        }
        _persistenceManager = persistenceManager;
        _observationID = observationID;
        _comment = comment;
        _dataset = null;
    }

    /**
     * Sets the appropriate comment.   This may be the observation comment or a config comment.
     *
     * @throws edu.gemini.obslog.obslog.OlLogException if an error occurs while constructing the observing log
     */
    public void execute() throws OlLogException {
        if (_dataset == null) {
            throw new OlLogException("Comment received is null");
        }

        // Need the observation data object too.  Can't get this far without a valid observation
        final DatasetLabel dlabel;
        try {
            dlabel = new DatasetLabel(_dataset);
        } catch (java.text.ParseException ex) {
            throw new OlLogException("Bad dataset id: " + _dataset);
        }

        // Check for an observing log component
        ObsLog.update(_persistenceManager.getDatabase(), _observationID, new ObsLog.UpdateOp() {
            @Override public void apply(ISPObservation obs, ObsLog log) {
                final DatasetRecord drecord = log.getDatasetRecord(dlabel);
                if (drecord == null) {
                    LOG.log(Level.WARNING, "Failed to update comment for: " + _dataset);
                } else {
                    // Now store the updated comment back to the database
                    log.qaLogDataObject.setComment(dlabel, _comment);
                }
            }
        });
    }

}
