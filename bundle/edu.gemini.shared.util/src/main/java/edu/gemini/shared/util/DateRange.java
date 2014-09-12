// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DateRange.java 27821 2010-11-05 22:01:32Z ddawson $
//

package edu.gemini.shared.util;

import java.io.Serializable;
import java.util.Date;

/**
 * An (immutable) class for representing a range of dates specified by a
 * start date and an end date.
 */
public class DateRange implements Serializable, Comparable {

    private Date _startDate;

    private Date _endDate;

    /**
     * Constructs with the given starting and ending dates.
     */
    public DateRange(Date startDate, Date endDate) {
        // Want this object to be immutable, so make copies of these dates
        // so the client can't change them.
        _startDate = (Date) startDate.clone();
        _endDate = (Date) endDate.clone();
    }

    /**
     * Constructs with the given starting and ending dates.
     */
    public DateRange(DateRange dr) {
        // Want this object to be immutable, so make copies of these dates
        // so the client can't change them.
        _startDate = (Date) dr._startDate.clone();
        _endDate = (Date) dr._endDate.clone();
    }

    /** 
     *      * Hibernate requires a no args constructor.
     */
    public DateRange() { }   

    /**
     * Override the protected clone() method to make it public.
     */
    public Object clone() {
        return this;  // since this is immutable
    }

    /**
     * This method supports sorting in collections, implements Comparable.
     */
    public int compareTo(Object o) {
        if (!(o instanceof DateRange))
            return 1;
        DateRange dr = (DateRange) o;
        if (getStartDate().getTime() < dr.getStartDate().getTime())
            return -1;
        if (getStartDate().getTime() > dr.getStartDate().getTime())
            return 1;
        if (getEndDate().getTime() < dr.getEndDate().getTime())
            return -1;
        if (getEndDate().getTime() > dr.getEndDate().getTime())
            return 1;
        return 0;
    }

    /**
     * Gets a "normalized" DateRange, which is the same as this date range
     * but guaranteed to have a start date that is before or the same as the
     * end date.
     *
     * @return this if the DateRange is already normalized, otherwise a new
     * DateRange with the start and end dates swapped
     */
    // Programmer's note - this method was originally called normalize(),
    // but that name collided with DiscreteRange::normalize().
    // It's not clean for subclasses to force changes in the parent,
    // but this class had no clients at the time.
    public DateRange normalizeRange() {
        DateRange dr = this;
        if (_startDate.after(_endDate)) {
            dr = new DateRange(_endDate, _startDate);
        }
        return dr;
    }

    /**
     * Gets the starting date for the range.
     * @return A copy of the starting date.
     */
    public Date getStartDate() {
        return (Date) _startDate.clone();
    }

    /**
     * Gets the ending date for the range.
     * @return A copy of the ending date.
     */
    public Date getEndDate() {
        return (Date) _endDate.clone();
    }

    /**
     * Gets the starting date for the range through the argument.
     */
    public Date getStartDate(Date output) {
        output.setTime(_startDate.getTime());
        return output;
    }

    /**
     * Gets the ending date for the range through the argument.
     */
    public Date getEndDate(Date output) {
        output.setTime(_endDate.getTime());
        return output;
    }

    /**
     * Overrides to return true if and only if the object is a DateRange
     * and the start date and end dates match.
     */
    public boolean equals(Object o) {
        if (!(o instanceof DateRange)) {
            return false;
        }
        DateRange dr = (DateRange) o;
        return _startDate.equals(dr.getStartDate()) && _endDate.equals(dr.getEndDate());
    }

    /**
     * Returns true if this DateRange contains the specified object.
     * DateRange contains() supports argument types of Calendar or Date.
     * Returns false if object is not in range or object class is not supported.
     */
    public boolean contains(Object o) {
        if (!(o instanceof Date))
            return false;
        Date d = (Date) o;
        return (!d.before(getStartDate()) && (!d.after(getEndDate())));
    }

    /**
     * Returns true if this range's start is equal or after the argument range end
     * <pre>
     *
     *      arg             this
     * *-----------*  *---------------*
     *
     * </pre>
     */
    public boolean after(DateRange dr) {
        return (_startDate.getTime() >= dr._endDate.getTime());
    }

    /**
     * Returns true if this range's end is equal or before the argument range start
     * <pre>
     *
     *      this            arg
     * *-----------*  *---------------*
     *
     * </pre>
     */
    public boolean before(DateRange dr) {
        return (_endDate.getTime() <= dr._startDate.getTime());
    }

    /**
     * Returns true if this range contains at least one unit that is also
     * contained in the argument range.
     * <pre>
     *
     *                           this
     *                    *---------------*
     *          *------------------*
     *                   arg
     *
     *                   this
     *     *---------------------------------*
     *          *------------------*
     *                   arg
     *
     * </pre>
     */
    public boolean intersects(DateRange dr) {
        return (_endDate.getTime() >= dr._startDate.getTime() && _startDate.getTime() <= dr._endDate.getTime());
    }

    /**
     * Returns the intersection of the ranges, i.e. the range consisting
     * of the units contained in both ranges.
     * Returns null if the ranges do not intersect.
     * <pre>
     *
     *           arg             this
     *     *---------------*---------------*
     *
     *              returns
     *
     *                     *
     *
     *
     *
     *                           this
     *                    *---------------*
     *          *------------------*
     *                   arg
     *
     *              returns
     *
     *                    *--------*
     *
     * </pre>
     */
    public DateRange intersection(DateRange dr) {
        if (!intersects(dr))
            return null;
        // There is at least one point in common.
        // Pick out the latest start date and earliest end date.
        Date start = (_startDate.getTime() > dr._startDate.getTime()) ? _startDate : dr._startDate;
        Date end = (_endDate.getTime() < dr._endDate.getTime()) ? _endDate : dr._endDate;
        return new DateRange(start, end);
    }

    /**
     * Overrides to match the definition of equals.
     */
    public int hashCode() {
        int result = _startDate.hashCode();
        return 37 * result + _endDate.hashCode();
    }

    /**
     * Overrides to display a human-readable representation of the start and
     * end dates.
     */
    public String toString() {
        return getClass().getName() + " [startDate=" + _startDate + ", endDate=" + _endDate + "]";
    }

}
