package edu.gemini.itc.shared;

import java.io.PrintStream;
import java.io.StringWriter;

/**
 * A formatting class to write pretty numbers and strings.
 * See the main test function for examples of use.
 * Note that the field width property is non-sticky.
 * Setting it affects only the next operation.
 * <p/>
 * This class could not inherit from StringWriter because the
 * StringWriter.write() returns void and I wanted this class
 * to return an instance of itself for chaining.
 * So it contains a StringWriter instead.
 * <p/>
 * This class is used to format printing to a PrintStream.
 */
public class FormatStringWriter {
    private StringWriter _sStringWriter = new StringWriter();

    private String _sBase;       // dec, oct, hex, bin
    private String _sJustify;    // left, right, center
    private char _cFillChar;
    private short _jFieldWidth; // 0 means auto-width
    private short _jPrecision;

    private final short _jDEFAULT_FIELD_WIDTH = 0;  // auto-adjusting
    private final short _jDEFAULT_PRECISION = 6;
    static public final String LEFT = "left";
    static public final String RIGHT = "right";
    static public final String CENTER = "center";
    private String[] _sJustifications = {LEFT, RIGHT, CENTER};
    static public final String DEC = "dec";
    static public final String OCT = "oct";
    static public final String HEX = "hex";
    static public final String BIN = "bin";
    private String[] _sBases = {DEC, OCT, HEX, BIN};

    public FormatStringWriter() {
        setDefaults();
    }

    public FormatStringWriter setDefaults() {
        _sBase = DEC;
        _sJustify = LEFT;
        _cFillChar = ' ';
        _jFieldWidth = _jDEFAULT_FIELD_WIDTH;
        _jPrecision = _jDEFAULT_PRECISION;
        return this;
    }

    public FormatStringWriter setBase(String sBase) {
        for (int i = 0; i < _sBases.length; ++i) {
            if (sBase.equals(_sBases[i])) _sBase = sBase;
        }
        return this;
    }

    public FormatStringWriter setJustification(String sJustify) {
        // Unrecognized adjustment strings are ignored.
        for (int i = 0; i < _sJustifications.length; ++i) {
            if (sJustify.equals(_sJustifications[i])) _sJustify = sJustify;
        }
        return this;
    }

    public FormatStringWriter setWidth(int iWidth) {
        // negative number set width to zero
        _jFieldWidth = (iWidth > 0) ? (short) iWidth : 0;
        return this;
    }

    public FormatStringWriter setPrecision(int iPrecision) {
        _jPrecision = (iPrecision >= 0) ? (short) iPrecision : 6;
        return this;
    }

    public FormatStringWriter write(String sString) {
        // Strings care about the following FormatStringWriter specifiers:
        // _sJustify, _cFillChar, _jFieldWidth
        if (_jFieldWidth == 0) {
            // auto-adjust width
            _sStringWriter.write(sString);
            return this;
        }
        if (_jFieldWidth >= sString.length()) {
            //  String fits in the field.
            //  Deal with adjustment and fill.
            int iExtraChars = _jFieldWidth - sString.length();
            if (_sJustify == "left") {
                _sStringWriter.write(sString);
                _fill(iExtraChars);
            } else if (_sJustify == "right") {
                _fill(iExtraChars);
                _sStringWriter.write(sString);
            } else {
                // Center adjustment.
                int iLeftFillChars = (int) (iExtraChars / 2);
                int iRightFillChars = (iExtraChars % 2 == 0) ?
                        iLeftFillChars : iLeftFillChars + 1;
                _fill(iLeftFillChars);
                _sStringWriter.write(sString);
                _fill(iRightFillChars);
            }
        } else {
            // String is at least as large as the field.
            // Deal with truncating.
            int iFrom, iTo;
            if (_sJustify.equals(CENTER)) {
                //  012345
                // xxxxxxxx
                iFrom = (int) ((sString.length() - _jFieldWidth) / 2);
                iTo = iFrom + _jFieldWidth - 1;
            } else if (_sJustify.equals(RIGHT)) {
                //   012345
                // xxxxxxxx
                iFrom = sString.length() - _jFieldWidth;
                iTo = sString.length() - 1;
            } else {
                // LEFT
                // 012345
                // xxxxxxxx
                iFrom = 0;
                iTo = _jFieldWidth - 1;
            }
            for (int i = iFrom; i < iTo; i++) {
                _sStringWriter.write(sString.charAt(i));
            }
        }

        _nosticky();    // Reset non-sticky parameters.
        return this;
    }

