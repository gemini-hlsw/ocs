package edu.gemini.obslog.actions;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.dataset.DatasetLabel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Gemini Observatory/AURA
 * $Id: BulkEditContext.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
 * This class provides some persistent scope for the bulk edit QA wizard/workflow. Information is stored between steps
 * and retrieved.
 */
public class BulkEditContext implements Serializable {
    private static final Logger LOG = Logger.getLogger(BulkEditContext.class.getName());

    static final String BULK_EDIT_PREFIX = "__BKQA";

    private String _id;
    private String _planID;
    private int _numberBulkDatasets;
    private Map<SPObservationID, List<DatasetLabel>> _bulkMap;
    private Map<SPObservationID, List<DatasetLabel>> _allMap;

    public BulkEditContext(List<String> allDatasetIDs) {
        this(null, allDatasetIDs);
    }

    public BulkEditContext(String planID, List<String> allDatasetIDs) {
        _planID = planID;
         _id = _createPlanID();
        _allMap = _prepare(allDatasetIDs);
    }


    private String _createPlanID() {
        return BULK_EDIT_PREFIX  + String.valueOf(System.currentTimeMillis());
    }

    public void addBulkDatasetIDs(List<String> bulkIDs) {
        if (bulkIDs == null) {
            LOG.severe("Nullfor bulkID String array");
            return;
        }
        _numberBulkDatasets = bulkIDs.size();
        //_bulkEditDatasetIDs = new ArrayList<String>(bulkIDs);
        _bulkMap = _prepare(bulkIDs);
    }

    /**
     * This private method creates a Map of observation ids and dataset labels to allow more efficient
     * and less noisy updates of the dataobjects during bulk updates
     *
     * @param datasetIDs a list of dataset IDs as Strings
     * @return a  <@link Map> of <@link SPObservationID> and a <tt>List</tt> of {@link edu.gemini.spModel.dataset.DatasetLabel> objects.
     * @throws Exception can throw a {@ParseTextException}
     */
    private Map<SPObservationID, List<DatasetLabel>> _prepare(List<String> datasetIDs) {
        Map<SPObservationID, List<DatasetLabel>> obsMap = new HashMap<SPObservationID, List<DatasetLabel>>();

        for (String datasetID : datasetIDs) {
            // Get the data label and turn it into an obsID
            try {
                DatasetLabel label = new DatasetLabel(datasetID);
                SPObservationID obsID = label.getObservationId();
                // Look it up
                List<DatasetLabel> labels = obsMap.get(obsID);
                if (labels == null) {
                    // Create a new one if nothing yet exists for this obsID
                    labels = new ArrayList<DatasetLabel>();
                }
                labels.add(label);
                obsMap.put(obsID, labels);
            } catch (java.text.ParseException ex) {
                LOG.severe("Improper dataset in context: " + datasetID);
            }
        }
        return obsMap;
    }

    /**
     * Return the id for this context
     */
    public String getID() {
        return _id;
    }

    /**
     * Set the plan ID as a String
     */
    public void setPlanID(String planID) {
        _planID = planID;
    }

    /**
     * Return the plan ID as a {@link String}.  This is suboptimal because in some future cases, there will not be a
     * planID.  Until I understand that part of the project, this will be needed for navigation.
     * @return plan ID as String
     */
    public String getPlanID() {
        return _planID;
    }

    /**
     * The number of datasets being edited is returned.
     * @return the number of datasets
     */
    protected int getBulkSize() {
        return _numberBulkDatasets;
    }

    /**
     * Returns the datasets being edited.
     * @return a list of {@link DatasetLabel} instances, one for each dataset
     */
    public List<DatasetLabel> getBulkDatasetLabels() {
        List<DatasetLabel> allLabels = new ArrayList<DatasetLabel>();
        for (SPObservationID obsID :  _bulkMap.keySet()) {
            List<DatasetLabel> labels = _bulkMap.get(obsID);
            allLabels.addAll(labels);
        }
        return allLabels;
    }

    /**
     * Return the observations that have datasets being edited
     * @return a {@link List} of {@link SPObservationID} objects.
     */
    public List<SPObservationID> getBulkObservationIDs() {
        return new ArrayList<SPObservationID>(_bulkMap.keySet());
    }

    /**
     * Return all the observations that were included in the initial request.
     * @return a {@link List} of {@link SPObservationID} objects.
     */
    public List<SPObservationID> getAllObservationIDs() {
        return new ArrayList<SPObservationID>(_allMap.keySet());
    }
}
