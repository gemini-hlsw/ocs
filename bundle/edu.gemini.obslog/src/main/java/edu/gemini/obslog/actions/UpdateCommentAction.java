package edu.gemini.obslog.actions;

import edu.gemini.obslog.obslog.executor.OlUpdateCommentExecutor;
import edu.gemini.obslog.obslog.executor.OlUpdateWeatherCommentExecutor;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;

import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: UpdateCommentAction.java,v 1.1 2006/12/05 14:56:16 gillies Exp $
//

public class UpdateCommentAction extends OlBaseAction {
    private static final Logger LOG = Logger.getLogger(UpdateCommentAction.class.getName());

    private String _configID;
    private String _observationID;
    private String _comment;

    public void setObservationID(String observationID) {
        _observationID = observationID;
    }

    public void setConfigID(String configID) {
        _configID = configID;
    }

    public void setComment(String comment) {
        _comment = comment;
    }

    public String execute() {

        if (_configID == null) {
            addActionError("Config ID is null");
            return ERROR;
        }

        if (_comment == null) {
            String message = "Comment is null";
            addActionError(message);
            return ERROR;
        }

        if (_observationID == null) {
            addActionError("Observation ID is null");
            return ERROR;
        }
        LOG.info("ObservationID: " + _observationID + " ConfigID: " + _configID + "\nComment: " + _comment);

        SPObservationID spObservationID = null;
        try {
            spObservationID = new SPObservationID(_observationID);
        } catch (SPBadIDException ex) {
            addActionError("Failed to create a observation ID: " + ex);
        }
        // So far, we've failed to create an observation ID, now try a plan id in case it's a weather comment update
        SPProgramID spProgramID = null;
        try {
            spProgramID = SPProgramID.toProgramID(_observationID);
        } catch (SPBadIDException ex) {
            addActionError("Failed to ceate a program ID: " + ex);
        }
        if (spObservationID == null && spProgramID == null) return ERROR;

        if (spObservationID != null) {
            try {
                OlUpdateCommentExecutor exec = new OlUpdateCommentExecutor(getPersistenceManager(), spObservationID, _configID, _comment);
                exec.execute();
            } catch (Exception ex) {
                addActionError("Failed while updating config comment: " + ex);
                return ERROR;
            }
        }
        // Assume weather comment update

        try {
            OlUpdateWeatherCommentExecutor exec = new OlUpdateWeatherCommentExecutor(getPersistenceManager(), spProgramID, _configID, _comment);
            exec.execute();
        } catch (Exception ex) {
            addActionError("Failed while updating weather comment: " + ex);
            return ERROR;
        }

        return SUCCESS;
    }

}
