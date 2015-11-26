package jsky.catalog;

/**
 * An interface representing a search condition for values in a given table column,
 * or parameters to a catalog query.
 */
public interface SearchCondition {

    /**
     * Return true if the condition is true for the given value.
     *
     * @param val The value to be checked against the condition.
     * @return true if the value satisfies the condition.
     */
    boolean isTrueFor(Comparable val);


    /**
     * Return true if the condition is true for the given numeric value.
     * If the condition was specified as a String, the return value is false.
     *
     * @param val The value to be checked against the condition.
     * @return true if the value satisfies the condition.
     */
    boolean isTrueFor(double val);

    /** Return the column or parameter name. */
    String getName();

    /** Return the column or parameter id. */
    String getId();

    /** Returns the value or values as a String in the format "value" or "val1,val2 */
    String getValueAsString();

    /** Returns the value or values as a String in the format "value" or "val1<sep>val2, where <sep> is given by the argument */
    String getValueAsString(String sep);

    /**
     * Returns a string representation of this class in the form "name=minVal[,maxVal]"
     */
    String toString();

    /**
     * Returns a string representation of this class in the form "name=minVal[<sep>maxVal]"
     * where <sep> is the value of the sep argument.
     */
    String toString(String sep);
}
