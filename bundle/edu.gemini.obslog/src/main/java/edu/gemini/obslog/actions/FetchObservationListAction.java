package edu.gemini.obslog.actions;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.obslog.IObservingLog;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.executor.OlObservationLogRequestExecutor;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPBadIDException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: FetchObservationListAction.java,v 1.5 2005/12/11 15:54:15 gillies Exp $
//

public class FetchObservationListAction extends OlBaseAction {
    private static final Logger LOG = Logger.getLogger(FetchObservationAction.class.getName());

    private IObservingLog _observingLog;
    private List<String> _observationIDs;

    public IObservingLog getObservingLog() {
        return _observingLog;
    }

    public void setObservationIDs(List<String> observationIDs) {
        if (observationIDs == null) throw new NullPointerException("null observation id list");
        _observationIDs = observationIDs;
    }

    public String execute() {
        if (_observationIDs == null) {
            addActionError("No observation id was set.");
            return ERROR;
        }

        _observingLog = null;
        List<SPObservationID> obsIDs = _convertIDs(_observationIDs);

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
            addActionError("No ObservingLog was generated for an observation list.");
            return ERROR;
        }

        return SUCCESS;
    }

    private List<SPObservationID> _convertIDs(List<String> stringIDs) throws NullPointerException {
        List<SPObservationID> result = new ArrayList<SPObservationID>();
        int listSize = stringIDs.size();
        if (listSize == 0) return result;

        SPObservationID obsID = null;
        try {
            for (int i = 0; i < listSize; i++) {
                obsID = new SPObservationID(stringIDs.get(i));
                result.add(obsID);
            }
        } catch (SPBadIDException ex) {
            LOG.severe(ex.getMessage() + ex);
            addActionError("Improper observation id in list: " + obsID.toString());
        }

        return result;
    }

}

