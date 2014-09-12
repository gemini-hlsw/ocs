package edu.gemini.obslog.actions;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.obslog.IObservingLog;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.executor.OlTARequestExecutor;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;

import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: FetchPlanTADisplayAction.java,v 1.2 2006/08/25 20:13:40 shane Exp $
//

public class FetchPlanTADisplayAction extends OlBaseAction {
    private static final Logger LOG = Logger.getLogger(FetchPlanTADisplayAction.class.getName());

    private String _planID;
    private IObservingLog _observingLog;
    //private OlLogOptions _options = new OlLogOptions();

    // Notice that this planID is not the one in the base class.  This is the action that sets the
    // session variable once the fetch is successful
    public void setPlanID(String planID) {
        _planID = planID;
    }

    public String getPlanID() {
        return _planID;
    }

    public IObservingLog getObservingLog() {
        return _observingLog;
    }

    public void validate() {
        if (_planID == null) {
            addActionError("No plan ID was set.");
        }
    }

    public String execute() {
        _observingLog = null;

        SPProgramID planID;
        // Try to open as a normal plan id
        try {
            planID = SPProgramID.toProgramID(_planID);
        } catch (SPBadIDException ex) {
            addActionError("Improper plan ID during plan fetch: " + _planID);
            return ERROR;
        }

        try {
            OlConfiguration obsLogConfig = getObsLogConfiguration();

            OlTARequestExecutor exec = new OlTARequestExecutor(getPersistenceManager(), obsLogConfig, planID, _user);
            exec.execute();
            _observingLog = exec.getObservingLog();
        } catch (OlLogException ex) {
            addActionError("Observing Log generation caused an exception: " + ex);
            return ERROR;
        }

        if (_observingLog == null) {
            addActionError("No ObservingLog was generated for plan: " + _planID);
            return ERROR;
        }

        return SUCCESS;
    }

}
