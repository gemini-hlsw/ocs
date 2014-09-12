//
// $
//

package edu.gemini.spdb.rapidtoo;

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;

import java.io.Serializable;

/**
 * A description of the timing window to be applied.
 */
public interface TooElevationConstraint extends Serializable {

    /**
     * Gets the type of the elevation constraint to use.
     */
    SPSiteQuality.ElevationConstraintType getType();

    /**
     * Gets the minimum value of the elevation constraint.
     */
    double getMin();

    /**
     * Gets the maximum value of the constraint.
     */
    double getMax();
}
