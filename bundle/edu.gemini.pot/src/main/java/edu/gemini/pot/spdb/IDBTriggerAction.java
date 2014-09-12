//
// $Id: IDBTriggerAction.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.SPCompositeChange;

import java.io.Serializable;


/**
 * Describes the action to perform when a {@link IDBTriggerCondition} is met.
 */
public interface IDBTriggerAction extends Serializable {

    /**
     * Performs the desired action is response to the
     * {@link IDBTriggerCondition} being met
     *
     * @param change the change that generated the trigger
     * @param handback object returned by the corresponding
     * {@link IDBTriggerCondition
     */
    void doTriggerAction(SPCompositeChange change, Object handback)
            ;
}
