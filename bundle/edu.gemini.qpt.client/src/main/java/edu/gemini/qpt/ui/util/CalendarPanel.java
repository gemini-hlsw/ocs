package edu.gemini.qpt.ui.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.jdesktop.swingx.calendar.DateSelectionModel;
import org.jdesktop.swingx.JXMonthView;

@SuppressWarnings("serial")
public class CalendarPanel extends JPanel {

    private final JXMonthView monthView;

    private long startDate, endDate;

    private static long truncateToMidnight(TimeZone timeZone, long date) {
        final Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE,      0);
        cal.set(Calendar.SECOND,      0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }


    public CalendarPanel(final TimeZone timeZone, long _startDate, long _endDate) {

        this.startDate = truncateToMidnight(timeZone, _startDate);
        this.endDate   = truncateToMidnight(timeZone, _endDate);

        monthView = new JXMonthView() {{
            setTimeZone(timeZone);

            // The crazy borders and insets were arrived at by experimentation.
            // The default geometry is really bad. This is better. Due to the use
            // of negative insets, modifications to this code may have surprising
            // results.

            Border b1 = BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY),
                    BorderFactory.createEmptyBorder(11, 4, 0, 4));

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(0, 0, 1, 1), b1));

            setMonthStringInsets(new Insets(-3, 0, -3, 0));

            setPreferredColumnCount(2);
            setFirstDisplayedDay(new Date(startDate));
            setSelectionInterval(new Date(startDate), new Date(endDate));
            setSelectionMode(DateSelectionModel.SelectionMode.SINGLE_SELECTION);
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    startDate = getFirstSelectionDate().getTime();
                    endDate   = getLastSelectionDate().getTime();
                }
            });
        }};

        setLayout(new BorderLayout());
        add(monthView, BorderLayout.CENTER);

        add(new NavButton("<", -1), BorderLayout.WEST);
        add(new NavButton(">",  1), BorderLayout.EAST);

    }

    public void setTimeZone(TimeZone tz) {
        if (tz == monthView.getTimeZone()) return;

        // We want the start date at midnight on the same day in the new
        // timezone.

        final Calendar cal0 = Calendar.getInstance(monthView.getTimeZone());
        cal0.setTimeInMillis(startDate);
        final int year  = cal0.get(Calendar.YEAR);
        final int month = cal0.get(Calendar.MONTH);
        final int day   = cal0.get(Calendar.DAY_OF_MONTH);

        final Calendar cal1 = Calendar.getInstance(tz);
        cal1.set(year, month, day, 0, 0, 0);
        cal1.set(Calendar.MILLISECOND, 0);
        startDate = cal1.getTimeInMillis();
        endDate   = startDate;
        final Date newStart = new Date(startDate);
        monthView.setTimeZone(tz);
        monthView.setFirstDisplayedDay(newStart);
        monthView.setSelectionInterval(newStart, newStart);
    }

    public long getStartDate() {
        return startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    private class NavButton extends JButton {
        public NavButton(String text, final int delta) {
            super(text);
            setFocusable(false);
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(monthView.getFirstDisplayedDay().getTime());
                    cal.add(Calendar.MONTH, delta);
                    monthView.setFirstDisplayedDay(new Date(cal.getTimeInMillis()));
                }
            });
        }
    }

}
