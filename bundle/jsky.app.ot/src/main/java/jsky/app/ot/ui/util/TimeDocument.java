package jsky.app.ot.ui.util;

import javax.swing.text.JTextComponent;
import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
import javax.swing.text.PlainDocument;
import javax.swing.*;
import java.awt.*;

/**
 * just a super simple document to allow entering dates in a text field.
 * This needs to be made more simple...
 * 
 */
public class TimeDocument extends PlainDocument {
    public static final String INIT_STRING = "00:00:00";

     private static int sep1 = 2, sep2 = 5;

     private JTextComponent textComponent;

    private static final int SECONDS_IN_DAY = 86400;


     public void setTime(String time) {
         try {
             super.remove(0, super.getLength());
             super.insertString(0, time, null);
         } catch (BadLocationException e) {
             e.printStackTrace();  
         }
     }

     public TimeDocument(JTextComponent tc) {
         textComponent = tc;
     }

     public void insertString(int offset, String s, AttributeSet attributeSet) throws BadLocationException {
         if (s.equals(INIT_STRING)) {
             super.insertString(offset, s, attributeSet);
             return;
         }
         try {
             Integer.parseInt(s);
         } catch (Exception ex) {
             return;  // only allow integer values
         }
         int newOffset = offset;
         if (atSeparator(offset)) {
             ++newOffset;
         }
         if (newOffset >= INIT_STRING.length()) {
             return;
         }
         if (!_checkTime(newOffset, s.charAt(0))) {
             return;
         }
         if (newOffset != offset) {
             textComponent.setCaretPosition(newOffset);
         }
         super.remove(newOffset, 1);
         super.insertString(newOffset, s, attributeSet);
     }


     // Figure out whether replacing the character (digit) at the given offset
     // in the text would create a valid time.
     private boolean _checkTime(int offset, char newDigit) {
         StringBuffer timeStrBuff = new StringBuffer(textComponent.getText());
         timeStrBuff.setCharAt(offset, newDigit);
         String tmp = timeStrBuff.toString();
         int i;

         // Check hours
         i = _parseInt(tmp, 0, sep1);
         if ((i < 0) || (i > 23)) {
             return false;
         }

         // Check minutes
         i = _parseInt(tmp, sep1 + 1, sep2);
         if ((i < 0) || (i > 59)) {
             return false;
         }

         // Check seconds
         i = _parseInt(tmp, sep2 + 1, -1);
         return !((i < 0) || (i > 59));
     }

     public void remove(int offset, int length) throws BadLocationException {
         if (atSeparator(offset)) {
             textComponent.setCaretPosition(offset - 1);
         } else {
             textComponent.setCaretPosition(offset);
         }
     }

     private boolean atSeparator(int offset) {
         return offset == sep1 || offset == sep2;
     }

     /**
      * Returns the total number of seconds represented by the time shown in
      * the text component.  This differs from {@link #getSecondsField} since this
      * method converts the entire string into seconds, not just the last two
      * digits.
      */
     public int getTotalSeconds() {
         return 60 * (getHoursField() * 60 + getMinutesField()) + getSecondsField();
     }

     /**
      * Sets the time shown in the text component to represent the specified
      * total number of seconds.
      */
     public void setTotalSeconds(int seconds) throws IllegalArgumentException {
         if (seconds >= SECONDS_IN_DAY)
             throw new IllegalArgumentException(seconds + " seconds too much for 24-hour time field");
         int hours = seconds / 3600;
         seconds -= hours * 3600;
         int minutes = seconds / 60;
         seconds -= minutes * 60;
         // build the string, represent each number as two digits
         StringBuffer sb = new StringBuffer((hours < 10) ? ("0" + hours) : "" + hours);
         sb.append(":").append((minutes < 10) ? ("0" + minutes) : "" + minutes);
         sb.append(":").append((seconds < 10) ? ("0" + seconds) : "" + seconds);
         //System.out.println("About to set this time string: " + (new String(sb)));
         try {
             super.remove(0, getLength());
         } catch (Exception ex) {
             ex.printStackTrace();
         }
         try {
             super.insertString(0, new String(sb), null);
         } catch (Exception ex) {
             ex.printStackTrace();
         }
     }

     //
     // Parses the indicated substring into an integer (sub2 can be -1 to specify
     // the end of the string).
     //
     private int _parseInt(String txt, int sub1, int sub2) {
         try {
             if (sub2 == -1) {
                 return Integer.parseInt(txt.substring(sub1));
             } else {
                 return Integer.parseInt(txt.substring(sub1, sub2));
             }
         } catch (Exception ex) {
             //die silently
         }
         return 0;
     }

     /**
      * Get the number of hours specified in the hours field (the first pair of
      * digits) of the text.
      */
     public int getHoursField() {
         return _parseInt(textComponent.getText(), 0, sep1);
     }

     /**
      * Get the number of minutes specified in the minutes field (the second
      * pair of digits) of the text.
      */
     public int getMinutesField() {
         return _parseInt(textComponent.getText(), sep1 + 1, sep2);
     }

     /**
      * Get the number of seconds specified in the seconds field (the last
      * two digits) of the text.
      */
     public int getSecondsField() {
         return _parseInt(textComponent.getText(), sep2 + 1, -1);
     }

     public static void main(String[] args) {
         JFrame f = new JFrame();
         Container contentPane = f.getContentPane();
         JLabel label = new JLabel("Time: ");
         Font font = new Font("Dialog", Font.PLAIN, 24);
         JTextField tf = new JTextField(INIT_STRING);
         label.setFont(font);
         tf.setFont(font);
         tf.setDocument(new TimeDocument(tf));
         contentPane.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
         contentPane.add(label);
         contentPane.add(tf);
         f.setSize(200, 150);
         f.setVisible(true);
     }
    
}
