package edu.gemini.obslog.actions;

import edu.gemini.obslog.obslog.executor.OlUpdateQAStateExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

//
// Gemini Observatory/AURA
// $Id: SetQAStateAction.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public class SetQAStateAction extends BulkQAEditBase {
    private static final Logger LOG = Logger.getLogger(SetQAStateAction.class.getName());
    private String _state;
    private String _planID;
    private List<String> _bulkDatasets;

    public SetQAStateAction() {
        super("SetQAStateAction");
    }

    public void setQAState(String state) {
        _state = state;
    }

    public String getPlanID() {
        return _planID;
    }

    public void setPlanID(String planID) {
        _planID = planID;
    }

    /**
     * This is  used by Webwork to add the list of selected datasets
     *
     * @param datasetIDs a list of <tt>String</tt>s created by Webwork.
     *                   Note that this happens before validate is called.
     */
    public void setBulk(List<String> datasetIDs) {
        if (LOG.isLoggable(Level.FINE)) LOG.fine("Set Bulk with size: " + datasetIDs.size());
        _bulkDatasets = new ArrayList<String>();
        _bulkDatasets.addAll(datasetIDs);
    }

    public void validate() {
        validateSession();
        if (_state == null) {
            addFieldError("state", "is set to null");
            return;
        }
    }

    public String execute() throws Exception {
        BulkEditContext bulkContext = getBulkEditContext();

        bulkContext.addBulkDatasetIDs(_bulkDatasets);
        if (LOG.isLoggable(Level.FINE)) LOG.fine("After add bulk edit has size: " + bulkContext.getBulkSize());

        OlUpdateQAStateExecutor updater = new OlUpdateQAStateExecutor(getPersistenceManager(), bulkContext.getBulkDatasetLabels(), _state, _user);
        updater.execute();

        // This cleans up the context since it is currently redirected to the initial page
        cleanupBulkContext();

        return SUCCESS;
    }

}



