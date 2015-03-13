package edu.gemini.spModel.guide;

import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.obs.context.ObsContext;

/**
 * A guide probe that can vignette the science area.
 */
public interface VignettingGuideProbe extends GuideProbe {
    /**
     * Calculate the vignetting factor of the guide probe for the given guide star under the given context.
     * The value returned should be a double in the range [0,1] that provides (an approximation of) the proportion
     * of the probe arm that vignettes the science area, i.e. 0 if the probe arm does not vignette at all, and
     * 1 if the entire probe arm vignettes the science area.
     *
     * @param ctx                  the context information
     * @param guideStarCoordinates the coordinates of the guide star
     * @return (an approximation of) the proportion of the probe arm that vignettes the science area, in [0,1]
     */
    public double calculateVignetting(final ObsContext ctx, final Coordinates guideStarCoordinates);
}
