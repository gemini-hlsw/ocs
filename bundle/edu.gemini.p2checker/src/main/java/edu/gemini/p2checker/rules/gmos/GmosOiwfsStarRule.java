package edu.gemini.p2checker.rules.gmos;

import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.IRule;
import edu.gemini.p2checker.api.ObservationElements;
import edu.gemini.p2checker.api.P2Problems;
import edu.gemini.pot.sp.ISPProgramNode;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;



/**
 * A rule for checking GMOS OIWFS guide star position.
 */
public final class GmosOiwfsStarRule implements IRule {
    private static final String PREFIX = "GmosOiwfsStarRule_";
    private static final String WARN = "The OIWFS guide star falls close to the edge of the guide probe field and may not be accessible at one or more offset positions.";
    private static final String ERROR = "The OIWFS guide star falls out of the range of the guide probe field one or more offset positions.";

    public IP2Problems check(ObservationElements elements)  {
        Option<ObsContext> opt = elements.getObsContext();
        if (opt.isEmpty()) return null;  // nothing to check

        ObsContext ctx = opt.getValue();

        P2Problems problems = new P2Problems();
        for (ISPProgramNode targetNode : elements.getTargetObsComponentNode()) {

            // Check the OIWFS guide stars.
            if (InstGmosSouth.SP_TYPE.equals(elements.getInstrument().getType()) ||
                    InstGmosNorth.SP_TYPE.equals(elements.getInstrument().getType())) {

                for (GmosOiwfsGuideProbe oiwfs : GmosOiwfsGuideProbe.values()) {

                    TargetEnvironment env = ctx.getTargets();
                    Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(oiwfs);
                    if (gtOpt.isEmpty()) break; // okay, no targets to check

                    GuideProbeTargets gt = gtOpt.getValue();
                    Option<SPTarget> primaryOpt = gt.getPrimary();
                    if (primaryOpt.isEmpty()) break; // okay, no target to check

                    SPTarget primary = primaryOpt.getValue();

                    oiwfs.checkBoundaries(primary, ctx).foreach(bs -> {
                        switch (bs) {
                            case inside:
                                break;
                            case innerBoundary:
                            case outerBoundary:
                                problems.addWarning(PREFIX + "WARN", WARN, targetNode);
                                break;
                            case outside:
                                problems.addError(PREFIX + "ERROR", ERROR, targetNode);
                                break;
                        }
                    });
                }
            }
        }
        return problems;
    }
}
