package edu.gemini.shared.util;

/**
 * This interface represents a range and interaction between ranges.
 * A Range will include its endpoints.
 * Note that this interface does not specify what a Range is.
 * Examples include integer numbers, dates, anything drawn from an
 * ordered discrete set.
 * Discrete ranges are different from continuous ranges in that two
 * ranges next to each other whose ends differ by a single unit can
 * be joined into one range.  With continuous ranges, there is no
 * distance small enough where the ranges can be joined.
 */
public interface DiscreteRange {

    Object clone();

    /**
     * Gets the object at the start of the range.
     */
    Object getStart();

    /**
     * Gets the object at the end of the range.
     */
    Object getEnd();

    /**
     * Returns true if the argument object is of the right class
     * and is contained in the range.
     * @param o An object of the set enumerated by the DiscreteRange data type.
     * For example, with a DateRange, o would be a Date.
     */
    boolean contains(Object o);

    /**
     * Gets a "normalized" Range, which is the same as this date range
     * but guaranteed to have a start date that is before or the same as the
     * end date.
     *
     * @return this if the Range is already normalized, otherwise a new
     * Range with the start and end dates swapped
     */
    DiscreteRange normalize();

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
    boolean contains(DiscreteRange arg);

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
    boolean after(DiscreteRange arg);

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
    boolean before(DiscreteRange arg);

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
    boolean intersects(DiscreteRange arg);

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
    boolean touches(DiscreteRange arg);

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
    DiscreteRange union(DiscreteRange arg);

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
    DiscreteRange intersection(DiscreteRange arg);

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
    DiscreteRange difference(DiscreteRange arg);

}

