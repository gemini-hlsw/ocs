package edu.gemini.spModel.guide;

/**
 * A interface marking a GuideOption, a state value that can be assigned to
 * a guider.  For example, a guide probe can be set to park, freeze or guide
 * and an on detector guide window can be turned on or off.
 */
public interface GuideOption {
    /**
     * Gets the name of the guide option.
     */
    String name();

    /**
     * Returns <code>true</code> if the guide option is active, or in use.
     * When offsetting, guiders can be parked, for example or on detector
     * guiders can be turned off.
     */
    boolean isActive();
}
