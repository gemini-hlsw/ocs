package edu.gemini.spModel.guide

import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.obs.context.ObsContext


trait VignettingGuideProbe {
  def calculateVignetting(ctx: ObsContext, guideStarCoordinates: Coordinates): Double
}
