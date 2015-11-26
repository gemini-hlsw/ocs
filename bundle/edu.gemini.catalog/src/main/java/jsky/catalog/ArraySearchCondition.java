package jsky.catalog;

import jsky.util.NameValue;

/**
 * Represents a search condition with an array of possible values.
 * The condition evaluates to true, if any one of the values in the array match.
 */
public class ArraySearchCondition extends AbstractSearchCondition {

    /** The condition will match any of these values */
    private Object[] _values;


    /**
     * Create a new ArraySearchCondition with the given values.
     */
    public ArraySearchCondition(FieldDesc fieldDesc, Object[] values) {
        super(fieldDesc);
        _values = values;
    }


    /** Return the array of values */
    public Object[] getValues() {
        return _values;
    }

    /**
     * Return true if the condition is true for the given value.
     *
     * @param val The value to be checked against the condition.
     * @return true if the value satisfies the condition.
     */
    public boolean isTrueFor(Comparable val) {
        if ((_values == null) || (_values.length == 0)) return true;

        for (int i = 0; i < _values.length; i++) {
            if (val == null) {
                if (_values[i] == null) return true;

            } else if (_values[i] != null) {
                // SW: Evil, but I don't care.  We need to store NameValue
                // pairs, not just strings
                if (_values[i] instanceof NameValue) {
                    if (((NameValue) _values[i]).getValue().equals(val)) return true;
                } else {
                    if (((Comparable) _values[i]).compareTo(val) == 0) return true;
                }
            }
        }
        return false;
    }


    /**
     * Return true if the condition is true for the given numeric value.
     * If the condition was specified as a String, the return value is false.
     *
     * @param val The value to be checked against the condition.
     * @return true if the value satisfies the condition.
     */
    public boolean isTrueFor(double val) {
        return isTrueFor(new Double(val));
    }


    /** Return the values as a String in the format "val1,val2,val3". */
    public String getValueAsString(String sep) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < _values.length; i++) {
            sb.append(_values[i].toString());
            if (i < _values.length - 1)
                sb.append(sep);
        }
        return sb.toString();
    }

    /**
     * Test cases
     */
    public static void main(String[] args) {
        ArraySearchCondition s = new ArraySearchCondition(new FieldDescAdapter("X"), new String[]{"AAA", "BBB", "CCC"});

        if (!s.isTrueFor("BBB"))
            throw new RuntimeException("test failed for BBB: " + s);
        if (s.isTrueFor("CXX"))
            throw new RuntimeException("test failed for CXX: " + s);

        System.out.println("All tests passed");
    }

}