    private void _fill(int iNumberOfChars) {
        // Print iNumberOfChars fill characters.
        for (int i = 0; i < iNumberOfChars; ++i) {
            _sStringWriter.write(_cFillChar);
        }
    }

    private void _nosticky() {
        // Most parameters are sticky, i.e. once set, they retain their
        // value until they are set again.
        // Reset fields that are not sticky to their default values.
        _jFieldWidth = _jDEFAULT_FIELD_WIDTH;
    }

    public FormatStringWriter write(long l) {
        // This routine is ultimately used for all integer types.
        // Branch on base.
        if (_sBase.equals(BIN)) {
            return write(Long.toBinaryString(l).trim());
        } else if (_sBase.equals(OCT)) {
            return write(Long.toOctalString(l).trim());
        } else if (_sBase.equals(HEX)) {
            return write(Long.toHexString(l).trim());
        } else {
            return write(Long.toString(l).trim());
        }
    }

    public FormatStringWriter write(double d) {
        return _writeDoubleString(Double.toString(d).trim());
    }

    private FormatStringWriter _writeDoubleString(String sDouble) {
        if (sDouble.contains("E")) {
            write(sDouble);
            return this;
        }

        // This routine is ultimately used for all floating point types.
        // Converting from Float to Double or vise versa can introduce
        // roundoff error and change the number.  Therefore, both the
        // float and double write() methods call this method with
        // their toString() representation.
        String sWholePart = "";
        String sFractionPart = "";
        String sDecimal = ".";
        int i = 0;
        while (i < sDouble.length() && sDouble.charAt(i) != '.') {
            ++i;
        }
        // i is pointing to the decimal point or the end of the string
        // if there was no decimal point.
        try {
            sWholePart = sDouble.substring(0, i);
        } catch (StringIndexOutOfBoundsException e) {
            write(e.toString());
        }
        if (i < sDouble.length() + 1) {
            try {
                sFractionPart = sDouble.substring(i + 1, sDouble.length());
            } catch (StringIndexOutOfBoundsException e) {
                write(e.toString());
            }
        }
        // Deal with precision.
        if (sFractionPart.length() > _jPrecision) {
            try {
                sFractionPart = sFractionPart.substring(0, _jPrecision);
            } catch (StringIndexOutOfBoundsException e) {
                write(e.toString());
            }
        } else if (sFractionPart.length() < _jPrecision) {
            int length = sFractionPart.length();
            // Fill with zeroes.
            for (i = 0; i < _jPrecision - length; ++i) {
                sFractionPart = sFractionPart + "0";
            }
        }

        // Don't write decimal point if there are no decimal digits.
        if (sFractionPart.length() == 0) sDecimal = "";

        return write(sWholePart + sDecimal + sFractionPart);
    }

    public FormatStringWriter clear() {
        _sStringWriter = null;  // Hint to gc
        _sStringWriter = new StringWriter();
        return this;
    }

    public FormatStringWriter println() {
        return println(System.out);
    }

    public FormatStringWriter println(PrintStream out) {
        out.println(this.toString());
        this.clear();
        return this;
    }

    public FormatStringWriter print(PrintStream out) {
        out.print(this.toString());
        this.clear();
        return this;
    }

    public String toString() {
        _sStringWriter.flush();
        return _sStringWriter.toString();
    }

    public String toString(short i) {
        return toString((long) i);
    }

    public String toString(int i) {
        return toString((long) i);
    }

    public String toString(long i) {
        String s = this.clear().write(i).toString();
        this.clear();
        return s;
    }

    public String toString(float d) {
        return toString((double) d);
    }

    public String toString(double d) {
        String s = this.clear().write(d).toString();
        this.clear();
        return s;
    }

}
