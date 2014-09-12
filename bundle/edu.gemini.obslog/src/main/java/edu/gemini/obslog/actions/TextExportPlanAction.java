package edu.gemini.obslog.actions;

import edu.gemini.obslog.TextExport.TextObslogExporter;
import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.obslog.IObservingLog;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.executor.OlPlanLogRequestExecutor;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;

import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: TextExportPlanAction.java,v 1.5 2006/12/05 14:56:16 gillies Exp $
//

public class TextExportPlanAction extends OlBaseAction {
    private static final Logger LOG = Logger.getLogger(TextExportPlanAction.class.getName());

    private String _planID;
    private String _exportResult;
    private OlLogOptions _options = new OlLogOptions();

    // Notice that this planID is not the one in the base class.  This is the action that sets the
    // session variable once the fetch is successful
    public void setPlanID(String planID) {
        _planID = planID;
    }

    public String getPlanID() {
        return _planID;
    }

    public String getReport() {
        return _exportResult;
    }

    public String execute() {
        if (_planID == null) {
            addActionError("No plan ID was set.");
            return ERROR;
        }
        LOG.info("PLAN ID: " + _planID);
        _exportResult = "No report was made";

        SPProgramID planID;
        // Try to open as a normal plan id
        try {
            planID = SPProgramID.toProgramID(_planID);
        } catch (SPBadIDException ex) {
            addActionError("Improper plan ID during plan fetch: " + _planID);
            return ERROR;
        }

        IObservingLog observingLog;
        try {
            OlConfiguration obsLogConfig = getObsLogConfiguration();

            OlPlanLogRequestExecutor exec = new OlPlanLogRequestExecutor(getPersistenceManager(), obsLogConfig, planID, _user);
            exec.execute();
            observingLog = exec.getObservingLog();
        } catch (OlLogException ex) {
            addActionError("Observing Log generation caused an exception: " + ex);
            return ERROR;
        }

        if (observingLog == null) {
            addActionError("No ObservingLog was generated for plan: " + _planID);
            return ERROR;
        }

        TextObslogExporter exp = new TextObslogExporter(observingLog, _options, planID);
        String result;
        try {
            result = exp.export();
        } catch (OlLogException ex) {
            addActionError("Failed while exporting observing log: " + _planID);
            return ERROR;
        }
        _exportResult = result;

        return SUCCESS;
    }

}
