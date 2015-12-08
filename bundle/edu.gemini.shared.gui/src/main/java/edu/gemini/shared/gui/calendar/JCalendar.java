package edu.gemini.shared.gui.calendar;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.*;
import java.io.Serializable;

import edu.gemini.shared.gui.ThinBorder;
import edu.gemini.shared.util.CalendarUtil;

/**
 * This component holds a CalendarMonth, a CalendarHeader, CalendarTitle,
 * and optional navigation buttons.  It can stand on its own as a
 * component or can be used in a CalendarGroup.
 */
public class JCalendar extends JPanel implements ActionListener, CalendarDisplayListener {

    // When narrower than this, show months as 3-letter abbrevs.
    private static final int WIDTH_LIMIT = 150;

    protected BasicArrowButton westArrow, eastArrow;

    protected DoubleArrowButton westDoubleArrow, eastDoubleArrow;

    protected CalendarTitle title;

    protected CalendarMonth month;

    protected CalendarGroup group;

    // This JCalendar may be governed by an outside CalendarModel.
    // Or if none is given, it constructs and uses its own model
    // that covers only the month displayed.
    private boolean _localCalendarModel;

    private ActionListener _actionListener = null;

    private ListSelectionListener _selectionListener = null;

    private CalendarSelectionListener _calendarSelectionListener = null;

    private TimeZone _tz = TimeZone.getDefault();

    public JCalendar(CalendarMonth month, boolean prev, boolean next, boolean pageBack, boolean pageForward) {
        _initialize(prev, next, pageBack, pageForward, month, new DefaultCalendarHeaderRenderer(), new CalendarGroup());
    }

    private void _initialize(boolean prev, boolean next, boolean pageBack, boolean pageForward, CalendarMonth month, CalendarCellRenderer headRenderer, CalendarGroup group) {
        this.month = month;
        month.addCalendarDisplayListener(this);
        this.group = group;
        this.group.add(month);
        setLayout(new BorderLayout());
        setBorder(new ThinBorder(ThinBorder.LOWERED));
        JPanel nav = new JPanel(new BorderLayout());
        nav.add(title = new CalendarTitle(formatLabel(month)), BorderLayout.CENTER);
        JPanel westPanel = new JPanel(new BorderLayout());
        JPanel eastPanel = new JPanel(new BorderLayout());
        if (prev) {
            if (pageBack) {
                westPanel.add(westArrow = new ArrowButton(BasicArrowButton.WEST), BorderLayout.WEST);
            } else {
                westPanel.add(westArrow = new ArrowButton(BasicArrowButton.WEST), BorderLayout.CENTER);
            }
            westArrow.addActionListener(e -> JCalendar.this.group.prevMonth());
        }
        if (pageBack) {
            if (prev) {
                westPanel.add(westDoubleArrow = new DoubleArrowButton(DoubleArrowButton.WEST), BorderLayout.EAST);
            } else {
                westPanel.add(westDoubleArrow = new DoubleArrowButton(DoubleArrowButton.WEST), BorderLayout.CENTER);
            }
            westDoubleArrow.addActionListener(e -> JCalendar.this.group.pageBackward());
        }
        nav.add(westPanel, BorderLayout.WEST);
        if (pageForward) {
            if (next) {
                eastPanel.add(eastDoubleArrow = new DoubleArrowButton(BasicArrowButton.EAST), BorderLayout.WEST);
            } else {
                eastPanel.add(eastDoubleArrow = new DoubleArrowButton(BasicArrowButton.EAST), BorderLayout.CENTER);
            }
            eastDoubleArrow.addActionListener(e -> JCalendar.this.group.pageForward());
        }
        if (next) {
            if (pageForward) {
                eastPanel.add(eastArrow = new ArrowButton(BasicArrowButton.EAST), BorderLayout.EAST);
            } else {
                eastPanel.add(eastArrow = new ArrowButton(BasicArrowButton.EAST), BorderLayout.CENTER);
            }
            eastArrow.addActionListener(e -> JCalendar.this.group.nextMonth());
        }
        nav.add(eastPanel, BorderLayout.EAST);
        JPanel header = new JPanel();
        header.setLayout(new BorderLayout());
        header.add(BorderLayout.NORTH, nav);
        //header.setBorder(BorderFactory.createRaisedBevelBorder());
        CalendarHeader calHeader = new CalendarHeader(headRenderer);
        header.add(BorderLayout.SOUTH, calHeader);
        add(header, BorderLayout.NORTH);
        add(month, BorderLayout.CENTER);
        month.addActionListener(this);
    }

