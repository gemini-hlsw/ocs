//
// $Id: TooUpdateFunctor.java 624 2006-11-24 18:41:19Z shane $
//

package edu.gemini.spdb.rapidtoo.impl;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPNodeNotLocalException;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.*;
import edu.gemini.spdb.rapidtoo.*;
import edu.gemini.util.security.auth.keychain.KeyService;


import java.security.Principal;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;

/**
 *
 */
public final class TooUpdateFunctor extends DBAbstractFunctor implements IDBParallelFunctor {
    private static final Logger LOG = Logger.getLogger(TooUpdateFunctor.class.getName());

    private TooUpdate _update;
    private TooUpdateException _ex;
    private ISPObservation _obs;
    private SPObservationID _obsId;
    private final KeyService _ks;

    public TooUpdateFunctor(TooUpdate update, KeyService keyService) {
        _update = update;
        _ks = keyService;
    }

    public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        try {
            _obs = new TooUpdateServiceImpl(_ks).handleUpdate(db, _update);
            if (_obs != null) _obsId = _obs.getObservationID();
            _update = null;
        } catch (TooUpdateException e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, e.getMessage(), e);
            } else {
                LOG.log(Level.WARNING, e.getMessage());
            }
            _ex = e;
        }
    }

    public SPObservationID getObservationId() {
        return _obsId;
    }

    public void mergeResults(Collection<IDBFunctor> collection) {
        // All but at most one functor will run in a slave that does not
        // contain the program to be updated.  Find the one functor with
        // valid results, if any.
        TooUpdateException tue = null;
        ISPObservation obs = null;
        SPObservationID obsId = null;
        for (IDBFunctor db : collection) {
            TooUpdateFunctor tuf = (TooUpdateFunctor) db;
            obs = tuf._obs;
            if (obs == null) {
                // remember one of the exceptions, prefering anything other
                // than authentication exceptions since all but one database
                // in the clustered db approach will throw an auth exception
                if ((tue == null) || (tue instanceof AuthenticationException)) {
                    tue = tuf._ex;
                }
            } else {
                obsId = tuf._obsId;
                break;
            }
        }

        // There will be authentication exceptions in all the functors
        // except one
        _obs   = obs;
        _obsId = obsId;

        // If the observation wasn't updated in any database, then record the
        // exception we decided upon
        if (obs == null) _ex = tue;
    }


    public static TooUpdateFunctor update(IDBDatabaseService db, KeyService ks, TooUpdate update, Set<Principal> user)
            throws TooUpdateException {

        TooUpdateFunctor func = new TooUpdateFunctor(update, ks);
        try {
            func = db.getQueryRunner(user).execute(func, null);
            if (func._ex != null) throw func._ex;
        } catch (SPNodeNotLocalException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return func;
    }
}
