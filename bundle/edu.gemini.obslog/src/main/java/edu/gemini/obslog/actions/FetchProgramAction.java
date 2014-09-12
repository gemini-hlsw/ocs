package edu.gemini.obslog.actions;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.obslog.IObservingLog;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.executor.OlProgramLogRequestExecutor;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;

import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: FetchProgramAction.java,v 1.5 2005/12/11 15:54:15 gillies Exp $
//

public class FetchProgramAction extends OlBaseAction {
    private static final Logger LOG = Logger.getLogger(FetchProgramAction.class.getName());

    private String _observationID;
    // Set during processing
    private String _programID = "unset";
    private IObservingLog _observingLog;
    private boolean _showAll = false;

    public void setObservationID(String observationID) {
        _observationID = observationID;
    }

    public String getObservationID() {
        return _observationID;
    }

    public IObservingLog getObservingLog() {
        return _observingLog;
    }

    public String getProgramID() {
        return _programID;
    }

    public void setShowAll(boolean state) {
        _showAll = state;
    }

    public String execute() {
        if (_observationID == null) {
            addActionError("No observation ID was set so no program is available.");
            return ERROR;
        }

        SPProgramID programID = null;
        _observingLog = null;
        try {
            SPObservationID spObsID = new SPObservationID(_observationID);
            programID = spObsID.getProgramID();
            _programID = programID.stringValue();
        } catch (SPBadIDException ex) {
            LOG.severe(ex.getMessage() + ex);
            addActionError("Improper observation id: " + _observationID);
            return ERROR;
        }

        try {
            OlConfiguration obsLogConfig = getObsLogConfiguration();

            OlLogOptions options = new OlLogOptions();
            options.setMultiNight(true);
            options.setShowEmpties(_showAll);
            OlProgramLogRequestExecutor exec = new OlProgramLogRequestExecutor(getPersistenceManager(),
                                                                               obsLogConfig,
                                                                               options,
                                                                               programID, _user);
            exec.execute();
            _observingLog = exec.getObservingLog();
        } catch (OlLogException ex) {
            addActionError("Observing Log generation caused an exception: " + ex);
            return ERROR;
        }

        if (_observingLog == null) {
            addActionError("No ObservingLog was generated for program: " + programID.stringValue());
            return ERROR;
        }

        return SUCCESS;
    }

}
