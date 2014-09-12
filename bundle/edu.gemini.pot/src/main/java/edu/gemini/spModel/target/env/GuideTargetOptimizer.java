//
// $
//

package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.guide.GuideProbeGroup;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.OptimizableGuideProbeGroup;
import edu.gemini.spModel.obs.context.ObsContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A function that attempts to optimize a collection of guide targets.  It
 * works by checking each {@link edu.gemini.spModel.guide.OptimizableGuideProbeGroup}
 * present amongst the guide targets in the environment.  Each is presented
 * the opportunity to optimize the guide target set.  If any changes have been
 * made, they are returned to the caller.
 */
public enum GuideTargetOptimizer {
    instance;

    /**
     * Optimizes the guide targets contained in the given
     * {@link ObsContext}, if possible.
     *
     * @param ctx the context of the observation in which the guide targets
     * find themselves
     *
     * @return {@link Some}<{@link TargetEnvironment}>, the optimized targets
     * if any changes are indicated, {@link None} otherwise
     */
    public Option<TargetEnvironment> optimize(ObsContext ctx) {
        Set<OptimizableGuideProbeGroup> groups = getGroups(ctx.getTargets());
        if (groups.size() == 0) return None.instance();

        TargetEnvironment newEnv = null;
        for (OptimizableGuideProbeGroup ogg : groups) {
            Option<TargetEnvironment> tmp = ogg.optimize(ctx);
            if (!tmp.isEmpty()) {
                newEnv = tmp.getValue();
                ctx = ctx.withTargets(newEnv);
            }
        }

        Option<TargetEnvironment> none = None.instance();
        return (newEnv == null) ? none : new Some<TargetEnvironment>(newEnv);
    }

    // Gets all the OptimizableGuideGroups represented in the target
    // environment.
    private Set<OptimizableGuideProbeGroup> getGroups(TargetEnvironment env) {
        Set<GuideProbe> guiders = env.getOrCreatePrimaryGuideGroup().getReferencedGuiders();
        if (guiders.size() == 0) return Collections.emptySet();

        Set<OptimizableGuideProbeGroup> res = new HashSet<OptimizableGuideProbeGroup>();
        for (GuideProbe guider : guiders) {
            Option<GuideProbeGroup> groupOpt = guider.getGroup();
            if (groupOpt.isEmpty()) continue;

            GuideProbeGroup group = groupOpt.getValue();
            if (group instanceof OptimizableGuideProbeGroup) {
                res.add((OptimizableGuideProbeGroup) group);
            }
        }
        return res;
    }
}
