package edu.gemini.spModel.guide;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;

/**
 * A {@link GuideProbeGroup} that can optimize the mapping of guide stars to guiders
 * in a particular target environment.
 */
public interface OptimizableGuideProbeGroup extends GuideProbeGroup {

    /**
     * Adds the given guide star to (a copy of) the target environment in the
     * given {@link ObsContext context}.  This method selects the appropriate
     * guider for the new guide star, possibly updating the existing
     * association of guiders to guide stars as well.
     *
     * @param guideStar the new guide star to incorporate in the target
     * environment
     *
     * @param isBAGS determines if the guide star was chosen by background
     * AGS (true) or manually selected (false)*
     *
     * @param ctx the context of the observation, which contains information
     * needed to assign a guider to the new guide star
     *
     * @return a newly optimized TargetEnvironment which incorporates the given
     * <code>guideStar</code>
     */
    TargetEnvironment add(SPTarget guideStar, boolean isBAGS, ObsContext ctx);

    /**
     * Optimizes the assignment of guide stars to guiders according to the given
     * {@link edu.gemini.spModel.obs.context.ObsContext context}.  This method
     * returns a new {@link TargetEnvironment} that contains the
     * updated assignment of guide stars to guide probes.
     *
     * @param ctx the context of the observation, which contains information
     * needed to assign the best combination of guiders to guide stars
     *
     * @return if the target environment can be optimized, a newly optimized
     * TargetEnvironment suited for the given {@link ObsContext context}
     * wrapped in a {@link edu.gemini.shared.util.immutable.Some} object;
     * {@link edu.gemini.shared.util.immutable.None} if there are no updates
     * to make to the context
     */
    Option<TargetEnvironment> optimize(ObsContext ctx);
}
