//
// $
//

package edu.gemini.too.window;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.too.TooConstraintFunctor;
import edu.gemini.too.event.api.TooEvent;
import edu.gemini.too.event.api.TooSubscriber;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 */
public final class TooWindowChecker implements TooSubscriber {
    private static final Logger LOG = Logger.getLogger(TooWindowChecker.class.getName());

    private final IDBDatabaseService _db;
    private final Set<Principal> _user;

    public TooWindowChecker(IDBDatabaseService db, Set<Principal> user) {
        _db = db;
        _user = user;
    }

    public void tooObservationReady(TooEvent evt) {
        final SPObservationID obsId = evt.report().getObservationId();
        if (obsId == null) {
            LOG.warning("Received a ToO event without an obs id.");
            return;
        }

        final ISPObservation obs = _db.lookupObservationByID(obsId);
        if (obs == null) {
            LOG.warning("Received a ToO event for an observation not in the database: " + obsId);
            return;
        }


        TooConstraintFunctor.addWindow(_db, obs, null, _user);
    }
}
