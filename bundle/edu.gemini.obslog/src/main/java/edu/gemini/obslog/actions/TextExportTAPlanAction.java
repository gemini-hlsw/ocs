package edu.gemini.obslog.actions;

import edu.gemini.obslog.TextExport.TextOnlySegmentExporter;
import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.obslog.IObservingLog;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.executor.OlTARequestExecutor;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;

//
// Gemini Observatory/AURA
// $Id: TextExportTAPlanAction.java,v 1.2 2006/08/25 20:13:40 shane Exp $
//

public class TextExportTAPlanAction extends OlBaseAction {
    //private static final Logger LOG = LogUtil.getLogger(TextExportTAPlanAction.class);

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

    public void validate() {
        if (_planID == null) {
            addActionError("No plan ID was set.");
        }
    }

    public String execute() {
        _exportResult = "No report was made";

        SPProgramID planID;
        // Try to open as a normal plan id
        try {
            planID = SPProgramID.toProgramID(_planID);
        } catch (SPBadIDException ex) {
            addActionError("Improper plan ID during plan fetch: " + _planID);
            return ERROR;
        }

        IObservingLog _observingLog;
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

        TextOnlySegmentExporter exp = new TextOnlySegmentExporter(_observingLog, "Time Accounting Information", _options, planID);
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
