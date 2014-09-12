// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
//

package edu.gemini.shared.gui.calendar;

import java.util.Calendar;
import java.io.Serializable;
import java.util.Date;

import edu.gemini.shared.util.DateRange;
import edu.gemini.shared.util.DiscreteRange;
import edu.gemini.shared.util.CalendarUtil;

/**
 * This class represents a range of dates using Calendars.
 * Time is measured at a precision of days.
 * The endpoints are included in the range.
 * DiscreteRanges are assumed to be normalized before calling Range operations.
 * This class is meant to be used with calendars constructed with
 * CalendarUtil.newGregorianCalendarInstance() which zeroes out everything
 * but the days, months, years.
 */
public class CalendarRange implements DiscreteRange, Comparable, Serializable {

    private Calendar _startCalendar;

    private Calendar _endCalendar;

    /**
     * Constructs a CalendarRange between specified Calendars.
     * They do not have to be in any order.  But operations on
     * ranges generally expect normalized ranges.
     */
    public CalendarRange(Calendar startCalendar, Calendar endCalendar) {
        // We want our own copy of the Calendars
        _startCalendar = (Calendar) startCalendar.clone();
        _endCalendar = (Calendar) endCalendar.clone();
    }

    /**
     * Constructs a CalendarRange between specified Dates.
     * They do not have to be in any order.  But operations on
     * ranges generally expect normalized ranges.
     */
    public CalendarRange(Date startDate, Date endDate) {
        this(CalendarUtil.newGregorianCalendarInstance(startDate), CalendarUtil.newGregorianCalendarInstance(endDate));
    }

    /**
     * Constructs a CalendarRange equivalent to specified DateRange.
     */
    public CalendarRange(DateRange dr) {
        this(dr.getStartDate(), dr.getEndDate());
    }

    /**
     * Returns the equivalent DateRange to this CalendarRange
     */
    public DateRange getDateRange() {
        return new DateRange(_startCalendar.getTime(), _endCalendar.getTime());
    }

    /**
     * Gets the object at the start of the range.
     */
    public Object getStart() {
        return _startCalendar;
    }

    /**
     * Gets the object at the end of the range.
     */
    public Object getEnd() {
        return _endCalendar;
    }

    /**
     * This method is required for sorting in a collection.
     * DiscreteRanges will be sorted according to their start date.
     */
    public int compareTo(Object o) {
        CalendarRange r = (CalendarRange) o;
        int i = CalendarUtil.compareTo(getStartCalendar(), r.getStartCalendar());
        if (i != 0)
            return i;
        return CalendarUtil.compareTo(getEndCalendar(), r.getEndCalendar());
    }

    public Calendar getStartCalendar() {
        return _startCalendar;
    }

    public Calendar getEndCalendar() {
        return _endCalendar;
    }

    public int getStartYear() {
        return _startCalendar.get(Calendar.YEAR);
    }

    public int getStartMonth() {
        return _startCalendar.get(Calendar.MONTH);
    }

    public int getStartDay() {
        return _startCalendar.get(Calendar.DAY_OF_MONTH);
    }

    public int getEndYear() {
        return _endCalendar.get(Calendar.YEAR);
    }

    public int getEndMonth() {
        return _endCalendar.get(Calendar.MONTH);
    }

