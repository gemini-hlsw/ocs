//
// $Id: ObsRecordTriggerCondition.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.impl.trigger;

import edu.gemini.pot.sp.SPCompositeChange;
import edu.gemini.pot.spdb.IDBTriggerCondition;

import java.io.ObjectStreamException;
import java.util.logging.Logger;

/**
 * A trigger condition fired whenever a dataset record's QA state is updated.
 * This condition is registered with the database such that when it occurs, the
 * matching {@link ObsRecordTriggerAction} is executed to send a notification to
 * the {@link ObsRecordTriggerHandler}.
 */
final class ObsRecordTriggerCondition implements IDBTriggerCondition {
    private static final Logger LOG = Logger.getLogger(ObsRecordTriggerCondition.class.getName());

    private static final long serialVersionUID = 1l;

    /**
     * There only needs to be one instance.
     */
    public static final ObsRecordTriggerCondition INSTANCE = new ObsRecordTriggerCondition();

    private ObsRecordTriggerCondition() {
    }

    public Object matches(SPCompositeChange change) {
        LOG.fine("Checking for ObsRecord update.");
        return DsetRecordChangeBuilder.create(change);
    }

    @SuppressWarnings({"MethodMayBeStatic", "UNUSED_THROWS"})
    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }
}
