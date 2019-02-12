//
// $Id: TooUpdate.java 311 2006-03-22 21:49:04Z shane $
//

package edu.gemini.spdb.rapidtoo;

import edu.gemini.shared.util.immutable.Option;
import java.io.Serializable;
import java.time.Duration;

/**
 * A description of the rapid TOO update that should be applied.
 */
public interface TooUpdate extends Serializable {

    /**
     * Gets the identity of the program and observation to update.
     */
    TooIdentity getIdentity();

    /**
     * Gets the base position to apply to the cloned template observation.
     */
    TooTarget getBasePosition();

    /**
     * Gets the guide star coordinate, etc. information.
     */
    Option<TooGuideTarget> getGuideStar();

    /**
     * Gets the instrument position angle to apply to the cloned template
     * observation.
     *
     * @return position angle in degrees or <code>null</code> if not specified
     */
    Double getPositionAngle();

    /**
     * Gets the (optional) exposure time to apply.
     */
    Option<Duration> getExposureTime();

    /**
     * Gets the text of the note that will be added to the observation.  For
     * example, it could contain a link to the finding chart to use.
     *
     * @return note text, or <code>null</code> if not specified
     */
    String getNote();

    /**
     * Gets the elevation constraints that should apply to this TOO trigger,
     * if any.
     *
     * @return elevation constraints or <code>null</code> if not specified
     */
    TooElevationConstraint getElevationConstraint();

    /**
     * Gets the explicit timing window that should apply to this TOO trigger,
     * if any.
     *
     * @return timing window or <code>null</code> if not specified
     */
    TooTimingWindow getTimingWindow();

    /**
     * Gets the name of the group to place the newly created observation in.
     * The new observation will be placed in this group, which will be created
     * if it does not exist.  The group name must match exactly the group in
     * the program in order to be found.  If not specified, the new observation
     * will simply become a child of the program node.
     *
     * @return group name, or <code>null</code> if not specified
     */
    String getGroup();

    /**
     * Determines whether the cloned template observation should be marked
     * with a ready status or left as on hold.
     *
     * @return <code>true</code> if the observation should be considered ready
     * to execute after the update (i.e., should generate a TOO alert),
     * <code>false</code> if it should be left on hold
     */
    boolean isReady();
}
