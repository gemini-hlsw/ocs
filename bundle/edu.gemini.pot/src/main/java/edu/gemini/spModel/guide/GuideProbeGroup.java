//
// $
//

package edu.gemini.spModel.guide;

import java.util.Collection;

/**
 * A collection of {@link GuideProbe}.
 */
public interface GuideProbeGroup {
    /**
     * Gets a short key used to identify the group.
     */
    String getKey();

    /**
     * Gets a display name used to show the group.
     */
    String getDisplayName();

    /**
     * Gets the members of the group.
     */
    Collection<ValidatableGuideProbe> getMembers();
}
