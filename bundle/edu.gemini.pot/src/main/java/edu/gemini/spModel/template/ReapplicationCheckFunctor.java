package edu.gemini.spModel.template;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.gemini.security.UserRolePrivileges;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obslog.ObsLog;

import static edu.gemini.spModel.obs.ObservationStatus.*;


import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.gemini.spModel.template.FunctorHelpers.lookupNode;

// This is run as a pre-flight check before showing the reapply dialog. The canReapply() method is later
// called from the ReapplicationFunctor as a final check.
public class ReapplicationCheckFunctor extends DBAbstractFunctor {

    private static final Logger LOGGER = Logger.getLogger(ReapplicationCheckFunctor.class.getName());

    private final UserRolePrivileges urps;
    private final Set<ISPObservation> selection = new HashSet<ISPObservation>();
    private final Map<ISPObservation, Boolean> results = new HashMap<ISPObservation, Boolean>();

    public ReapplicationCheckFunctor(UserRolePrivileges urps) {
        this.urps = urps;
    }

    public void add(ISPObservation obsNode) {
        selection.add(obsNode);
    }

    public void execute(IDBDatabaseService db, ISPNode ignored, Set<Principal> principals) {
        try {
            for (ISPObservation obs : selection)
                results.put(obs, canReapply(db, urps, obs));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Trouble during template reapply check.", e);
            setException(e);
        }
    }

    public Map<ISPObservation, Boolean> getResults() {
        return results;
    }

    // N.B. this is also called from ReapplicationFunctor
    static boolean canReapply(IDBDatabaseService db, UserRolePrivileges urps, ISPObservation obs)  {

        // Make sure there's an originating template
        final SPObservation obsData = (SPObservation) obs.getDataObject();
        final SPNodeKey key = obsData.getOriginatingTemplate();
        if (key == null) return false;

        // And make sure that template exists
        final ISPProgram prog = db.lookupProgram(obs.getProgramKey());
        final ISPObservation templateObs = (ISPObservation) lookupNode(key, prog);
        if (templateObs == null) return false;

        // Make sure that it doesn't have an obslog.
        if (!ObsLog.isEmpty(obs)) return false;

        final ObservationStatus max = maxReapplyStatus(urps);
        if (max == null) return false;

        return ObservationStatus.computeFor(obs).compareTo(max) <= 0;
    }

    // We'll consider it enough to have an obslog at all, whether it contains
    // datasets or not.  If an observation has an obslog, it was at least once
    // slewed for observing and the events are recorded.  In reality, only
    // observations that are ongoing or beyond will have an obslog anyway.
    private static boolean containsObsLog(ISPObservation obs)  {
        return obs.getObsExecLog() != null;
    }

    // PI fetch userType
    // Reapply only observations with observation status Phase 2
    //
    // NGO fetch userType Reapply only allowed for observations at In Review, For
    // Review, or Phase 2. Any other observations must not be be selectable for
    // Reapply.
    //
    // Staff (OTR, OT -onsite) - Reapply allowed for all observations that are not at
    // status Observed or Ongoing.  (also includes inactive)
    private static ObservationStatus maxReapplyStatus(UserRolePrivileges urps) {
        switch (urps) {
            case PI:    return PHASE2;
            case NGO:   return IN_REVIEW;
            case STAFF: return READY;
            default:    return null;  // sorry
        }
    }
}
