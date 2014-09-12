//
// $Id: DefaultHeaderItem.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple immutable {@link HeaderItem} implementation.
 */
public final class DefaultHeaderItem implements Serializable, HeaderItem {

    public static HeaderItem create(String keyword, String value, String comment) {
        return new DefaultHeaderItem(keyword, value, comment, true);
    }

    public static HeaderItem create(String keyword, int value, String comment) {
        return new DefaultHeaderItem(keyword, String.valueOf(value), comment, false);
    }

    public static HeaderItem create(String keyword, long value, String comment) {
        return new DefaultHeaderItem(keyword, String.valueOf(value), comment, false);
    }

    private static Pattern TRAIL0 = Pattern.compile("(\\d+\\.[1-9]*)0*$");

    public static HeaderItem create(String keyword, double value, String comment) {
        // 18 is the most that could appear after the .
        String val = String.format("%1$20.18f", value).trim();

        // Remove trailing 0s, if any.
        Matcher m = TRAIL0.matcher(val);
        if (m.matches()) {
            val = m.group(1);
        }

        return new DefaultHeaderItem(keyword, val, comment, false);
    }

    public static HeaderItem create(String keyword, boolean value, String comment) {
        String boolStr = value ? "T" : "F";
        return new DefaultHeaderItem(keyword, boolStr, comment, false);
    }

    public static HeaderItem createComment(String keyword, String comment) {
        return new DefaultHeaderItem(keyword, null, comment, false);
    }

    private String _keyword;
    private String _value;
    private String _comment;
    private boolean _isString;

    DefaultHeaderItem(String keyword, String value, String comment, boolean isString) {
        _keyword  = keyword;
        if (_keyword.length() > 8) {
            throw new FitsException("Keyword longer than 8 characters: " +
                    keyword);
        }
        if (value != null) {
            _value = value.trim();
            int len = _value.length();
            if ((len > 70) || (isString && (len > 68))) {
                throw new FitsException("value is too long: " + _value);
            }
        }
        if (comment != null) {
            _comment  = comment.trim();
        }
        _isString = isString;
    }

    public String getKeyword() {
        return _keyword;
    }

    public boolean isStringValue() {
        return _isString;
    }

    public String getValue() {
        return _value;
    }

    public int getIntValue() {
        return Integer.parseInt(_value);
    }

    public double getDoubleValue() {
        return Double.parseDouble(_value);
    }

    public boolean getBooleanValue() {
        return "T".equals(_value);
    }

    public String getComment() {
        if (_comment == null) return null;

        // Assuming fixed format, comment cannot be larger than 47 characters
        // if there is a value.
        int max = 47;
        if (_value == null) {
            // Comment can take up all the remaining space.
            max = 72;  // "COMMENTadfadsfasd" is legal
        } else {
            // If the value needs more space, then the comment will have
            // to be smaller.
            int sub = _value.length() - 20;
            if (sub > 0) max -= sub;
        }
        if (_comment.length() > max) {
            return _comment.substring(0, max);
        }
        return _comment;
    }

    public String toString() {
        return HeaderItemFormat.format(this);
    }

    public boolean equals(Object o) {
        if (!(o instanceof HeaderItem)) return false;

        HeaderItem that = (HeaderItem) o;
        if (!_keyword.equals(that.getKeyword())) return false;
        if (_value == null) {
            if (that.getValue() != null) return false;
        } else {
            if (!_value.equals(that.getValue())) return false;
        }
        String comment = getComment();
        if (comment == null) {
            if (that.getComment() != null) return false;
        } else {
            if (!comment.equals(that.getComment())) return false;
        }
        return (_isString == that.isStringValue());
    }

    public int hashCode() {
        int res = _keyword.hashCode();
        if (_value != null) {
            res = 31*res + _value.hashCode();
        }
        String comment = getComment();
        if (comment != null) {
            res = 31*res + comment.hashCode();
        }
        res = 31*res + (_isString ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode());
        return res;
    }
}
