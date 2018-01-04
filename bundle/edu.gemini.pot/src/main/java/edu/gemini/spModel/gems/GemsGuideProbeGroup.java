package edu.gemini.spModel.gems;

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.core.SiderealTarget;
import edu.gemini.spModel.guide.GuideProbeGroup;
import edu.gemini.spModel.target.SPTarget;

/**
 * Defines key aspects of the catalog search.
 *
 * See OT-21
 */
public interface GemsGuideProbeGroup extends GuideProbeGroup {
    Angle getRadiusLimits();

    default boolean asterismPreFilter(final ImList<SiderealTarget> targets) {
        return true;
    }
}
