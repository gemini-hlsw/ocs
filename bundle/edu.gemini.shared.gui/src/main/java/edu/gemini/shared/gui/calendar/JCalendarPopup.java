// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: JCalendarPopup.java 38330 2011-11-02 15:16:29Z nbarriga $
//

package edu.gemini.shared.gui.calendar;

import edu.gemini.shared.util.CalendarUtil;

import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * This class is a popup calendar widget used to select a single date.
 */
public class JCalendarPopup extends JPanel implements ActionListener, CalendarSelectionListener {

    protected DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG);

    protected BasicArrowButton calendarButton;

    protected JCalendar calendar;

    protected JPopupMenu popup;

    protected JTextField field;

    protected Date date;

    private Date _originalDate = null;

    private int _maxWidth = Integer.MAX_VALUE;

    private boolean _resized = false;  // A hack for correct size after resizing

    private CalendarSelectionListener _calendarSelectionListener = null;

    private class MouseWatcher extends MouseAdapter {

        public void mouseClicked(MouseEvent event) {
            if (!popup.isVisible()) {
                // if (!field.hasFocus()) calendarButton.doClick();
            }
        }
    }

    /**
     * constructs a calendar popup with the current date selected.
     */
    public JCalendarPopup() {
        this(new Date());
    }

    /**
     * constructs a calendar popup with the specified date selected.
     */
    public JCalendarPopup(Date date) {
        this(date, TimeZone.getDefault());
    }

    /**
     * Constructs a calendar, given a date and a timezone. This is a hack,
     * because the setTimeZone method doesn't work, because the setDate method doesn't work.
     * todo: Fix this class. This is the second hack needed because the class doesn't work as expected.
     *
     * @param date
     * @param tz
     */
    public JCalendarPopup(Date date, TimeZone tz) {
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, field = new JTextField());
        field.setEditable(false);
        setBorder(field.getBorder());
        field.setBorder(null);
        calendarButton = new BasicArrowButton(BasicArrowButton.SOUTH);
        calendarButton.addActionListener(this);
        add(BorderLayout.EAST, calendarButton);
        calendar = new JCalendar(new CalendarMonth(date, tz), true, true, true, true);
        formatter.setTimeZone(tz); // REL-849
        calendar.setTimeZone(tz); // REL-849
        calendar.getCalendarMonth().setMode(CalendarMonth.POPUP_MODE);
        calendar.addCalendarSelectionListener(this);
        //calendar.addActionListener(this);
        popup = new JPopupMenu();
        popup.add(calendar);
        setField(date);
        addComponentListener(new ResizeListener());
        field.addMouseListener(new MouseWatcher());
        calendar.setDate(date);

    }
    /**
     * Overridden from JComponent to enable/disable the text field and button.
     */
    public void setEnabled(boolean enabled) {
        if (popup.isVisible())
            popup.setVisible(false);
        field.setEnabled(enabled);
        calendarButton.setEnabled(enabled);
    }

    /**
     * Implements CalendarSelectionListener
     */
    public void dateSelected(CalendarSelectionEvent event) {
        if (event.getSource() == calendar) {
            setField(event.getDate(formatter.getTimeZone()));
            popup.setVisible(false);
            popup.setPreferredSize(null);
            // SW: Don't steal the focus!
//            field.requestFocus();
        } else {
            // Should never get here.
            System.out.println("Source: " + event.getSource());
        }
    }

    /**
     * Gets the maximum width of the popup.
     */
    public int getMaximumWidth() {
        return _maxWidth;
    }

    /**
     * Sets the maximum width of the popup.
     */
    public void setMaximumWidth(int maxWidth) {
        _maxWidth = maxWidth;
    }

    /**
     * This gets called when user clicks the arrow button next to text field.
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == calendarButton) {
            field.requestFocus();
            getRootPane().setDefaultButton(null);
            if (!popup.isVisible()) {
                //popup.pack();
                //popup.doLayout();
                _originalDate = calendar.getDate();
                Rectangle bounds = _getPopupBounds(popup, calendarButton);
                popup.setPreferredSize(new Dimension(bounds.width, bounds.height));
                popup.show(calendarButton, bounds.x, bounds.y);
                // Here is a hack.  The popup does not know what its preferred
                // size should be until the JCalendar is sized (not just
                // preferred size, but actual size).  But that does
                // not get sized until you show.  Chicken and egg problem.
                // So this hack shows the popup to size the JCalendar, then
                // shuts the popup and shows a second time.  This time the
                // JCalendar is already sized right.
                // This only happens on resizes.
                if (_resized) {
                    popup.setVisible(false);
                    bounds = _getPopupBounds(popup, calendarButton);
                    popup.setPreferredSize(new Dimension(bounds.width, bounds.height));
                    popup.show(calendarButton, bounds.x, bounds.y);
                    _resized = false;
                }
            } else {
                popup.setVisible(false);
                popup.setPreferredSize(null);
                field.requestFocus();
            }
        }
    }

    // Returns a Rectangle with this meaning:
    // width, height are size the popup should be.
    // x,y are position relative to component referenceComponent.
    // Tries first to be below ref component.  If it can't fit full-sided
    // there, it tries above.
    private Rectangle _getPopupBounds(JComponent c, Component referenceComponent) {
        Window window = SwingUtilities.windowForComponent(referenceComponent);
        Insets wInsets = window.getInsets();
        Dimension wDimension = window.getSize();
        Rectangle wbounds = new Rectangle(wDimension);
        Dimension refDimension = referenceComponent.getSize();
        Rectangle refBounds = new Rectangle(refDimension);
        wbounds = SwingUtilities.convertRectangle(window, wbounds, referenceComponent);
        wbounds.x += wInsets.left;
        wbounds.width -= wInsets.left + wInsets.right;
        wbounds.y += wInsets.top;
        wbounds.height -= wInsets.top + wInsets.bottom;
        c.setPreferredSize(null);  // make sure the last size is cleared
        Dimension d = c.getPreferredSize();
        if (d.width > getMaximumWidth())
            d.width = getMaximumWidth();
        if (d.height > getMaximumWidth())
            d.height = getMaximumWidth();
        // d is the size it wants to be.  See if this fits anywhere.

        Rectangle cbounds = new Rectangle(d);    // in local coords
        int bottomClearance = wbounds.y + wbounds.height - refBounds.height;
        int topClearance = -wbounds.y;
        boolean bottom = true;
        if (bottomClearance >= cbounds.height) {
            bottom = true;
        } else if (topClearance >= cbounds.height) {
            bottom = false;
        } else {
            bottom = (bottomClearance >= topClearance);
        }
        if (bottom) {
            // place popup relative to referenceComponent (below and rt justified)
            cbounds.x = refBounds.width - cbounds.width;
            cbounds.y = refBounds.height;

            // clip to window
            if (cbounds.y + cbounds.height > wbounds.y + wbounds.height) {
                cbounds.height = wbounds.y + wbounds.height - cbounds.y;
            }
            if (cbounds.x < wbounds.x) {
                cbounds.width -= wbounds.x - cbounds.x;
                cbounds.x = wbounds.x;
            }
        } else {
            // place popup relative to referenceComponent (above and rt justified)
            cbounds.x = refBounds.width - cbounds.width;
            cbounds.y = -cbounds.height;

            // clip to window
            if (cbounds.y < wbounds.y) {
                cbounds.height = -wbounds.y;
                cbounds.y = wbounds.y;
            }
            if (cbounds.x < wbounds.x) {
                cbounds.width -= wbounds.x - cbounds.x;
                cbounds.x = wbounds.x;
            }
        }
        return cbounds;
    }

    /** Puts specified date in the text field */
    protected void setField(Date date) {
        this.date = date;
        if (date == null) {
            field.setText("Null date");
        } else {
            field.setText(formatter.format(date));
        }
    }

    /**
     * Gets the TimeZone that will be used to interpret Dates from the calendar.
     * Default is TimeZone.getDefault().
     */
    public TimeZone getTimeZone() {
        return calendar.getTimeZone();
    }

    /**
     * Sets the TimeZone that will be used to interpret Dates from the calendar.
     * Default is TimeZone.getDefault().
     * Note that setting the timezone will not change the date currently
     * selected in the widget.
     */
    public void setTimeZone(TimeZone tz) {
        formatter.setTimeZone(tz);
        calendar.setTimeZone(tz);
        setDate(getDate());
    }

    public void setDate(Date date) {
        calendar.setDate(date);
    }

    public Date getDate() {
        return calendar.getDate();
    }

    public void setCalendar(JCalendar calendar) {
        this.calendar = calendar;
    }

    public JCalendar getCalendar() {
        return calendar;
    }

    public void setDateFormat(DateFormat formatter) {
        this.formatter = formatter;
    }

    /**
     * Add a listener to the calendar that's notified of selection change.
     *
     * @param listener The CalendarSelectionListener to add.
     */
    public void addCalendarSelectionListener(CalendarSelectionListener listener) {
        if (_calendarSelectionListener == null) {
            _calendarSelectionListener = new CalendarSelectionHandler();
            calendar.addCalendarSelectionListener(_calendarSelectionListener);
        }
        listenerList.add(CalendarSelectionListener.class, listener);
    }

    /**
     * Remove a listener from the list that's notified each time a
     * change to the selection occurs.
     *
     * @param listener The CalendarSelectionListener to remove.
     * @see #addCalendarSelectionListener
     */
    public void removeCalendarSelectionListener(CalendarSelectionListener listener) {
        listenerList.remove(CalendarSelectionListener.class, listener);
    }

    /**
     * A CalendarSelectionListener that forwards CalendarSelectionEvents from
     * the JCalendar to the popup's CalendarSelectionListeners.
     * The forwarded events only differ from the originals in that their
     * source is the calendar instead of the selectionModel itself.
     */
    private class CalendarSelectionHandler implements CalendarSelectionListener, Serializable {

        public void dateSelected(CalendarSelectionEvent e) {
            fireDateSelected(e.getYear(), e.getMonth(), e.getDay());
        }
    }

    /**
     * This method notifies JCalendarPopup CalendarSelectionListeners that
     * the selection has changed.
     */
    protected void fireDateSelected(int year, int month, int day) {
        Object[] listeners = listenerList.getListenerList();
        CalendarSelectionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CalendarSelectionListener.class) {
                if (e == null) {
                    e = new CalendarSelectionEvent(this, year, month, day);
                }
                ((CalendarSelectionListener) listeners[i + 1]).dateSelected(e);
            }
        }
    }

    private class ResizeListener extends ComponentAdapter {

        public void componentResized(ComponentEvent e) {
            Dimension d = getSize();
            int width = d.width;
            Insets insets;
            insets = getInsets();
            width -= (insets.left + insets.right + 4);
            //calendar.getCalendarMonth().setPreferredSize
            calendar.setPreferredSize(new Dimension(width, width));
            popup.setPreferredSize(null);
            _resized = true;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setTitle("JCalendarPopup Test Harness");
        frame.setBounds(50, 100, 165, 165);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });

        // Create a one-month calendar popup with navigation buttons.
        // Here is how a client uses this thing - listen to it with
        // a CalendarSelectionListener.
        JCalendarPopup calendar = new JCalendarPopup();
        calendar.setMaximumWidth(400);
        calendar.addCalendarSelectionListener(new CalendarSelectionListener() {
            public void dateSelected(CalendarSelectionEvent e) {
                System.out.println("Test result - Selected date: " + CalendarUtil.toString(e.getDate(TimeZone.getDefault())));
            }
        });
        JPanel pan = new JPanel(new BorderLayout());
        pan.add(calendar, BorderLayout.NORTH);
        frame.getContentPane().add("Center", pan);

        //frame.pack();
        frame.setVisible(true);
    }
}
