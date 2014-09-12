//
// $Id: ObsRecordTriggerHandler.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.impl.trigger;



import java.util.Collection;

/**
 * A Remote object interface for a client side receiver of
 * {@link edu.gemini.spModel.dataset.DatasetExecRecord} events.
 */
public interface ObsRecordTriggerHandler {

    /**
     * Signals to the client side remote object that one or more
     * {@link edu.gemini.spModel.dataset.DatasetExecRecord}s have been added,
     * modified, or removed.
     *
     * @param update collection of updates to
     * {@link edu.gemini.spModel.dataset.DatasetExecRecord}s
     */
    void obsRecTrigger(Collection<DsetRecordChange> update) ;
}
