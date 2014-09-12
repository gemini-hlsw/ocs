// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: StringParameter.java 7020 2006-05-10 15:06:05Z gillies $
//

package edu.gemini.spModel.data.config;

/**
 * A simple implementation of the <code>IParameter</code>
 * interface where the value is a single <code>{@link String}</code>.
 *
 * <p><b>Note that this implementation is not synchronized.</b>
 */
public class StringParameter implements IParameter {
    private String _name;
    private String _value;

    /**
     * The default value.
     */
    public static final String DEFAULT_VALUE = "";

    /**
     * The Factory method for creating a StringParameter from a name only.
     */
    static public StringParameter getInstance(String name) {
        if (name == null) {
            throw new IllegalArgumentException("parameter 'name' may not be null");
        }

        return new StringParameter(name, DEFAULT_VALUE);
    }

    /**
     * The Factory method for creating a StringParameter from a name and value.
     */
    static public StringParameter getInstance(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("parameter 'name' and 'value' may not be null");
        }

        return new StringParameter(name, value);
    }

    /**
     * Convenience method to create a new Parameter and store in the given
     * <code>{@link ISysConfig}</code>.
     */
    static public StringParameter getInstance(ISysConfig sc, String name, String value) {
        if (sc == null) {
            throw new IllegalArgumentException("parameter 'config' may not be null");
        }
        StringParameter sp = getInstance(name, value);
        if (sp == null) return null;

        sc.putParameter(sp);
        return sp;
    }

    // The private constructor
    private StringParameter(String name, String value) {
        _name = name;
        _value = value;
    }

    /**
     * Performs a clone of the parameter.
     * The parameter value is also cloned if it is of type ArrayList.
     */
    public Object clone() {
        StringParameter res;
        try {
            res = (StringParameter) super.clone();
        } catch (CloneNotSupportedException ex) {
            // can't happen since this object implements Cloneable
            throw new RuntimeException();
        }

        return res;
    }

    /**
     * Overrides to provide semantic equality.
     */
    public boolean equals(Object obj) {
        // Is this the same object?
        if (this == obj) return true;

        // Is the object null?
        if (obj == null) return false;

        // Does the given object have the same class as this?
        if (obj.getClass() != getClass()) return false;

        StringParameter other = (StringParameter) obj;

        // Check the names.
        if (!_name.equals(other._name)) return false;

        // Check the values (either may be null).
        if (_value == null) {
            if (other._value != null) return false;
        } else {
            if (!_value.equals(other._value)) return false;
        }

        return true;
    }

    /**
     * Overrides to agree with the redefinition of <code>equals</code>.
     */
    public int hashCode() {
        int result = _name.hashCode();
        return result * 37 + ((_value == null) ? 0 : _value.hashCode());
    }

    public String getName() {
        return _name;
    }

    public void setValue(String newValue) {
        _value = newValue;
    }

    /**
     * Required for the interface.
     */
    public void setValue(Object obj) {
        if (!(obj instanceof String)) throw new IllegalArgumentException("Value must be a String");

        setValue((String) obj);
    }

    /**
     * Required for the interface.
     */
    public Object getValue() {
        return _value;
    }

    public String toString() {
        return getClass().getName() + " [name=" + _name + ", value=" + _value + "]";
    }

    public String getAsString() {
        return _name + " = " + _value;
    }


}
