//
// $Id: ObsRecordTriggerClient.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.impl.trigger;

import edu.gemini.pot.spdb.IDBDatabaseService;

import java.util.HashSet;
import java.util.Set;

/**
 * The ObsRecordTriggerClient hides the implementation details involved in
 * working with triggers  in the ODB.  Namely, it registers the
 * {@link ObsRecordTriggerCondition} and {@link ObsRecordTriggerAction} with
 * the ODB, and maintains the associated Lease on the registration.
 */
public final class ObsRecordTriggerClient {
    private final Set<IDBDatabaseService> _dbs = new HashSet<IDBDatabaseService>();
    private final ObsRecordTriggerCondition cond = ObsRecordTriggerCondition.INSTANCE;
    private final ObsRecordTriggerAction action;

    private boolean _enabled = false;

    public ObsRecordTriggerClient(ObsRecordTriggerHandler handler) {
        action = new ObsRecordTriggerAction(handler);
    }

    private synchronized void _start() {
        if (!_enabled) return;
        for (IDBDatabaseService db: _dbs)
            db.registerTrigger(cond, action);
    }

    private synchronized void _stop() {
        for (IDBDatabaseService db: _dbs)
            db.unregisterTrigger(cond, action);
    }

    public synchronized boolean isEnabled() {
        return _enabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        if (enabled == _enabled) return;
        _enabled = enabled;
        if (enabled) {
            _start();
        } else {
            _stop();
        }
    }

    public synchronized void addDatabase(IDBDatabaseService database) {
        _dbs.add(database);
        _start();
    }

    public synchronized void removeDatabase(IDBDatabaseService database) {
        database.unregisterTrigger(cond, action);
        _dbs.remove(database);
    }

}
