package edu.gemini.spModel.guide;

import edu.gemini.skycalc.Offset;
import edu.gemini.spModel.obs.context.ObsContext;

/**
 * An interface that represents a guider which can validate offset positions
 * to determine if they are in range of the guider when tracking the associated
 * guide star.
 */
public interface OffsetValidatingGuideProbe extends GuideProbe {

    /**
     * Determines whether the given offset is in range in the given context.
     * Drawing tools, for example, can use this information to indicate the
     * status of the offset appropriately.
     *
     * @param ctx context in which the offset position is validated
     * @param offset position
     *
     * @return <code>true</code> if the offset is considered in range of the
     * guide star; <code>false</code> otherwise
     */
    boolean inRange(ObsContext ctx, Offset offset);
}
