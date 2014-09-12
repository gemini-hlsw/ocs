package edu.gemini.obslog.actions;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.obslog.IObservingLog;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.executor.OlPlanLogRequestExecutor;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.util.NightlyProgIdGenerator;

import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: FetchPlanAction.java,v 1.12 2006/08/25 20:13:40 shane Exp $
//

public class FetchPlanAction extends OlBaseAction {
    private static final Logger LOG = Logger.getLogger(FetchPlanAction.class.getName());

    private String _planID;
    private IObservingLog _observingLog;
    private OlLogOptions _options = new OlLogOptions();

    static private final String HINT_TODAY = "today";
    static private final String HINT_YESTERDAY = "yesterday";
    static private final String HINT_TWODAYSAGO = "two_days_ago";

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

    // Special handler for convenience planIDs
    private SPProgramID _handleSpecialPlanID(String hint) {
        if (hint == null) return null;

        if (!HINT_TODAY.equals(hint) && !HINT_YESTERDAY.equals(hint) && !HINT_TWODAYSAGO.equals(hint)) return null;

        Calendar cal = Calendar.getInstance();
        // Default case of "today" passes through
        if (_planID.equals(HINT_YESTERDAY)) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        } else if (_planID.equals(HINT_TWODAYSAGO)) {
            cal.add(Calendar.DAY_OF_YEAR, -2);
        }

        return NightlyProgIdGenerator.getProgramID("PLAN", _options.getGeminiSite(), cal.getTimeInMillis());
    }

    public String execute() {
        if (_planID == null) {
            addActionError("No plan ID was set.");
            return ERROR;
        }
        LOG.info("PLAN ID: " + _planID);

        _observingLog = null;

        SPProgramID planID = _handleSpecialPlanID(_planID);
        if (planID != null) {
            // Replace the planid with the real planid
            _planID = planID.stringValue();
        } else {
            // Try to open as a normal plan id
            try {
                planID = SPProgramID.toProgramID(_planID);
            } catch (SPBadIDException ex) {
                addActionError("Improper plan ID during plan fetch: " + _planID);
                return ERROR;
            }
        }

        try {
            OlConfiguration obsLogConfig = getObsLogConfiguration();

            OlPlanLogRequestExecutor exec = new OlPlanLogRequestExecutor(getPersistenceManager(), obsLogConfig, planID, _user);
            exec.execute();
            _observingLog = exec.getObservingLog();
        } catch (OlLogException ex) {
            LOG.log(Level.INFO, "Got this exception!", ex);
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
