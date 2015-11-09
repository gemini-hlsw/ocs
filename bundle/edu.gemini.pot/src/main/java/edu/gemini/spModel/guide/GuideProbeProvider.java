package edu.gemini.spModel.guide;

import java.util.Collection;

/**
 * A provider of {@link GuideProbe}s.  Observation components that are
 * associated with particular guiders should implement this interface to
 * publish them.  For example, GMOS has an OIWFS and Altair has an AOWFS.
 * They publish the existence of these guiders via the implementation of
 * this interface.
 */
public interface GuideProbeProvider {

    /**
     * Gets the collection of all the guiders provided by the implementing
     * data object.
     */
    Collection<GuideProbe> getGuideProbes();
}
