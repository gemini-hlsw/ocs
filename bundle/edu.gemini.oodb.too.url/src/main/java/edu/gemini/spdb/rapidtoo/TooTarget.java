//
// $Id: TooTarget.java 307 2006-03-16 18:36:14Z shane $
//

package edu.gemini.spdb.rapidtoo;

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.core.Magnitude;

import java.io.Serializable;

/**
 * A description of the target to use in a rapid TOO update.
 */
public interface TooTarget extends Serializable {
    /**
     * The name of target, which is also used to name the observation that is
     * cloned and updated.
     *
     * @return the name of the target
     */
    String getName();

    /**
     * Gets the RA of the target in degrees.
     *
     * @return target RA in degrees
     */
    double getRa();

    /**
     * Gets the declination of the target in degrees.
     *
     * @return target declination in degrees
     * <code>null</code>
     */
    double getDec();

    /**
     * Gets the list of magnitudes (possibly empty)
     *
     * @return magnitude list, possibly empty
     */
    ImList<Magnitude> getMagnitudes();

}
