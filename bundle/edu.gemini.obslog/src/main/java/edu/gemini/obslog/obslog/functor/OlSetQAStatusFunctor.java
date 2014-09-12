package edu.gemini.obslog.obslog.functor;

import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.GeminiRuntimeException;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.obslog.ObsQaLog;

import java.security.Principal;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Gemini Observatory/AURA
 * $Id: OlSetQAStatusFunctor.java,v 1.3 2006/10/17 21:37:11 shane Exp $
 * This functor takes a list of datasets as Strings and sets all of their QAstatus values to one value
**/
public class OlSetQAStatusFunctor extends DBAbstractQueryFunctor {

    private static final Logger LOG = Logger.getLogger(OlSetQAStatusFunctor.class.getName());
    private List<DatasetLabel> _datasetLabels;
    private DatasetQaState _state;

    public OlSetQAStatusFunctor(List<DatasetLabel> datasetLabels, DatasetQaState state) {
        _datasetLabels = datasetLabels;
        _state = state;
    }

    /**
     * This method takes sets a {@link DatasetQaState} value  and sets it on all of the datasets
     * in the array of {@link DatasetLabel}.  It's known at this point that all the datasets are within the one
     * observation with observation ID given.
     * @param db the database to use
     * @param obsID an {@link SPObservationID} for the observation containing the datasets
     * @param datasets a list of {@link DatasetLabel} instances
     * @param state a QA state value
     * @throws OlLogException if various objects are not present.
     */
    private void _setOneObservationsDatasets(IDBDatabaseService db, SPObservationID obsID,
                                             final List<DatasetLabel> datasets, final DatasetQaState state) throws OlLogException {
        // Now get that observation
        final ISPObservation obs = db.lookupObservationByID(obsID);
        if (obs == null) {
            // Possible that there is no observation.  When running in
            // a master/slave configuration, at most one slave database
            // will contain the observation.
            LOG.log(Level.FINE, "No observation located for: " + obsID.stringValue());
            return;
        }

        // Check for an observing log component
        ObsLog.update(db, obsID, new ObsLog.UpdateOp() {
            @Override public void apply(ISPObservation obs, ObsLog log) {
                // Now  set all its datasets
                final ObsQaLog qaLog = log.qaLogDataObject;
                for (DatasetLabel label : datasets) {
                    qaLog.setQaState(label, state);
                }
            }
        });
    }

    /**
     * This private method creates a Map of observation ids and dataset labels to allow more efficient
     * and less noisy updates of the dataobjects during bulk updates
     *
     * @param datasetLabels a list of {@link DatasetLabel} objects that are data datasets to change
     * @return a  <@link Map> of <@link SPObservationID> and a <tt>List</tt> of {@link DatasetLabel> objects.
     * @throws Exception can throw a {@ParseTextException}
     */
    private Map<SPObservationID, List<DatasetLabel>> _prepare(List<DatasetLabel> datasetLabels) throws Exception {
        Map<SPObservationID, List<DatasetLabel>> obsMap = new HashMap<SPObservationID, List<DatasetLabel>>();

        for (DatasetLabel label : datasetLabels) {
            // Get the data label and turn it into an obsID
            SPObservationID obsID = label.getObservationId();
            // Look it up
            List<DatasetLabel> labels = obsMap.get(obsID);
            if (labels == null) {
                // Create a new one if nothing yet exists for this obsID
                labels = new ArrayList<DatasetLabel>();
            }
            labels.add(label);
            obsMap.put(obsID, labels);
        }

        return obsMap;
    }

    /**
     * The normal functor execute method.  First it prepares the {@link Map} and then for each entry
     * it sets the QAState
     * @param db database to use
     * @param node
     * @param principals
     */
    public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        if (_datasetLabels == null || _state == null) throw new NullPointerException();

        try {
            Map<SPObservationID, List<DatasetLabel>> obsMap = _prepare(_datasetLabels);
            // Is there anything to do?
            if (obsMap.size() == 0) return;

            for (SPObservationID observationID : obsMap.keySet()) {
                _setOneObservationsDatasets(db, observationID, obsMap.get(observationID), _state);
            }
        } catch (Exception ex) {
            //LOG.log(Level.WARNING, "Remote exception in local code!", ex);
            throw GeminiRuntimeException.newException(ex);
        }
    }

    public static void create(IDBDatabaseService db, List<DatasetLabel> datasetLabels, DatasetQaState state, Set<Principal> user) throws OlLogException {

        OlSetQAStatusFunctor lf = new OlSetQAStatusFunctor(datasetLabels, state);
        try {
            lf = db.getQueryRunner(user).execute(lf, null);
        } catch (SPNodeNotLocalException ex) {
            // node is null so this cannot happen
            throw GeminiRuntimeException.newException(ex);
        }

        if (lf.getException() != null) {
            throw new OlLogException(lf.getException());
        }
    }
}

