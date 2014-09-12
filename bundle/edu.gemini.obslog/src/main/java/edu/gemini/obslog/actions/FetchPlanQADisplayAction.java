package edu.gemini.obslog.actions;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.obslog.*;
import edu.gemini.obslog.obslog.executor.OlQAPlanRequestExecutor;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.dataset.DatasetQaState;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: FetchPlanQADisplayAction.java,v 1.5 2006/08/25 20:13:40 shane Exp $
//

public class FetchPlanQADisplayAction extends BulkQAEditBase {
    private static final Logger LOG = Logger.getLogger(FetchPlanQADisplayAction.class.getName());

    private String _planID;
    private IObservingLog _observingLog;

    public FetchPlanQADisplayAction() {
        super("FetchPlanQADisplayAction");
    }

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

     /**
     * Return the names of the QA states for  the view
     *
     * @return A list of  values that are the names of each of the available QA Status values.
     */
    public List<String> getQAStates() {
        List<String> states = new ArrayList<String>();
        for (DatasetQaState type : DatasetQaState.values()) {
            states.add(type.displayValue());
        }
        return states;
    }

    public int getDatasetCount() {
        int count = 0;
        for (IObservingLogSegment segment : getObservingLog().getLogSegments()) {
            count += segment.getSize();
        }
        return count;
    }

    public void validate() {
        if (_planID == null) {
            addActionError("No plan ID was set.");
        }
    }

    private List<String> _getAllDatasetIDs() {
        List<String> datasetIDs = new ArrayList<String>();
        // I hate this, but another way would require casting or a major rewrite
        for (IObservingLogSegment seg : getObservingLog().getLogSegments()) {
            List<ConfigMap> rows = seg.getRows();
            for (ConfigMap row : rows) {
                datasetIDs.add(row.sget(ConfigMapUtil.DATA_LABELS_ITEM_NAME));
            }
        }
        LOG.info("Found: " + datasetIDs.size() + " datasetIDs");
        return datasetIDs;
    }

    public String execute() {
        LOG.info("PLAN ID: " + _planID);

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

            OlQAPlanRequestExecutor exec = new OlQAPlanRequestExecutor(getPersistenceManager(), obsLogConfig, planID, _user);
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

        // Now create a bulk context and store it in the Session
        createBulkContext(_planID, _getAllDatasetIDs());

        return SUCCESS;
    }

}
