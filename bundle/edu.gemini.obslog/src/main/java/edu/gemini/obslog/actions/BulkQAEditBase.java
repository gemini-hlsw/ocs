package edu.gemini.obslog.actions;

import edu.gemini.spModel.dataset.DatasetLabel;
import org.apache.struts2.interceptor.SessionAware;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: BulkQAEditBase.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public class BulkQAEditBase extends OlBaseAction implements SessionAware {
    private static final Logger LOG = Logger.getLogger(BulkQAEditBase.class.getName());

    private Map _session;
    // Note that the private bulkID is needed here to
    private String _bulkID;
    private String _stepName;

    public BulkQAEditBase(String stepName) {
        _stepName = stepName;
    }

    /**
     * Method implementing {@link org.apache.struts2.interceptor.SessionAware}.
     * Receives the current session.  Used to store bulk edit information
     *
     * @param session a {@link java.util.Map} of objects in the session
     */
    public void setSession(Map session) {
        _session = session;
    }

    /**
     * Create a bulk context for sharing between Bulk Edit steps
     *
     * @param allDatasetIDs the list of all the datasets in the initial query
     */
    protected void createBulkContext(List<String> allDatasetIDs) {
        createBulkContext(null, allDatasetIDs);
    }

    @SuppressWarnings("unchecked")
    protected void createBulkContext(String planID, List<String> allDatasetIDs) {
        if (LOG.isLoggable(Level.FINE)) LOG.fine("Create  bulk context in: " + _stepName);
        // Now create a bulk context and store it in the Session
        BulkEditContext bulkContext = new BulkEditContext(planID, allDatasetIDs);
        _bulkID = bulkContext.getID();
        _session.put(_bulkID, bulkContext);
    }

    /**
     * Method to fetch the <tt>BulkEditContext</tt> in subclasses
     *
     * @return a BulkEditContext or null if the session doesn't exist
     */
    protected BulkEditContext getBulkEditContext() {
        if (LOG.isLoggable(Level.FINE)) LOG.fine("getBulkEditContext: " + _stepName);
        Map session = getSession();
        if (session == null) {
            LOG.info("Session null in getBulkEditContext: " + _stepName);
            return null;
        }
        BulkEditContext bulkEditContext = (BulkEditContext) session.get(_bulkID);
        if (bulkEditContext == null) {
            LOG.info("Bulk edit context for: " + _bulkID + " is null");
        }
        return bulkEditContext;
    }

    /**
     * A convenience method to call from validate methods to ensure that the context is set properly
     */
    protected void validateSession() {
        if (LOG.isLoggable(Level.FINE)) LOG.fine("validateSession: " + _stepName);
        // First see if the session is null
        if (getSession() == null) {
            addActionError("Session is null for BulkEdit step:" + _stepName);
            return;
        }

        // Now  see if a bulkID was set, which it must be at this step
        if (_bulkID == null) {
            addActionError("Bulk ID is not set for step: " + _stepName);
            return;
        }

        BulkEditContext bulkContext = getBulkEditContext();
        // Check to see that the thing returned is not null
        if (bulkContext == null) {
            addActionError("Bulk Context: " + _bulkID + " is null for step: " + _stepName);
        }
    }

    /**
     * Removes the current bulk session from this request
     */
    protected void cleanupBulkContext() {
        // Remove session info
        String bulkID = getBulkID();
        if (LOG.isLoggable(Level.FINE)) LOG.fine("Remove: " + bulkID + " in step: " + _stepName);
        _session.remove(bulkID);
    }

    /**
     * Returns the current session
     *
     * @return the Session {@link Map}
     */
    private Map getSession() {
        return _session;
    }

    /**
     * Set the bulk ID to be used in this step.  Usually passed between steps via WebWork set/get
     *
     * @param bulkID the bulk ID
     */
    public void setBulkID(String bulkID) {
        if (LOG.isLoggable(Level.FINE)) LOG.fine("Set bulk ID in step: " + _stepName + " : " + bulkID);
        _bulkID = bulkID;
    }

    /**
     * Return the bulk ID in this context
     *
     * @return a String bulk ID
     */
    public String getBulkID() {
        if (LOG.isLoggable(Level.FINE)) LOG.fine("Get bulk ID in step: " + _stepName + " : " + _bulkID);
        return _bulkID;
    }

    public void dump() {
         int numIDs = getBulkEditContext().getBulkSize();
         if (numIDs == 0) LOG.info("No datasets for editing in context");

         LOG.info("BULK edit size is: " + numIDs);

         for (DatasetLabel value : getBulkEditContext().getBulkDatasetLabels()) {
             LOG.info("Edit dataset= " + value);
         }
    }
}



