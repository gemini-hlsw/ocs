package edu.gemini.spModel.target.system;

import java.util.Date;

/**
 * Public interface for a NonSidereal Target
 */
public abstract class INonSiderealTarget extends ITarget {

    /**
     * Constructs with the system option.
     *
     * @param systemOption
     * @throws IllegalArgumentException if the given <code>systemOption</code>
     *                                            is not permitted
     */
    protected INonSiderealTarget(TypeBase systemOption) throws IllegalArgumentException {
        super(systemOption);
    }

    /**
     * A given position for a non sidereal object is valid only
     * at a certain time. This method will return de <code>Date</code>
     * object that represents the time where the stored Ra and Dec for
     * the given NonSidereal Target are gotten
     * @return Date when the stored position is valid, <code>null</code>
     * if the positions are not being set.
     */
    public abstract Date getDateForPosition();

}
