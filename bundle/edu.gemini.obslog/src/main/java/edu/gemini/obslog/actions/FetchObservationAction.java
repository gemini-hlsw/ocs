package edu.gemini.obslog.actions;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.executor.OlObservationLogRequestExecutor;
import edu.gemini.obslog.obslog.IObservingLog;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPBadIDException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: FetchObservationAction.java,v 1.6 2005/12/11 15:54:15 gillies Exp $
//

public class FetchObservationAction extends OlBaseAction {
    private static final Logger LOG = Logger.getLogger(FetchObservationAction.class.getName());

    private String _observationID;
    private IObservingLog _observingLog;

    public void setObservationID(String observationID) {
        _observationID = observationID;
    }

    public String getObservationID() {
        return _observationID;
    }

    public IObservingLog getObservingLog() {
        return _observingLog;
    }

    public String execute() {
        if (_observationID == null) {
            addActionError("No observation id was set.");
            return ERROR;
        }

        SPObservationID obsID = null;
        _observingLog = null;
        try {
            obsID = new SPObservationID(_observationID);
        } catch (SPBadIDException ex) {
            LOG.severe(ex.getMessage() + ex);
            addActionError("Improper observation id: " + _observationID);
            return ERROR;
        }

        List<SPObservationID> obsIDs = new ArrayList<>(1);
        obsIDs.add(obsID);
        try {
            OlConfiguration obsLogConfig = getObsLogConfiguration();

            OlObservationLogRequestExecutor exec = new OlObservationLogRequestExecutor(getPersistenceManager(), obsLogConfig, obsIDs, _user);
            exec.execute();
            _observingLog = exec.getObservingLog();
        } catch (OlLogException ex) {
            // node is null so this cannot happen
            addActionError("Observing Log generation caused an exception: " + ex);
        }

        if (_observingLog == null) {
            addActionError("No ObservingLog was generated for observation: " + _observationID);
            return ERROR;
        }

        return SUCCESS;
    }

}



