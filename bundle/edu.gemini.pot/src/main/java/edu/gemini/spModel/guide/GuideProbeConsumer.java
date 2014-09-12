//
// $
//

package edu.gemini.spModel.guide;

import java.util.Collection;

/**
 * A consumer of {@link GuideProbe}s, in that the presence of the implementing
 * class in an observation prohibits the existance of the indicated guiders.
 * When a given context contains a provider and a consumer of the same guider,
 * that guider will not appear among the available options. For example,
 * adaptive optics components cannot be used with the PWFS2 probe. An adaptive
 * optics component can implement this interface to indicate that PWFS2 should
 * not be considered available when it is present.
 */
public interface GuideProbeConsumer {

    /**
     * Gets the collection of all the guiders prohibited by the implementing
     * data object.
     */
    Collection<GuideProbe> getConsumedGuideProbes();
}
