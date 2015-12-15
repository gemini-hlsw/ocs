
package edu.gemini.shared.gui.calendar;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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
public class YearMonthDayRange implements DiscreteRange, Comparable<YearMonthDayRange>, Serializable {

    private YearMonthDay _start;

    private YearMonthDay _end;

    /**
     * Constructs a YearMonthDayRange between specified Calendars.
     * They do not have to be in any order.  But operations on
     * ranges generally expect normalized ranges.
     */
    public YearMonthDayRange(YearMonthDay startYearMonthDay, YearMonthDay endYearMonthDay) {
        // We want our own copy of the YearMonthDays
        _start = (YearMonthDay) startYearMonthDay.clone();
        _end = (YearMonthDay) endYearMonthDay.clone();
    }

    /**
     * Gets the object at the start of the range.
     */
    @Override
    public Object getStart() {
        return _start;
    }

    /**
     * Gets the object at the end of the range.
     */
    @Override
    public Object getEnd() {
        return _end;
    }

    /**
     * This method is required for sorting in a collection.
     * DiscreteRanges will be sorted according to their start date.
     */
    @Override
    public int compareTo(YearMonthDayRange o) {
        int i = _start.compareTo(o._start);
        if (i != 0)
            return i;
        return _end.compareTo(o._end);
    }

    /**
     * Overrides to return true if and only if the object is a YearMonthDayRange
     * and the start date and end dates match.
     *
     *             this
     * *--------------------------*
     * *--------------------------*
     *             arg
     *
     */
    public boolean equals(Object o) {
        if (!(o instanceof YearMonthDayRange))
            return false;
        YearMonthDayRange arg = (YearMonthDayRange) o;
        return (_start.equals(arg._start) && _end.equals(arg._end));
    }

    /**
     * Override the clone() method to allow cloning of YearMonthDayRange objects.
     */
    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public Object clone() {
        return new YearMonthDayRange(_start, _end);
    }

    /**
     * Returns true if this DiscreteRange contains the specified object.
     * YearMonthDayRange contains() supports argument types of Calendar or Date.
     * Returns false if object is not in range or object class is not supported.
     */
    public boolean contains(Object o) {
        if (!(o instanceof YearMonthDay))
            return false;
        YearMonthDay arg = (YearMonthDay) o;
        return (!(arg.before(_start) || arg.after(_end)));
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
        if (!(rangeArg instanceof YearMonthDayRange))
            return false;
        YearMonthDayRange arg = (YearMonthDayRange) rangeArg;
        return ((!(_start.after(arg._start))) && (!(_end.before(arg._end))));
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
        if (!(rangeArg instanceof YearMonthDayRange))
            return false;
        YearMonthDayRange arg = (YearMonthDayRange) rangeArg;
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
        if (!(rangeArg instanceof YearMonthDayRange))
            return false;
        YearMonthDayRange arg = (YearMonthDayRange) rangeArg;
        return (!arg._end.after(_start));
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
        if (!(rangeArg instanceof YearMonthDayRange))
            return false;
        YearMonthDayRange arg = (YearMonthDayRange) rangeArg;
        return (!arg._start.before(_start));
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
        if (!(rangeArg instanceof YearMonthDayRange))
            return false;
        YearMonthDayRange arg = (YearMonthDayRange) rangeArg;
        return ((!_end.before(arg._start)) && (!_start.after(arg._end)));
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
        if (!(rangeArg instanceof YearMonthDayRange))
            return false;
        YearMonthDayRange arg = (YearMonthDayRange) rangeArg;
        if (intersects(arg))
            return true;
        // now only hope is that ranges just touch at ends
        // i.e. ends differ by one day.
        if (before(arg)) {
            Calendar c1 = CalendarUtil.newGregorianCalendarInstance(_end.year, _end.month, _end.day);
            Calendar c2 = CalendarUtil.newGregorianCalendarInstance(arg._start.year, arg._start.month, arg._start.day);
            c1.add(Calendar.DAY_OF_MONTH, 1);
            c1.add(Calendar.SECOND, 1); // just to make sure
            return (!CalendarUtil.before(c1, c2));
        } else {
            Calendar c1 = CalendarUtil.newGregorianCalendarInstance(_start.year, _start.month, _start.day);
            Calendar c2 = CalendarUtil.newGregorianCalendarInstance(arg._end.year, arg._end.month, arg._end.day);
            c2.add(Calendar.DAY_OF_MONTH, 1);
            c2.add(Calendar.SECOND, 1); // just to make sure
            return (!CalendarUtil.before(c2, c1));
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
    public DiscreteRange union(DiscreteRange rangeArg) {
        if (!(rangeArg instanceof YearMonthDayRange))
            return null;
        YearMonthDayRange arg = (YearMonthDayRange) rangeArg;
        if (!touches(arg))
            return null;
        YearMonthDay start = _start.before(arg._start) ? _start : arg._start;
        YearMonthDay end = _end.after(arg._end) ? _end : arg._end;
        return new YearMonthDayRange(start, end);
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
        if (!(rangeArg instanceof YearMonthDayRange))
            return null;
        YearMonthDayRange arg = (YearMonthDayRange) rangeArg;
        if (_start.after(arg._end) || _end.before(arg._start))
            return null;
        // There is at least one point in common.
        // Pick out the latest start date and earliest end date.
        YearMonthDay start = (_start.after(arg._start)) ? _start : arg._start;
        YearMonthDay end = (_end.before(arg._end)) ? _end : arg._end;
        return new YearMonthDayRange(start, end);
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
        return null;
    }
    /*
    if (!(rangeArg instanceof YearMonthDayRange)) return null;
    YearMonthDayRange arg = (YearMonthDayRange)rangeArg;
    Calendar start, end;
    if (CalendarUtil.after(_end, arg._end)) {
       end = _end;
       start = (CalendarUtil.after(_start, arg._start)) ?
          _start : arg._start;
    } else if (CalendarUtil.before(_start, arg._start)) {
       start = _start;
       end = (CalendarUtil.before(_end, arg._end)) ?
          _end : arg._end;
    } else {
       return null;  // this range is contained by arg range
    }
    return new YearMonthDayRange(start, end);
    }*/

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
        if (_start.after(_end)) {
            r = new YearMonthDayRange(_end, _start);
        }
        return r;
    }

    /**
     * Overrides to match the definition of equals.
     */
    public int hashCode() {
        int result = _start.hashCode();
        return 37 * result + _end.hashCode();
    }

    /**
     * Overrides to display a human-readable representation of the start and
     * end dates.
     */
    public String toString() {
        return getClass().getName() + "[start=" + _start + ", end=" + _end;
    }

}
