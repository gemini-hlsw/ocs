//
// $
//

package edu.gemini.spModel.guide;

import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;

/**
 * An interface that represents a guider which can validate guide star
 * positions to determine if they are in range. Some guide stars may not be
 * valid in a particular observation context because they may be out of range
 * of the guide probe or on detector guide window, etc.
 */
public interface ValidatableGuideProbe extends GuideProbe, GuideStarValidator {
}