//
// $
//

package edu.gemini.shared.skyobject.coords;

import java.io.Serializable;

/**
 * An interface that marks an arbitrary coordinate system and provides a
 * mechanism for converting the coordinates to a standard HMS/Deg system at
 * a particular time.
 */
public interface SkyCoordinates extends Serializable {
    /**
     * Gets the coordinates represented as standard RA (HMS) and declination
     * (degrees) coordinates.  Non-sidereal objects may be found at a specific
     * RA and declination only at a particular time, so this method requires
     * passing the desired date.  For other coordinate systems, the coordinates
     * may be considered valid at any time and the time information won't be
     * used.
     *
     * @param date the time at which the coordinates will be valid
     *
     * @return HmsDegCoordinates representation of the coordinate system
     */
    HmsDegCoordinates toHmsDeg(long date);
}
