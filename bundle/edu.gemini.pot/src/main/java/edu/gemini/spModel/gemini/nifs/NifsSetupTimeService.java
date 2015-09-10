//
// $
//

package edu.gemini.spModel.gemini.nifs;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;


import java.util.logging.Logger;


// OT-511: NIFS Setup times:
// IF NIFS is used +Altair + OIWFS, setup time = 20 minutes
// IF NIFS is used + Occulting disk, setup time = 25 minutes (regardless of guide configuration)
// IF NIFS is used with just Altair or PWFS2 (no OIWFS, no occulting disk), setup time = 15 minutes

//SCT-219/OT-637: Add additional overhead for acquisitions with LGS
//Art Note: As stated above, If using occulting disk, the setup time
// is 25 minutes regardless of guide config. However, since the implementation is checking
// for the Altair component and its configuration first and also for the use of OIWFS,
// then we won't get 25 minutes every time an occulting disk is in use,
// only if NO Altair or NO OIWFS are in use....
// I'm not changing that behavior now, though sounds like a bug. Will ask for clarifications

// SCT-275: rework setup time calculation according to instructions

// Latest updates in Project Insight SCI-0107.  Decided to move all this crap
// to a separate functor since many remote method calls are otherwise required.


/**
 * A utility class used to calculate NifsSetupTime.
 */
public class NifsSetupTimeService {
    private static final Logger LOG = Logger.getLogger(NifsSetupTimeService.class.getName());

    /** Setup time when not using Altair + LGS. */
    public static final double BASE_SETUP_TIME_SEC     = 11 * 60;
    /** Setup time when using Altair + LGS. */
    public static final double BASE_LGS_SETUP_TIME_SEC = 25 * 60;
    /** Additional setup time to account for the OIWFS. */
    public static final double OIWFS_SETUP_SEC         =  5 * 60;
    /** Additional setup time to account for coronograpic mode. */
    public static final double CORONOGRAPHY_SETUP_SEC  =  4 * 60;

    // Helper used to gather the observation components involved in the
    // calculation.
    private static class Context {
        InstNIFS nifs;
        TargetObsComp target;
        InstAltair altair;

        Context(ISPObservation obs)  {
            for (ISPObsComponent obsComp : obs.getObsComponents()) {
                SPComponentType type = obsComp.getType();
                if (TargetObsComp.SP_TYPE.equals(type)) {
                    target = (TargetObsComp) obsComp.getDataObject();
                } else if (InstNIFS.SP_TYPE.equals(type)) {
                    nifs = (InstNIFS) obsComp.getDataObject();
                } else if (InstAltair.SP_TYPE.equals(type)) {
                    altair = (InstAltair) obsComp.getDataObject();
                }
            }
        }
    }


    public static double getSetupTimeSec(ISPObservation nifsObservation)  {
        Context ctx = new Context(nifsObservation);

        if (ctx.nifs == null) {
            LOG.warning("Tried to calculate NIFS setup time for a non-NIFS observation");
            return 0; // not NIFS!
        }

        // 11 minutes in general, 25 if using Altair with laser guide stars.
        double setupSeconds = BASE_SETUP_TIME_SEC;
        if ((ctx.altair != null) && (ctx.altair.getGuideStarType() == AltairParams.GuideStarType.LGS)) {
            setupSeconds = BASE_LGS_SETUP_TIME_SEC;
        }

        // Add 5 minutes if using the OIWFS
        if (ctx.target != null) {
            final TargetEnvironment env = ctx.target.getTargetEnvironment();
            final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(NifsOiwfsGuideProbe.instance);
            if (gtOpt.exists(GuideProbeTargets::containsTargets)) {
                setupSeconds += OIWFS_SETUP_SEC;
            }
        }

        // Add 4 minutes if using an occulting disk. (Coronography)
        NIFSParams.Mask mask = ctx.nifs.getMask();
        if ((mask != null) && mask.isOccultingDisk()) {
            setupSeconds += CORONOGRAPHY_SETUP_SEC;
        }
        return setupSeconds;
    }
}
