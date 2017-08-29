package edu.gemini.p2checker.rules.pwfs;

import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.IRule;
import edu.gemini.p2checker.api.ObservationElements;
import edu.gemini.p2checker.api.P2Problems;
import edu.gemini.pot.sp.ISPProgramNode;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;



/**
 * REL-548: PWFS guide star reachability checks
 */
public class PwfsRule implements IRule {
    private static final String PREFIX = "PwfsRule_";
    private static final String ERROR = " guide star falls out of the range of the guide probe field at one or more offset positions.";
    private static final String WARN = " guide star falls inside the recommended radius at one or more offset positions and may cause vignetting.";

    public IP2Problems check(ObservationElements elements)  {
        Option<ObsContext> opt = elements.getObsContext();
        if (opt.isEmpty()) return null;  // nothing to check

        ObsContext ctx = opt.getValue();

        P2Problems problems = new P2Problems();
        for (ISPProgramNode targetNode : elements.getTargetObsComponentNode()) {

            for (PwfsGuideProbe pwfs : PwfsGuideProbe.values()) {

                TargetEnvironment env = ctx.getTargets();
                Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(pwfs);
                if (gtOpt.isEmpty()) continue; // okay, no targets to check

                GuideProbeTargets gt = gtOpt.getValue();
                Option<SPTarget> primaryOpt = gt.getPrimary();
                if (primaryOpt.isEmpty()) continue; // okay, no target to check

                SPTarget primary = primaryOpt.getValue();
                try {
                    pwfs.checkBoundaries(primary, ctx).foreach(bs -> {
                        switch (bs) {
                            case vignetting:
                                problems.addWarning(PREFIX + "WARN", "The " + pwfs.getKey() + WARN, targetNode);
                                break;
                            case inRange:
                                // OK, this is where it should be: between inner and outer limits
                                break;
                            case outOfRange:
                                problems.addError(PREFIX + "ERROR", "The " + pwfs.getKey() + ERROR, targetNode);
                                break;
                        }
                    });
                } catch (InternalError e) {
                    // ignore "radius limits for this instrument are not defined" error here
                }
            }
        }
        return problems;
    }

}
