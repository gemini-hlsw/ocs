package edu.gemini.spModel.guide;

import edu.gemini.spModel.obs.context.ObsContext;

/**
 * A guide probe that can vignette the science area.
 */
public interface VignettingGuideProbe extends GuideProbe {

    /**
     * Constructs a VignettingCalculator for this GuideProbe in the given
     * context.
     */
    VignettingCalculator calculator(ObsContext ctx);
}
