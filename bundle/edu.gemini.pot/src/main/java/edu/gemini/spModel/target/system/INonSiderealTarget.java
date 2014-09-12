package edu.gemini.spModel.target.system;

import java.util.Date;

/**
 * Public interface for a NonSidereal Target
 */
public interface INonSiderealTarget extends ITarget {

    /**
     * A given position for a non sidereal object is valid only
     * at a certain time. This method will return de <code>Date</code>
     * object that represents the time where the stored Ra and Dec for
     * the given NonSidereal Target are gotten
     * @return Date when the stored position is valid, <code>null</code>
     * if the positions are not being set.
     */
    public Date getDateForPosition();

}
