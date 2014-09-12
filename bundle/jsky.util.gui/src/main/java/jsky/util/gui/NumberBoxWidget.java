// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: NumberBoxWidget.java 6719 2005-11-08 19:35:36Z brighton $
//
package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;



/**
 * A text box for entering numerical values.
 *
 * @author	Allan Brighton
 */
public class NumberBoxWidget extends TextBoxWidget {

    /** If true, allow negative numbers */
    boolean _allowNegative = true;

    /** Default constructor */
    public NumberBoxWidget() {
    }


    /** Set to true to allow negative numbers (default), false to disallow them. */
    public void setAllowNegative(boolean b) {
        _allowNegative = b;
    }


    protected Document createDefaultModel() {
        return new NumericDocument();
    }

    class NumericDocument extends PlainDocument {

        public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException {

            if (str == null) {
                return;
            }

            String currentText = getText(0, getLength());
            String beforeOffset = currentText.substring(0, offs);
            String afterOffset = currentText.substring(offs, currentText.length());
            String result = beforeOffset + str + afterOffset;

            if (result.startsWith("-")) {
                if (_allowNegative)
                    super.insertString(offs, str, a);
                else
                    Toolkit.getDefaultToolkit().beep();
                return;
            }

            if (result.equals(".")) {
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


    /**
     * test main
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("NumberBoxWidget");

        NumberBoxWidget tbw = new NumberBoxWidget();
        tbw.setAllowNegative(false);
        tbw.addWatcher(new TextBoxWidgetWatcher() {
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                System.out.println("textBoxKeyPress: " + tbwe.getValue());
            }

            public void textBoxAction(TextBoxWidget tbwe) {
                System.out.println("textBoxAction: " + tbwe.getValue());
            }
        });

        frame.getContentPane().add(tbw, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

