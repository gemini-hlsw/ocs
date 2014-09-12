/*
 * Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: Param.java 7116 2006-06-04 22:13:06Z rnorris $
 */

package edu.gemini.spModel.pio;

import java.util.List;

/**
 * Describes a single named parameter with zero or more values and optional
 * units. This interface corresponds to the param element in SpXML2.dtd.
 * Typically a Param will be multi-valued or single valued.  This interface
 * supports both concepts with convenience methods for the single-valued
 * (most common) case.  Though use of either set of methods is supported and
 * well defined, it may be more clear to use one or the other and not mix the
 * two.
 *
 * <h3>Methods that make sense for a single-value parameter</h3>
 * <ul>
 * <li>{@link #getValue()}</li>
 * <li>{@link #setValue(String)}</li>
 * </ul>
 *
 * <h3>Methods that make sense in a multi-value parameter context</h3>
 * <ul>
 * <li>{@link #clearValues()}</li>
 * <li>{@link #addValue(String)}</li>
 * <li>{@link #getValueCount()}</li>
 * <li>{@link #getValue(int)}</li>
 * <li>{@link #setValue(int, String)}</li>
 * <li>{@link #clearValues()}</li>
 * <li>{@link #getValues()}</li>
 * <li>{@link #setValues(List)}</li>
 * </ul>
 *
 */
public interface Param extends PioNamedNode {

    /**
     * Sets the name of this Param. For Params, the name is not optional so the
     * <code>name</code> parameter of this method may <em>not</em> be
     * <code>null</code>.
     *
     * @param name new name for this Param; may be not be <code>null</code>
     *
     * @throws NullPointerException if <code>name</code> is null
     */
    void setName(String name);

    /**
     * Returns the units of the parameter value(s), or <code>null</code> if
     * not set.
     */
    String getUnits();

    /**
     * Sets the units of the parameter value(s).
     *
     * @param units new setting for the parameter value(s) units (for example,
     * "hours" or "nights"); may be <code>null</code> to clear the setting
     */
    void setUnits(String units);

    /**
     * Returns the first (or only) value of the parameter, if there is one,
     * <code>null</code> otherwise.
     */
    String getValue();

    /**
     * Sets the single value of the parameter.  If there are other
     * values then they are removed.  At the end of this method call, the
     * parameter will have one value (or none if <code>value</code> is
     * <code>null</code>).
     *
     * @param value new first (or only) value for the parameter; if
     * <code>null</code> then all parameters are cleared (see also
     * {@link #clearValues})
     */
    void setValue(String value);

    /**
     * Returns the number of values this parameter has.
     */
    int getValueCount();

    /**
     * Removes all the values in the parameter, leaving it with no values.
     * This is a synonym for {@link #setValue(String)} and
     * {@link #setValues(List)} when a <code>null</code> argument is provided.
     */
    void clearValues();

    /**
     * Adds a new value to the end of the list of values associated with this
     * parameter.
     *
     * @param value new value to append to the list of values; may not be
     * <code>null</code>
     *
     * @throws NullPointerException if <code>value</code> is <code>null</code>
     */
    void addValue(String value);

    /**
     * Returns the <code>index</code>th parameter value.
     *
     * @param index index of the value to return
     *
     * @throws IndexOutOfBoundsException if index is out of range
     * (index < 0 || index >= #getValueCount())
     */
    String getValue(int index);

    /**
     * Sets the <code>index</code>th parameter value.
     *
     * @param index index of the value to return
     * @param value new value for this element of the value list; may not be
     * <code>null</code>
     *
     * @throws IndexOutOfBoundsException if index is out of range
     * (index < 0 || index >= #getValueCount())
     * @throws NullPointerException if <code>value</code> is <code>null</code>
     */
    void setValue(int index, String value);

    /**
     * Returns a list containing all the values for this parameter in order, or
     * <code>Collections.EMPTY_LIST</code> if there are none.
     *
     * @return List of String values of the parameter
     */
    List<String> getValues();

    /**
     * Sets the list of values associated with this parameter, replacing any
     * that already existed.
     *
     * @param values List of String values for this parameter; if
     * <code>null</code> then all values for this parameter are cleared (see
     * also {@link #clearValues}
     *
     * @throws ClassCastException if any element of <code>values</code> is not
     * a <code>java.lang.String</code>
     */
    void setValues(List<String> values);
}
