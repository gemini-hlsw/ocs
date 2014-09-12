package edu.gemini.spModel.gems;

import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.guide.GuideProbeGroup;

/**
 * Defines key aspects of the catalog search.
 *
 * See OT-21
 */
public interface GemsGuideProbeGroup extends GuideProbeGroup {
    Angle getRadiusLimits();
}
