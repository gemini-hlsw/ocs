package edu.gemini.spModel.guide;

import edu.gemini.skycalc.Coordinates;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.obs.context.ObsContext;

/**
 * A {@link GuideProbeGroup} that provides the capability to select the best guider
 * for the given guide star {@link Coordinates}.
 */
public interface SelectableGuideProbeGroup extends GuideProbeGroup {

    /**
     * Selects a guider to use with the given guide star
     * {@link Coordinates coordinates} if any are appropriate.  This is
     * similar to
     * {@link OptimizableGuideProbeGroup#add(edu.gemini.spModel.target.SPTarget, edu.gemini.spModel.obs.context.ObsContext)}
     * but does not optimize the entire collection of guide stars in the
     * target environment.
     *
     * @param guideStar the coordinates of the guide star
     *
     * @param ctx the context of the observation, which contains information
     * needed to select the guide star
     *
     * @return {@link Some}<{@link GuideProbe}> if a guider could be found for
     * this <code>guideStar</code>; {@link None} otherwise
     */
    Option<GuideProbe> select(Coordinates guideStar, ObsContext ctx);
}
