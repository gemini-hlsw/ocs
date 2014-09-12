package edu.gemini.obslog.obslog.executor;

import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.functor.OlSetQAStatusFunctor;
import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.type.SpTypeUtil;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;


import java.util.List;

//
// Gemini Observatory/AURA
// $Id: OlUpdateQAStateExecutor.java,v 1.2 2006/06/12 05:06:59 gillies Exp $
//

public class OlUpdateQAStateExecutor {
    public static final Logger LOG = Logger.getLogger(OlUpdateQAStateExecutor.class.getName());

    private OlPersistenceManager _persistenceManager;
    private List<DatasetLabel> _datasetIDs;
    private String _QAState;
    private final Set<Principal> _user;

    public OlUpdateQAStateExecutor(OlPersistenceManager persistenceManager, List<DatasetLabel> datasetIDs, String QAstate, Set<Principal> user) {
        _persistenceManager = persistenceManager;
        _datasetIDs = datasetIDs;
        _QAState = QAstate;
        _user = user;
    }

    /**
     * Bulk operation to sets the QA state on a list of datasets.
     *
     * @throws edu.gemini.obslog.obslog.OlLogException if an error occurs while doing the update
     */
    public void execute() throws OlLogException {
        if (_datasetIDs == null) {
            throw new OlLogException("Dataset ID list is null");
        }

        // Update all datasets to the same QA state
        DatasetQaState QAstate = SpTypeUtil.oldValueOf(DatasetQaState.class,  _QAState, DatasetQaState.PASS);
        OlSetQAStatusFunctor.create(_persistenceManager.getDatabase(), _datasetIDs, QAstate, _user);
    }

}
