package jsky.app.ot;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.security.UserRole;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.util.security.permission.NgoPermission;
import edu.gemini.util.security.permission.PiPermission;
import edu.gemini.util.security.permission.StaffPermission;
import edu.gemini.util.security.permission.VisitorPermission;
import edu.gemini.util.security.policy.ImplicitPolicyForJava;
import jsky.app.ot.userprefs.general.GeneralPreferences;
import java.security.Permission;
import java.util.logging.Logger;

/**
 * Stores the values of the OT application command line options.
 */
public class OTOptions {

    private static final Logger LOGGER = Logger.getLogger(OTOptions.class.getName());

    /**
     * Return true if the phase 2 checking engine is enabled.
     */
    public static boolean isCheckingEngineEnabled() {
        return GeneralPreferences.fetch().isPhase2Checking();
    }

    /**
     * Return true if editing the current science program or observation is allowed
     */
    // TODO: inline this
    public static boolean isEditable(ISPProgram prog, ISPObservation obsOrNull) {
        return areRootAndCurrentObsIfAnyEditable(prog, obsOrNull);
    }

    public static boolean areRootAndCurrentObsIfAnyEditable(ISPProgram prog, ISPObservation obsOrNull) {
        // SANITY CLAUSE
        if ((obsOrNull != null) && (obsOrNull.getProgram() != prog))
            throw new IllegalArgumentException("obs is from another program");

        return (obsOrNull == null) ? isProgramEditable(prog) : isObservationEditable(obsOrNull);
    }

    public static boolean isObservationEditable(ISPObservation obs) {
        if (obs == null) {
            return false;
        } else {
            // LORD OF DESTRUCTION: DataObjectManager get without set
            final SPObservation spObs = (SPObservation) obs.getDataObject();
            if (ObservationStatus.computeFor(obs) == ObservationStatus.OBSERVED) {
                return false;
            } else {
                final SPProgramID pid = obs.getProgramID();
                return (isPI(pid) && SPObservation.isEditableForPI(obs)) || (isNGO(pid) && spObs.isEditableForNGO()) || isStaff(pid);
            }
        }
    }

    public static boolean isProgramEditable(ISPProgram prog) {
        if (prog != null) {
            final SPProgramID pid = prog.getProgramID();
            return isPI(pid) || isStaff(pid) || isNGO(pid);
        }
        return false;
    }

    public static boolean isTestEnabled() {
        return false;
    }

    // TODO: USER ROLE
    public static UserRole getUserRole(String progId) {
        LOGGER.warning("Returning null for UserRole; this will go away.");
        return null;
    }

    // TODO: USER ROLE
    public static UserRole getUserRole() {
        LOGGER.warning("Returning null for UserRole; this will go away.");
        return null;
    }

    ///
    /// SECURITY CHECKS
    ///
    /// TODO: get rid of these checks and move to fine-grained permissions
    ///

    public static boolean isPI(SPProgramID pid) {
        return hasPermission(new PiPermission(pid));
    }

    public static boolean isNGO(SPProgramID pid) {
        return hasPermission(new NgoPermission(pid));
    }

    public static boolean isStaff(SPProgramID pid) {
        return hasPermission(new StaffPermission(pid)) ||
               hasPermission(new VisitorPermission(pid));
    }

    public static boolean isStaff(ISPProgram prog) {
        final SPProgramID pid = (prog == null) ? null : prog.getProgramID();
        return (pid != null) && isStaff(pid);
    }

    public static boolean isStaffGlobally() {
        return hasPermission(new StaffPermission()) ||
               hasPermission(new VisitorPermission());

    }

    public static boolean hasPermission(Permission p) {
        return ImplicitPolicyForJava.hasPermission(SPDB.get(), OT.getKeyChain(), p);
    }

}
