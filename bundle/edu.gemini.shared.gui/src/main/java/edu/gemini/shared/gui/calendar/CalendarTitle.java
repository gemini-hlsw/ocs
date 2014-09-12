package edu.gemini.shared.gui.calendar;

import javax.swing.JLabel;

import edu.gemini.shared.gui.ThinBorder;

public class CalendarTitle extends JLabel {

    public CalendarTitle(String text) {
        super(text);
        setHorizontalAlignment(JLabel.CENTER);
        setBorder(new ThinBorder(ThinBorder.RAISED));
    }
}
