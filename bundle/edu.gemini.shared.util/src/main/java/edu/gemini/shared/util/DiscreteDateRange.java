// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DiscreteDateRange.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.util;

import java.io.Serializable;
import java.util.Date;
import java.util.Calendar;

/**
 * An (immutable) class for representing a range of dates specified by a
 * start date and an end date.
 */
public class DiscreteDateRange extends DateRange implements DiscreteRange, Serializable, Comparable {

    public static final int DEFAULT_PRECISION = Calendar.DAY_OF_MONTH;

    private int _precision;  // a Calendar constant, e.g. Calendar.DAY_OF_MONTH

    /**
     * Constructs with the given starting and ending dates with
     * default precision of days.
     */
    public DiscreteDateRange(Date startDate, Date endDate) {
        this(CalendarUtil.setPrecision(startDate, DEFAULT_PRECISION), CalendarUtil.setPrecision(endDate, DEFAULT_PRECISION), DEFAULT_PRECISION);
    }

    /**
     * Constructs with the given starting and ending dates and specified precision.
     * @param startDate The start date of the range.
     * @param endDate The end date of the range.
     * @param precision Ranges will be joined if their ends differ by less than
     * precision.  This value is one of the Calendar field constants.
     * e.g. Calendar.DAY_OF_MONTH.
     */
    public DiscreteDateRange(Date startDate, Date endDate, int precision) {
        super(CalendarUtil.setPrecision(startDate, DEFAULT_PRECISION), CalendarUtil.setPrecision(endDate, DEFAULT_PRECISION));
        _setPrecision(precision);
    }

    /**
     * Constructs with the given DateRange and default precision of days.
     * @param dr The date range.
     */
    public DiscreteDateRange(DateRange dr) {
        this(dr, Calendar.DAY_OF_MONTH);
    }

    /**
     * Constructs with the given DateRange and specified precision.
     * @param dr The date range.
     * @param precision Ranges will be joined if their ends differ by less than
     * precision.  This value is one of the Calendar field constants.
     * e.g. Calendar.DAY_OF_MONTH.
     */
    public DiscreteDateRange(DateRange dr, int precision) {
        super(dr);
        _setPrecision(precision);
    }

    /**
     * Override the protected clone() method to make it public.
     */
    public Object clone() {
        return this;
    }

    /**
     * Gets a "normalized" DiscreteDateRange, which is the same as this date range
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
    public DiscreteRange normalize() {
        DiscreteRange dr = this;
        Date start = new Date();
        Date end = new Date();
        getStartDate(start);
        getEndDate(end);
        if (start.after(end)) {
            dr = new DiscreteDateRange(end, start, getPrecision());
        }
        return dr;
    }

    /**
     * Gets the starting date for the range.
     */
    public Object getStart() {
        return getStartDate();
    }

    /**
     * Gets the ending date for the range.
     */
    public Object getEnd() {
        return getEndDate();
    }

    /**
     * Gets the precision.
     */
    public int getPrecision() {
        return _precision;
    }

    /**
     * Sets the precision and truncates Date to have only indicated precision.
     * The method of truncation is to convert start and end Dates to a
     * Calendar and zero the fields of lesser precision.
     */
    private void _setPrecision(int precision) {
        _precision = precision;
    }

    /**
     * Returns true if this DateRange contains the specified object.
     * DateRange contains() supports argument types of Calendar or Date.
     * Returns false if object is not in range or object class is not supported.
     */
    public boolean contains(Object o) {
        if (o instanceof Date)
            return super.contains(o);
        if (!(o instanceof Calendar))
            return false;
        Calendar c = (Calendar) o;
        Date d = c.getTime();
        return super.contains(d);
    }

    /**
     * Returns true if this range contains specified range
     * <pre>
     *             this
     * *--------------------------*
     *      *---------------*
     *             arg
     *
     * </pre>
     */
    public boolean contains(DiscreteRange arg) {
        if (!(arg instanceof DateRange))
            return false;
        DateRange dr = (DateRange) arg;
        return (!dr.getStartDate().before(getStartDate()) && (!dr.getEndDate().after(getEndDate())));
    }

    /**
     * Returns true if this range is contained by specified range
     * <pre>
     *
     *             this
     *      *---------------*
     * *--------------------------*
     *             arg
     *
     * </pre>
     */
    public boolean containedBy(DiscreteRange arg) {
        if (!(arg instanceof DateRange))
            return false;
        DateRange dr = (DateRange) arg;
        return (dr.contains(this));
    }

    /**
     * Returns true if this range's end is after the argument range
     * <pre>
     *
     *      arg             this
     * *-----------*  *---------------*
     *
     * </pre>
     */
    public boolean after(DiscreteRange arg) {
        if (!(arg instanceof DateRange))
            return false;
        return after((DateRange) arg);
    }