    private String formatLabel(CalendarMonth month) {
        String s;
        if (month.getDisplayMode() == CalendarMonth.MULTI_MONTH_MODE) {
            s = CalendarUtil.getShortMonthName(month.getFrom().month) + " " + month.getFrom().year + " / ";
            s += CalendarUtil.getShortMonthName(month.getTo().month) + " " + month.getTo().year;
        } else if (getSize().width < WIDTH_LIMIT) {
            s = CalendarUtil.getShortMonthName(month.getFrom().month) + " " + month.getFrom().year;
        } else {
            s = CalendarUtil.getMonthName(month.getFrom().month) + " " + month.getFrom().year;
        }
        return s;
    }

    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == month) {
            title.setText(formatLabel(month));
            title.repaint();
        }
    }

    /**
     * Gets the TimeZone that will be used to interpret Dates from the calendar.
     * Default is TimeZone.getDefault().
     */
    public TimeZone getTimeZone() {
        return _tz;
    }

    /**
     * Sets the TimeZone that will be used to interpret Dates from the calendar.
     * Default is TimeZone.getDefault().
     */
    public void setTimeZone(TimeZone tz) {
        _tz = tz;
        month.setTimeZone(_tz);
    }

    /**
     * Gives client access to the CalendarMonth so it can be configured.
     */
    public CalendarMonth getCalendarMonth() {
        return month;
    }

    /**
     * Implements the CalendarDisplayListener interface.
     */
    public void displayChanged(CalendarDisplayEvent e) {
        CalendarMonth m = (CalendarMonth) e.getSource();
        title.setText(formatLabel(m));
    }

    /**
     * Add a listener to the calendar that's notified each time an
     * actionn is performed.  Listeners added directly to the JCalendar
     * will have their ActionEvent.getSource() == this JCalendar
     * (instead of the CalendarMonth).
     *
     * @param listener The ActionListener to add.
     */
    public void addActionListener(ActionListener listener) {
        if (_actionListener == null) {
            _actionListener = new ActionHandler();
            getCalendarMonth().addActionListener(_actionListener);
        }
        listenerList.add(ActionListener.class, listener);
    }

    /**
     * Remove a listener from the list that's notified each time a
     * action is performed
     *
     * @param listener The ActionListener to remove.
     * @see #addActionListener
     */
    public void removeActionListener(ActionListener listener) {
        listenerList.remove(ActionListener.class, listener);
    }

    /**
     * A ActionListener that forwards ActionEvents from
     * the CalendarMonth to the calendar ActionListeners.
     * The forwarded events only differ from the originals in that their
     * source is the calendar instead of the selectionModel itself.
     */
    private class ActionHandler implements ActionListener, Serializable {

        public void actionPerformed(ActionEvent e) {
            fireActionPerformed(e.getActionCommand(), e.getModifiers());
        }
    }

    /**
     * This method notifies  ActionListeners that an action has taken place.
     * It's used to forward ActionEvents from the CalendarMonth to
     * the ActionListeners added directly to JCalendar.
     */
    protected void fireActionPerformed(String command, int modifiers) {
        Object[] listeners = listenerList.getListenerList();
        ActionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                if (e == null) {
                    e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command, modifiers);
                }
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    /**
     * Add a listener to the calendar that's notified each time an
     * selection is made.  Listeners added directly to the JCalendar
     * will have their ActionEvent.getSource() == this JCalendar
     * (instead of the CalendarMonth).
     *
     * @param listener The ListSelectionListener to add.
     */
    public void addListSelectionListener(ListSelectionListener listener) {
        if (_selectionListener == null) {
            _selectionListener = new SelectionHandler();
            getCalendarMonth().addListSelectionListener(_selectionListener);
        }
        listenerList.add(ListSelectionListener.class, listener);
    }

    /**
     * A ListSelectionListener that forwards ListSelectionEvents from
     * the CalendarMonth to the calendar ListSelectionListeners.
     * The forwarded events only differ from the originals in that their
     * source is the calendar instead of the selectionModel itself.
     */
    private class SelectionHandler implements ListSelectionListener, Serializable {

        public void valueChanged(ListSelectionEvent e) {
            fireValueChanged(e.getFirstIndex(), e.getLastIndex(), e.getValueIsAdjusting());
        }
    }

    /**
     * This method notifies ListSelectionListeners of a selection.
     * It's used to forward ListSelectionEvents from the CalendarMonth to
     * the ListSelectionListeners added directly to JCalendar.
     */
    protected void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
        Object[] listeners = listenerList.getListenerList();
        ListSelectionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ListSelectionListener.class) {
                if (e == null) {
                    e = new ListSelectionEvent(this, firstIndex, lastIndex, isAdjusting);
                }
                ((ListSelectionListener) listeners[i + 1]).valueChanged(e);
            }
        }
    }

    /**
     * Add a listener to the calendar that's notified each time an
     * selection is made.  Listeners added directly to the JCalendar
     * will have their ActionEvent.getSource() == this JCalendar
     * (instead of the CalendarMonth).
     *
     * @param listener The CalendarSelectionListener to add.
     */
    public void addCalendarSelectionListener(CalendarSelectionListener listener) {
        if (_calendarSelectionListener == null) {
            _calendarSelectionListener = new CalendarSelectionHandler();
            getCalendarMonth().addCalendarSelectionListener(_calendarSelectionListener);
        }
        listenerList.add(CalendarSelectionListener.class, listener);
    }

    /**
     * A CalendarSelectionListener that forwards CalendarSelectionEvents from
     * the CalendarMonth to the calendar CalendarSelectionListeners.
     * The forwarded events only differ from the originals in that their
     * source is the calendar instead of the selectionModel itself.
     */
    private class CalendarSelectionHandler implements CalendarSelectionListener, Serializable {

        public void dateSelected(CalendarSelectionEvent e) {
            fireDateSelected(new YearMonthDay(e.getYear(), e.getMonth(), e.getDay()));
        }
    }

    /**
     * This method notifies ListSelectionListeners of a selection.
     * It's used to forward ListSelectionEvents from the CalendarMonth to
     * the ListSelectionListeners added directly to JCalendar.
     */
    protected void fireDateSelected(YearMonthDay ymd) {
        Object[] listeners = listenerList.getListenerList();
        CalendarSelectionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CalendarSelectionListener.class) {
                if (e == null) {
                    e = new CalendarSelectionEvent(this, ymd.year, ymd.month, ymd.day);
                }
                ((CalendarSelectionListener) listeners[i + 1]).dateSelected(e);
            }
        }
    }

    /**
     * A convenience method to allow client to set selection to specified day.
     */
    public void setDate(Date d) {
        getCalendarMonth().setDate(d);
    }

    /**
     * A convenience method to allow client to get selection, if any.
     * The selection was actually just a year, month and day number.
     * These must be interpreted in a time zone to mean anything.
     * This returned date it interpreted in the default time zone.
     */
    public Date getDate() {
        YearMonthDay ymd = getCalendarMonth().getDate();
        if (ymd == null)
            return null;
        Calendar c = new GregorianCalendar();
        c.clear();
        c.setTimeZone(_tz);
        c.set(Calendar.YEAR, ymd.year);
        c.set(Calendar.MONTH, ymd.month);
        c.set(Calendar.DAY_OF_MONTH, ymd.day);
        return c.getTime();
    }

    /**
     * Test driver for JCalendar
     */
    public static void main(String args[]) {
        JFrame frame = new JFrame();
        frame.setTitle("JCalendar Test Harness");
        frame.setBounds(50, 100, 350, 400);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        DefaultCalendarModel model = new DefaultCalendarModel(1999, 0, 1999, 6);
        JPanel pan = new JPanel(new BorderLayout());
        CalendarMonth cm = new CalendarMonth(model);
        DefaultCalendarCellRenderer r = (DefaultCalendarCellRenderer) cm.getCellRenderer();
        cm.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        cm.setDisplayMode(CalendarMonth.MULTI_MONTH_MODE);
        r.setBackground1(AbstractCalendarRenderer.DEFAULT_MULTI_MONTH_BACKGROUND1);
        r.setBackground2(AbstractCalendarRenderer.DEFAULT_MULTI_MONTH_BACKGROUND2);
        JCalendar jcal = new JCalendar(cm, true, true, true, true);
        jcal.addListSelectionListener(e -> System.out.println("Got selection event: " + e.toString()));
        pan.add(jcal);
        frame.getContentPane().add("Center", pan);

        //frame.pack();
        frame.setVisible(true);
    }

}
