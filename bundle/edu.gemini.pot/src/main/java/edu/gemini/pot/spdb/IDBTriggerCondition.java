//
// $Id: IDBTriggerCondition.java 6657 2005-10-03 23:04:25Z shane $
//
package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.SPCompositeChange;

import java.io.Serializable;

/**
 * This interface defines a mechanism whereby the client may specify the
 * conditions under which a trigger is fired.  All the triggerable events
 * start their life as an ordinary {@link SPCompositeChange} in a particular
 * {@link edu.gemini.pot.sp.ISPNode node}.  The implementation of this
 * class must discern those events which are of interest to it.  For example,
 * one implementation might be to look for those events that are data object
 * stores for an {@link edu.gemini.pot.sp.ISPObservation} in which a particular
 * value has changed.
 *
 * <p>When a trigger condition is matched, the associated
 * {@link IDBTriggerAction} registered with it is executed.
 */
public interface IDBTriggerCondition extends Serializable {

    /**
     * Determines whether the given SPCompositeChange represents an event that
     * should generate a {@link IDBTriggerAction trigger action}.
     *
     * @param change a change event fired because of a modification to a
     * remote node
     *
     * @return an arbitrary non-null object if the given change should generate
     * a trigger, <code>null</code> otherwise; the object returned will be
     * passed to the corresponding {@link IDBTriggerAction}
     */
    Object matches(SPCompositeChange change);
}
