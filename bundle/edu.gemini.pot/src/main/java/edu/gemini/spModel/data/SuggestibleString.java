//
// $Id: SuggestibleString.java 7037 2006-05-12 21:51:28Z shane $
//

package edu.gemini.spModel.data;

import edu.gemini.spModel.type.DisplayableSpType;

import java.io.Serializable;

/**
 * A string with an enumerated type containing non-binding suggestions for the
 * value.
 */
public class SuggestibleString implements Comparable, Serializable {

    private Class<? extends Enum> _enumClass;
    private String _value;

    public SuggestibleString(Class<? extends Enum> enumClass) {
        if (enumClass == null) throw new NullPointerException();
        _enumClass = enumClass;

        Enum e = (enumClass.getEnumConstants())[0];
        if (DisplayableSpType.class.isAssignableFrom(enumClass)) {
            _value = ((DisplayableSpType) e).displayValue();
        } else {
            _value = e.name();
        }
    }

    public Class<? extends Enum> getEnumClass() {
        return _enumClass;
    }

    public Enum[] getEnumConstants() {
        return _enumClass.getEnumConstants();
    }

    public String getStringValue() {
        return _value;
    }

    public void setStringValue(String value) {
        _value = value;
    }

    public String toString() {
        return getStringValue();
    }

    public int compareTo(Object other) {
        SuggestibleString that = (SuggestibleString) other;

        if (_enumClass != that._enumClass) {
            return _enumClass.getName().compareTo(that._enumClass.getName());
        }

        if (_value == null) {
            return that._value == null ? 0 : -1;
        } else if (that._value == null) {
            return 1;
        }
        return _value.compareTo(that._value);
    }

    public boolean equals(Object other) {
        if (!(other instanceof SuggestibleString)) return false;

        SuggestibleString that = (SuggestibleString) other;

        if (_value == null) {
            return that._value == null;
        }
        if (!_value.equals(that._value)) return false;

        return _enumClass.equals(that._enumClass);
    }

    public int hashCode() {
        int res = _enumClass.hashCode();
        if (_value != null) {
            res = 37*res + _value.hashCode();
        }
        return res;
    }
}
