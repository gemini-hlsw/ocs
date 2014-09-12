/**
 * $Id: StatusFilter.java 6292 2005-06-03 21:46:52Z brighton $
 */

package jsky.app.ot.viewer;

import edu.gemini.pot.sp.ISPGroup;
import edu.gemini.pot.sp.ISPObservation;

/**
 * An interface for filtering out observations and groups, based on the current
 * status filter selection.
 */
public interface StatusFilter {

    boolean isStatusEnabled(ISPGroup group);
    boolean isStatusEnabled(ISPObservation obs);

    /**
     * Enable the display of observations with any status
     */
    void setStatusEnabled();
}
