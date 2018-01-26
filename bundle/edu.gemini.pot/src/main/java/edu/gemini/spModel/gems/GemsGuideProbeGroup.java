package edu.gemini.spModel.gems;

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.core.SiderealTarget;
import edu.gemini.spModel.guide.GuideProbeGroup;
import edu.gemini.spModel.obs.context.ObsContext;

/**
 * Defines key aspects of the catalog search.
 *
 * See OT-21
 */
public interface GemsGuideProbeGroup extends GuideProbeGroup {
    Angle getRadiusLimits();

    // Filtering that can be done on asterisms prior to Strehl calculations.
    default boolean asterismPreFilter(final ImList<SiderealTarget> targets) {
        return true;
    }

    // Filtering that can be done on asterisms when settings like posAngle are known.
    default boolean asterismFilter(final ObsContext ctx, final ImList<SiderealTarget> targets) {
        return true;
    }
}
