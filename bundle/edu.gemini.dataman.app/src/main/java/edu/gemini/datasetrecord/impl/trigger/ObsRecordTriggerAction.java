//
// $Id: ObsRecordTriggerAction.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.impl.trigger;

import edu.gemini.pot.sp.SPCompositeChange;
import edu.gemini.pot.spdb.IDBTriggerAction;


import java.util.Collection;

/**
 * The {@link edu.gemini.pot.spdb.IDBTriggerAction} implementation for
 * {@link edu.gemini.spModel.dataset.DatasetExecRecord} events.  This code runs
 * in the ODB when the {@link ObsRecordTriggerCondition} is met.  It simply
 * notifies the client, via a remote reference that it holds, that a
 * given set of DatasetRecords have been updated.
 */
final class ObsRecordTriggerAction implements IDBTriggerAction {
//    private static final Logger LOG = Logger.getLogger(ObsRecordTriggerAction.class.getName());

    private static final long serialVersionUID = 1l;

    // Remote reference to the handler running in the client.
    private ObsRecordTriggerHandler _handler;

    /**
     * Constructs with a remote reference to the client-side handler.
     */
    public ObsRecordTriggerAction(ObsRecordTriggerHandler handler) {
        _handler = handler;
    }

    public void doTriggerAction(SPCompositeChange change, Object handback) {

        Collection<DsetRecordChange> updates;
        updates = (Collection<DsetRecordChange>) handback;
        if (updates != null) {
            _handler.obsRecTrigger(updates);
        }
    }
}