    /**
     * Returns true if this range's start is before the argument range
     * <pre>
     *
     *      this            arg
     * *-----------*  *---------------*
     *
     * </pre>
     */
    public boolean before(DiscreteRange arg) {
        if (!(arg instanceof DateRange))
            return false;
        return before((DateRange) arg);
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
    public boolean intersects(DiscreteRange arg) {
        if (!(arg instanceof DateRange))
            return false;
        return super.intersects((DateRange) arg);
    }

    /**
     * If two ranges "touch" they can be combined into one range.
     * Returns true if the union of the ranges is one continuous range.
     * Intersects implies touches, so touches is weaker than intersects.
     * <pre>
     *
     *                           this
     *                    *---------------*
     *          *------------------*
     *                   arg
     *
     *      *------------------**-------------------*
     *               arg                this
     *
     * </pre>
     */
    public boolean touches(DiscreteRange arg) {
        if (!(arg instanceof DateRange))
            return false;
        DateRange dr = (DateRange) arg;
        if (intersects(arg))
            return true;
        // now only hope is that ranges just touch at ends
        // i.e. ends differ by one day.
        Calendar c1, c2;
        if (before(dr)) {
            c1 = CalendarUtil.newGregorianCalendarInstance(getEndDate());
            c1.add(getPrecision(), 1);
            c1.add(Calendar.MILLISECOND, 1); // just to push it over the line.
            return c1.getTime().getTime() >= dr.getStartDate().getTime();
        }
        else {
            c1 = CalendarUtil.newGregorianCalendarInstance(getStartDate());
            c1.add(getPrecision(), -1);
            c1.add(Calendar.MILLISECOND, -1); // just to push it over the line.
            return c1.getTime().getTime() <= dr.getEndDate().getTime();
        }
    }

    /**
     * If this range touches the argument range, this method returns
     * the union of the two ranges.
     * Ranges must "touch" if the union is to be a single range.
     * This method returns null if the union is more than one range,
     * i.e. the ranges do not touch.
     * <pre>
     *
     *           arg             this
     *     *---------------*---------------*
     *
     *              returns
     *
     *     *-------------------------------*
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
     *          *-------------------------*
     *
     * </pre>
     */
    public DiscreteRange union(DiscreteRange arg) {
        if (!touches(arg))
            return null;
        if (!(arg instanceof DateRange))
            return null;
        DateRange dr = (DateRange) arg;
        long start1 = getStartDate().getTime();
        long start2 = dr.getStartDate().getTime();
        long end1 = getEndDate().getTime();
        long end2 = dr.getEndDate().getTime();
        long start = (start1 < start2) ? start1 : start2;
        long end = (end1 > end2) ? end1 : end2;
        return new DiscreteDateRange(new Date(start), new Date(end), getPrecision());
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
    public DiscreteRange intersection(DiscreteRange arg) {
        if (!(arg instanceof DateRange))
            return null;
        DateRange dr = super.intersection((DateRange) arg);
        if (dr == null)
            return null;
        return new DiscreteDateRange(dr, getPrecision());
    }

    /**
     * Returns the difference of the ranges, i.e. the range consisting
     * of the units contained in this range that are NOT contained
     * in the argument range.
     * Returns null if this range is contained by the argument range.
     * <pre>
     *
     *                           this
     *                    *---------------*
     *          *------------------*|     |
     *                   arg        |     |
     *                              *-----*
     *
     * </pre>
     */
    public DiscreteRange difference(DiscreteRange arg) {
        // This concept makes no sense.
        // A difference between two ranges can be multiple ranges.
        // Can't return the result in a DiscreteRange.
        return null;

        /*
        if (!(arg instanceof DateRange)) return null;
        DateRange dr = super.difference((DateRange)arg);
        return new DiscreteDateRange(dr, getPrecision());

        Date start, end;
        if (_endDate.getTime() > dr._endDate.getTime()) {
           end = _endDate;
           if (_startDate.getTime() > dr.

           start = (_startDate.getTime() >= dr._startDate.getTime()) ?
              _startDate : dr._startDate;
        } else if (_startDate.getTime() <= dr._startDate.getTime()) {
           start = _startDate;
           end = (_endDate.getTime() < dr._endDate.getTime()) ?
              _endDate : dr._endDate;
        } else {
           return null;  // this range is contained by arg range
        }
        return new DateRange(start, end);
        */
    }

    /**
     * Overrides to match the definition of equals.
     */
    public int hashCode() {
        int result = getStartDate().hashCode();
        return 37 * result + getEndDate().hashCode();
    }

    /**
     * Overrides to display a human-readable representation of the start and
     * end dates.
     */
    public String toString() {
        return getClass().getName() + " [startDate=" + CalendarUtil.toString(getStartDate()) + ", endDate=" + CalendarUtil.toString(getEndDate()) + ", precision=" + _precision + "]";
    }

}
