//
// $
//

package edu.gemini.spdb.rapidtoo;

import java.io.Serializable;
import java.util.Date;

/**
 * A description of the timing window to be applied.
 */
public interface TooTimingWindow extends Serializable {
    /**
     * Gets the start of the timing window.
     */
    Date getDate();

    /**
     * Gets the duration of the timing window in milliseconds.
     */
    long getDuration();
}
