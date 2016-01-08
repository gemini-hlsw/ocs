package jsky.catalog;

import jsky.util.StringUtil;


/**
 * Represents a search condition for values in a given table column,
 * or parameters to a query or request.
 */
public class ValueSearchCondition extends AbstractSearchCondition {

    /** The minimum value */
    private final Comparable _val;


    /**
     * Create a new ValueSearchCondition for the given column or parameter description.
     */
    public ValueSearchCondition(final FieldDesc fieldDesc, final Comparable<?> val) {
        super(fieldDesc);
        _val = val;
    }

    /** Return the value (actually a Double or String) */
    public Comparable<?> getVal() {
        return _val;
    }

    /**
     * Return true if the condition is true for the given value.
     *
     * @param val The value to be checked against the condition.
     * @return true if the value satisfies the condition.
     */
    @SuppressWarnings("unchecked")
    @Override public boolean isTrueFor(final Comparable val) {
        if (_val instanceof String && val instanceof String) {
            return StringUtil.match((String) _val, (String) val);
        }
        return (_val.compareTo(val) == 0);
    }


    /**
     * Return true if the condition is true for the given numeric value.
     * If the condition was specified as a String, the return value is false.
     *
     * @param val The value to be checked against the condition.
     * @return true if the value satisfies the condition.
     */
    @Override public boolean isTrueFor(final double val) {
        return isTrueFor(new Double(val));
    }


    /** Return the value as a String. */
    @Override public String getValueAsString(final String sep) {
        return _val.toString();
    }

}





