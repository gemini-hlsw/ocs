//
// $
//

package edu.gemini.spModel.too;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPNodeNotLocalException;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;


import java.security.Principal;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class TooConstraintFunctor extends DBAbstractFunctor {
    private static final Logger LOG = Logger.getLogger(TooConstraintFunctor.class.getName());

    private SPSiteQuality.TimingWindow _win;

    public TooConstraintFunctor(SPSiteQuality.TimingWindow win) {
        _win = win;
    }

    public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        ISPObservation obs = (ISPObservation) node;

        try {
            TooConstraintService.addTimingWindow(obs, db.getFactory(), _win);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Remote exception in local method call", ex);
        }
    }

    /**
     * Executes
     * {@link TooConstraintService#addTimingWindow(edu.gemini.pot.sp.ISPObservation, edu.gemini.pot.sp.ISPFactory, edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow)}
     * in a functor.
     *
     * @param db
     * @param obs
     * @param win (optional) explicit timing window to use; will default to
     * the default timing window for the TOO type and be added if no window
     * already has been specified in the observation
     */
    public static void addWindow(IDBDatabaseService db, ISPObservation obs, SPSiteQuality.TimingWindow win, Set<Principal> user)  {
        TooConstraintFunctor func = new TooConstraintFunctor(win);

        try {
            db.getQueryRunner(user).execute(func, obs);
        } catch (SPNodeNotLocalException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
