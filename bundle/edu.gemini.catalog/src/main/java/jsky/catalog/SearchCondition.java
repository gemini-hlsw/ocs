/*
 * ESO Archive
 *
 * $Id: SearchCondition.java 35363 2011-06-05 09:22:23Z abrighton $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/17  Created
 */

package jsky.catalog;


/**
 * An interface representing a search condition for values in a given table column,
 * or parameters to a catalog query.
 */
public abstract interface SearchCondition {

    /**
     * Return true if the condition is true for the given value.
     *
     * @param val The value to be checked against the condition.
     * @return true if the value satisfies the condition.
     */
    public boolean isTrueFor(Comparable val);


    /**
     * Return true if the condition is true for the given numeric value.
     * If the condition was specified as a String, the return value is false.
     *
     * @param val The value to be checked against the condition.
     * @return true if the value satisfies the condition.
     */
    public boolean isTrueFor(double val);

    /** Return the column or parameter description. */
    public FieldDesc getFieldDesc();

    /** Return the column or parameter name. */
    public String getName();

    /** Return the column or parameter id. */
    public String getId();

    /** Returns the value or values as a String in the format "value" or "val1,val2 */
    public String getValueAsString();

    /** Returns the value or values as a String in the format "value" or "val1<sep>val2, where <sep> is given by the argument */
    public String getValueAsString(String sep);

    /**
     * Returns a string representation of this class in the form "name=minVal[,maxVal]"
     */
    public String toString();

    /**
     * Returns a string representation of this class in the form "name=minVal[<sep>maxVal]"
     * where <sep> is the value of the sep argument.
     */
    public String toString(String sep);
}
