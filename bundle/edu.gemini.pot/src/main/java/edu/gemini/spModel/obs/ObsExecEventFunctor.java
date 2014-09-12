package edu.gemini.spModel.obs;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPNodeNotLocalException;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.GeminiRuntimeException;
import edu.gemini.spModel.event.ObsExecEvent;

import java.security.Principal;
import java.util.Set;


/**
 * Wraps the {@link }ObsExecEventHandler} in a functor so that all the
 * work is performed in the database.
 */
public final class ObsExecEventFunctor extends DBAbstractFunctor {
    private final ObsExecEvent event;

    public ObsExecEventFunctor(ObsExecEvent event) {
        this.event = event;
    }

    public void execute(IDBDatabaseService database, ISPNode node, Set<Principal> principals) {
        ObsExecEventHandler.handle(event, database);
    }

    public static void handle(ObsExecEvent event, Set<Principal> principals)  {
        handle(event, SPDB.get(), principals);
    }

    public static void handle(ObsExecEvent event, IDBDatabaseService db, Set<Principal> principals)  {
        ObsExecEventFunctor func = new ObsExecEventFunctor(event);
        try {
            db.getQueryRunner(principals).execute(func, null);
        } catch (SPNodeNotLocalException ex) {
            throw GeminiRuntimeException.newException(ex);
        }

    }
}
