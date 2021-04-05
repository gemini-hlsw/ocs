package jsky.util.gui;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.util.regex.Pattern;

/**
 * A text box for entering numerical values.
 *
 * @author	Allan Brighton
 */
public class NumberBoxWidget extends TextBoxWidget {

    // Allow unfinished exponential notation strings like e.g. "1e", "10E-" etc.
    // which could be the starting sequence of numbers like "1e4", "10E-15".
    private static Pattern IncompleteExponentialNumber = Pattern.compile("-?[0-9]*?.?([0-9]+[e|E]-?)?");

    /** If true, allow negative numbers */
    private boolean allowNegative = true;

    /** Default constructor */
    public NumberBoxWidget() { }

    /** Set to true to allow negative numbers (default), false to disallow them. */
    public void setAllowNegative(boolean b) {
        allowNegative = b;
    }

    protected Document createDefaultModel() {
        return new NumericDocument();
    }

    class NumericDocument extends PlainDocument {

        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {

            if (str == null) {
                return;
            }

            final String currentText = getText(0, getLength());
            final String beforeOffset = currentText.substring(0, offs);
            final String afterOffset = currentText.substring(offs, currentText.length());
            final String result = beforeOffset + str + afterOffset;

            // Check if this is a negative number
            if (!allowNegative && result.trim().startsWith("-")) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            // Allow incomplete exponential numbers; conveniently the DecimalFormatter used
            // to parse those (incomplete) strings back into doubles just ignores incomplete
            // exponential parts, e.g. "1.23e" becomes 1.23 .
            if (IncompleteExponentialNumber.matcher(result).matches()) {
                super.insertString(offs, str, a);
                return;
            }

            try {
                Double.parseDouble(result);
                super.insertString(offs, str, a);
            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

}