    public int getEndDay() {
        return _endCalendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Overrides to return true if and only if the object is a CalendarRange
     * and the start date and end dates match.
     *
     *             this
     * *--------------------------*
     * *--------------------------*
     *             arg
     *
     */
    public boolean equals(Object o) {
        if (!(o instanceof CalendarRange)) {
            return false;
        }
        CalendarRange arg = (CalendarRange) o;
        return (CalendarUtil.equals(getStartCalendar(), arg.getStartCalendar()) && CalendarUtil.equals(getEndCalendar(), arg.getEndCalendar()));
    }

    /**
     * Returns true if this DiscreteRange contains the specified object.
     * CalendarRange contains() supports argument types of Calendar or Date.
     * Returns false if object is not in range or object class is not supported.
     */
    public boolean contains(Object o) {
        if (!(o instanceof Calendar) && !(o instanceof Date))
            return false;
        Calendar c;
        if (o instanceof Calendar) {
            c = (Calendar) o;
        } else {
            Date d = (Date) o;
            c = CalendarUtil.newGregorianCalendarInstance(d);
        }
        return (!CalendarUtil.before(c, getStartCalendar()) && (!CalendarUtil.after(c, getEndCalendar())));
    }

    /**
     * Override the clone() method to allow cloning of CalendarRange objects.
     */
    public Object clone() {
        return new CalendarRange(getStartCalendar(), getEndCalendar());
        /*
        try {
          oClone = (CalendarRange)super.clone();
        } catch (CloneNotSupportedException e) {
          e.printStackTrace();
        }
        // Object.clone() clones all basic-type data members.
        // Now just set data members that are object handles.
        oClone._startCalendar = (Calendar)_startCalendar.clone();
        oClone._endCalendar = (Calendar)_endCalendar.clone();
        return oClone;*/
    }

    /**
     * Returns true if this range contains specified range
     * <pre>
     *
     *             this
     * *--------------------------*
     *      *---------------*
     *             arg
     *
     * </pre>
     */
    public boolean contains(DiscreteRange rangeArg) {
        if (!(rangeArg instanceof CalendarRange))
            return false;
        CalendarRange arg = (CalendarRange) rangeArg;
        return ((!CalendarUtil.after(getStartCalendar(), arg.getStartCalendar())) && (!CalendarUtil.before(getEndCalendar(), arg.getEndCalendar())));
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
    public boolean containedBy(DiscreteRange rangeArg) {
        if (!(rangeArg instanceof CalendarRange))
            return false;
        CalendarRange arg = (CalendarRange) rangeArg;
        return (arg.contains(this));
    }

    /**
     * Returns true if this range's end is after the argument range
     * and the intersection contains 1 or fewer units.
     * <pre>
     *
     *      arg             this
     * *-----------*  *---------------*
     *
     * </pre>
     */
    public boolean after(DiscreteRange rangeArg) {
        if (!(rangeArg instanceof CalendarRange))
            return false;
        CalendarRange arg = (CalendarRange) rangeArg;
        return (CalendarUtil.after(getStartCalendar(), arg.getEndCalendar()));
    }

    /**
     * Returns true if this range's start is before the argument range
     * and the intersection contains 1 or fewer units.
     * <pre>
     *
     *      this            arg
     * *-----------*  *---------------*
     *
     * </pre>
     */
    public boolean before(DiscreteRange rangeArg) {
        if (!(rangeArg instanceof CalendarRange))
            return false;
        CalendarRange arg = (CalendarRange) rangeArg;
        return (CalendarUtil.before(getEndCalendar(), arg.getStartCalendar()));
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
    public boolean intersects(DiscreteRange rangeArg) {
        if (!(rangeArg instanceof CalendarRange))
            return false;
        CalendarRange arg = (CalendarRange) rangeArg;
        return ((!CalendarUtil.before(getEndCalendar(), arg.getStartCalendar())) && (!CalendarUtil.after(getStartCalendar(), arg.getEndCalendar())));
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
    public boolean touches(DiscreteRange rangeArg) {
        if (!(rangeArg instanceof CalendarRange))
            return false;
        CalendarRange arg = (CalendarRange) rangeArg;
        if (intersects(arg))
            return true;
        // now only hope is that ranges just touch at ends
        // i.e. ends differ by one day.
        Calendar c1, c2;
        if (before(arg)) {
            c1 = (Calendar) getEndCalendar().clone();
            c1.add(Calendar.DAY_OF_MONTH, 1);
            c2 = arg.getStartCalendar();
        } else {
            c1 = (Calendar) getStartCalendar().clone();
            c1.add(Calendar.DAY_OF_MONTH, -1);
            c2 = arg.getEndCalendar();
        }
        return CalendarUtil.equals(c1, c2);
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
    public DiscreteRange union(DiscreteRange rangeArg) {
        if (!(rangeArg instanceof CalendarRange))
            return null;
        CalendarRange arg = (CalendarRange) rangeArg;
        if (!touches(arg))
            return null;
        Calendar start1 = getStartCalendar();
        Calendar start2 = arg.getStartCalendar();
        Calendar end1 = getEndCalendar();
        Calendar end2 = arg.getEndCalendar();
        Calendar start = (CalendarUtil.before(start1, start2)) ? start1 : start2;
        Calendar end = (CalendarUtil.after(end1, end2)) ? end1 : end2;
        return new CalendarRange(start, end);
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
    public DiscreteRange intersection(DiscreteRange rangeArg) {
        if (!(rangeArg instanceof CalendarRange))
            return null;
        CalendarRange arg = (CalendarRange) rangeArg;
        if (CalendarUtil.after(_startCalendar, arg._endCalendar) || CalendarUtil.before(_endCalendar, arg._startCalendar))
            return null;
        // There is at least one point in common.
        // Pick out the latest start date and earliest end date.
        Calendar start = (CalendarUtil.after(_startCalendar, arg._startCalendar)) ? _startCalendar : arg._startCalendar;
        Calendar end = (CalendarUtil.before(_endCalendar, arg._endCalendar)) ? _endCalendar : arg._endCalendar;
        return new CalendarRange(start, end);
    }

    /**
     * Returns the difference of the ranges, i.e. the range consisting
     * of the units contained in this range that are NOT contained
     * in the argument range.
     * Returns null if this range is contained by the argument range
     * because the result is two ranges and we can't return two.
     * <pre>
     *
     *                           this
     *                    *---------------*
     *          *------------------*
     *                   arg
     *
     *                   returns
     *
     *                             *-----*
     *
     * </pre>
     */
    public DiscreteRange difference(DiscreteRange rangeArg) {
        if (!(rangeArg instanceof CalendarRange))
            return null;
        CalendarRange arg = (CalendarRange) rangeArg;
        Calendar start, end;
        if (CalendarUtil.after(_endCalendar, arg._endCalendar)) {
            end = _endCalendar;
            start = (CalendarUtil.after(_startCalendar, arg._startCalendar)) ? _startCalendar : arg._startCalendar;
        } else if (CalendarUtil.before(_startCalendar, arg._startCalendar)) {
            start = _startCalendar;
            end = (CalendarUtil.before(_endCalendar, arg._endCalendar)) ? _endCalendar : arg._endCalendar;
        } else {
            return null;  // this range is contained by arg range
        }
        return new CalendarRange(start, end);
    }

    /**
     * Gets a "normalized" DiscreteRange, which is the same as this date range
     * but guaranteed to have a start date that is before or the same as the
     * end date.
     *
     * @return this if the DiscreteRange is already normalized, otherwise a new
     * DiscreteRange with the start and end dates swapped
     */
    public DiscreteRange normalize() {
        DiscreteRange r = this;
        if (CalendarUtil.after(_startCalendar, _endCalendar)) {
            r = new CalendarRange(_endCalendar, _startCalendar);
        }
        return r;
    }

    /**
     * Returns the first point of intersection between two CalendarRanges
     * or null if they do not intersect.
     */
    public Calendar intersectionStart(CalendarRange arg) {
        if (!intersects(arg))
            return null;
        // they do overlap, return the latest "from" calendar
        if (CalendarUtil.before(getStartCalendar(), arg.getStartCalendar())) {
            return arg.getStartCalendar();
        } else {
            return getStartCalendar();
        }
    }

    /**
     * Returns the last point of intersection between two CalendarRanges
     * or null if they do not intersect.
     */
    public Calendar intersectionEnd(CalendarRange arg) {
        if (!intersects(arg))
            return null;
        // they do overlap, return the earliest "to" calendar
        if (CalendarUtil.after(getEndCalendar(), arg.getEndCalendar())) {
            return arg.getEndCalendar();
        } else {
            return getEndCalendar();
        }
    }

    /**
     * Overrides to match the definition of equals.
     */
    public int hashCode() {
        int result = _startCalendar.hashCode();
        return 37 * result + _endCalendar.hashCode();
    }

    /**
     * Overrides to display a human-readable representation of the start and
     * end dates.
     */
    public String toString() {
        return getClass().getName() + "[start=" + CalendarUtil.toString(_startCalendar) + ", end=" + CalendarUtil.toString(_endCalendar);
    }

}
