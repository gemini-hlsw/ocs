package edu.gemini.shared.gui.calendar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.TimeZone;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.ListSelectionModel;
import javax.swing.CellRendererPane;
import javax.swing.DefaultListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputListener;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.border.Border;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.text.DateFormatSymbols;
import java.io.Serializable;

import edu.gemini.shared.gui.ThinBorder;
import edu.gemini.shared.util.CalendarUtil;

/**
 * A CalendarMonth is a graphical representation of
 * the Gregorian Calendar.  It can display one or two integral months
 * in a span of time controlled by the data model.
 * It shows the days of the month and optionally the names of the days on top.
 * This implementation is based heavily on JList.
 * Should implement Scrollable someday.
 */

// Note - inheriting from JPanel will not clear the inside of the component
// on resize.  JLabel is convenient for this purpose.  There is no other
// reason to inherit from JLabel.

public class CalendarMonth extends JLabel implements MouseInputListener, KeyListener, FocusListener, CalendarDataListener {

    /** Number of days in a week. */
    public static final int DAYS_IN_WEEK = 7;

    /** Show only a single month. */
    public static final int SINGLE_MONTH_MODE = 0;

    /** Show two months. */
    public static final int MULTI_MONTH_MODE = 1;

    /** Maximum number of months visible at one time. */
    public static final int MAX_NUM_MONTHS_VISIBLE = 2;  // most months shown

    /** Minimum number of months visible at one time. */
    public static final int MIN_NUM_MONTHS_VISIBLE = 1;  // fewest months shown

    /** Color used to paint text. */
    public static final Color DEFAULT_FOREGROUND_COLOR = new Color(0x000000);

    /** Color used to draw grid lines. */
    public static final Color DEFAULT_GRID_COLOR = new Color(0x000000);

    /** Color used to indicate selections. */
    public static final Color DEFAULT_SELECTION_BACKGROUND = new Color(0xff2222);

    /** A command string for an ActionEvent fired by this component
     when enter key is pressed. */
    public static final String ACTION_ENTER = "Enter";

    /** A command string for an ActionEvent fired by this component
     when mouse is clicked. */
    public static final String ACTION_CLICK = "Click";

    /** A command string for an ActionEvent fired by this component
     when calendar is advanced to next month. */
    public static final String ACTION_NEXT = "Next";

    /** A command string for an ActionEvent fired by this component
     when calendar rolls to previous month. */
    public static final String ACTION_PREV = "Prev";

    /** A command string for an ActionEvent fired by this component
     when calendar is rolled to beginning of model interval. */
    public static final String ACTION_START = "Start";

    /** A command string for an ActionEvent fired by this component
     when calendar is rolled to end of model interval. */
    public static final String ACTION_END = "End";

    /** A command string for an ActionEvent fired by this component
     when calendar is advanced forward a page jump. */
    public static final String ACTION_PAGE_FORWARD = "PageForward";

    /** A command string for an ActionEvent fired by this component
     when calendar is advanced backward a page jump. */
    public static final String ACTION_PAGE_BACKWARD = "PageBackward";

    /**
     * A command string for an ActionEvent fired by this component
     * when a single date is selected in SINGLE_SELECTION_MODE
     */
    public static final String ACTION_SELECT = "Select";

    /** Mode that sets configuration appropriate for popup widget. */
    public static final int POPUP_MODE = 0;

    /** Mode that sets configuration appropriate for multi-month widget. */
    public static final int MULTI_MODE = 1;

    private static final int LINE_WIDTH = 1;  // width of grid lines

    // calendar will automatically switch from 2 to 1 month display
    // when aspect ratio limits are reached.  ratio = width / heightn
    private static final float MAX_ASPECT_RATIO = (float) 0.85;

    // How far does page up/down move?
    private static final int DEFAULT_PAGE_JUMP = 12;  // in months

    private int _pageJump = DEFAULT_PAGE_JUMP;

    /** Cached values used to paint the widget */
    protected int calendarTop = 0;

    protected int calendarBottom = 0;

    protected int calendarLeft = 0;

    protected int calendarRight = 0;

    protected int _weekRows;         // # of rows displayed

    protected int _rowHeight = 0;    // height of row including one line width

    protected int _columnWidth = 0;  // width of column including one line width

    protected int[] _daysInMonths;  // # of days in each month in model interval

    protected int _numberOfDays;      // # days in model interval

    private MonthYear _from;        // first month shown on calendar

    private MonthYear _to;          // last month shown on calendar

    private int _numberOfMonths;    // number of months shown

    /** Cached when calendar rolls */
    private RowColumn _month1FirstDay, _month1LastDay;

    private RowColumn _month2FirstDay, _month2LastDay;

    private int _month1FirstDayIndex, _month2LastDayIndex;

    private boolean _fromEven;   // whether first month is even or odd wrt model

    // If this is true, the model will be created to serve a single month
    // and a calendar rolling operation will create a new one-month model.
    private boolean _movingModel = false;

    private YearMonthDay _movingModelDate = null;

    /** Constant indicates cell size is constant as size of moonths change. */
    public static final int CONSTANT_SIZE = 0;

    /** Constant indicates cell size is variable as size of moonths change. */
    public static final int VARIABLE_SIZE = 1;

    private int _gridSizingMode;

    private boolean _showGrid = false;

    private Color _gridColor = DEFAULT_GRID_COLOR;

    private int _monthDisplayMode = SINGLE_MONTH_MODE;

    protected boolean hasFocus = false;

    private CalendarModel _calendarModel = null;

    private CalendarCellRenderer _calendarCellRenderer = null;

    private ListSelectionModel _listSelectionModel = null;

    private ListSelectionListener _selectionListener;

    private CellRendererPane _renderPane = new CellRendererPane();

    private CalendarCellRenderer _calendarHeaderRenderer;

    private TimeZone _timeZone;

    // Remember that JComponent has this protected member:
    // protected EventListenerList listenerList = new EventListenerList();

    // This variable indicates whether cached valued need to be recalculated.
    private boolean _dirty = false;

    /**
     * Constructs a CalendarMonth with the default model, selection model,
     * and cell renderer.
     * Selection model defaults to SINGLE_SELECTION.
     */
    public CalendarMonth() {
        this(new DefaultCalendarModel(), new DefaultListSelectionModel(), new DefaultCalendarCellRenderer());
        _movingModel = true;
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * Constructs a CalendarMonth with the default model spanning single month
     * containing date, the specified selection model and cell renderer.
     * Selection model defaults to SINGLE_SELECTION.
     */
    public CalendarMonth(Date date) {
        this(new DefaultCalendarModel(date), new DefaultListSelectionModel(), new DefaultCalendarCellRenderer());
        Calendar c = _newGregorianCalendarInstance(date);
        _setMovingModel(new YearMonthDay(c.get(c.YEAR), c.get(c.MONTH), c.get(c.DAY_OF_MONTH)));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
      * Constructs a CalendarMonth with the default model spanning single month
      * containing date, the specified selection model and cell renderer.
      * Selection model defaults to SINGLE_SELECTION.
      */
     public CalendarMonth(Date date, TimeZone tz) {
         this(new DefaultCalendarModel(date, tz), new DefaultListSelectionModel(), new DefaultCalendarCellRenderer(), tz);
         Calendar c = _newGregorianCalendarInstance(date);
         _setMovingModel(new YearMonthDay(c.get(c.YEAR), c.get(c.MONTH), c.get(c.DAY_OF_MONTH)));
         setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
     }


    /**
     * Constructs a CalendarMonth with the specified model.
     */
    public CalendarMonth(CalendarModel model) {
        this(model, new DefaultListSelectionModel(), new DefaultCalendarCellRenderer());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * Constructs CalendarMonth.
     */
    public CalendarMonth(CalendarModel model, ListSelectionModel selector, CalendarCellRenderer renderer) {
        this(model, selector, renderer, TimeZone.getDefault());
    }

    /**
     * Constructs CalendarMonth.
     */
    public CalendarMonth(CalendarModel model, ListSelectionModel selector, CalendarCellRenderer renderer, TimeZone tz) {
        if (model == null) {
            throw new IllegalArgumentException("model must be non null");
        }
        if (selector == null) {
            throw new IllegalArgumentException("selector must be non null");
        }
        if (renderer == null) {
            throw new IllegalArgumentException("renderer must be non null");
        }
        _timeZone = tz;
        _listSelectionModel = selector;
        _calendarCellRenderer = renderer;
        setLayout(new BorderLayout());
        _renderPane = new CellRendererPane();
        add(_renderPane, BorderLayout.CENTER);
        addMouseMotionListener(this);
        addMouseListener(this);
        addFocusListener(this);
        addKeyListener(this);
        setModel(model);
        _from = new MonthYear(getModel().getStartYear(), getModel().getStartMonth());
        _to = new MonthYear(_from);
        _setNumberOfMonths(1);
        moveToStart();
        setForeground(DEFAULT_FOREGROUND_COLOR);
        setGridColor(DEFAULT_GRID_COLOR);
        setGridSizingMode(CONSTANT_SIZE); // CONSTANT_SIZE or VARIABLE_SIZE

        // This listener will watch for resize events and recalculate
        // cached paint values.
        addComponentListener(new ResizeListener());
    }

    /**
     * Gets information about the span of time currently shown on the calendar.
     */
    public MonthYear getFrom() {
        return _from;
    }

    /**
     * Gets information about the span of time currently shown on the calendar.
     */
    public MonthYear getTo() {
        return _to;
    }

    /**
     * Gets the timezone used internally by this calendar.
     */
    public TimeZone getTimeZone() {
        return _timeZone;
    }

    /**
     * Sets the timezone used internally by this calendar.
     */
    public void setTimeZone(TimeZone timeZone) {
        _timeZone = timeZone;
    }

    /**
     * Gets the size of a page jump in months.
     */
    public int getPageJump() {
        return _pageJump;
    }

    /**
     * Sets the size of a page jump in months.
     * @param jump The size of a page jump in months.
     * Used by pageForward(), pageBackward()
     */
    public void setPageJump(int jump) {
        _pageJump = jump;
    }

    /**
     * Convenience method that configures the CalendarMonth appropriately
     * for a given use.  It relieves the client of the burdon of setting
     * many configurations.
     * This method was inspired by KLG tables which offer similar capability.
     * These settings are not permanent.  A client can subsequently set
     * configurations and overwrite the settings made by this method call.
     * If the mode is not recognized, this is a no-op.
     * Note that a side effect of this method is that it sets the cell renderer.
     * @param mode POPUP_MODE configures appearance to be suitable for a popup.
     * MULTI_MODE configures appearance to be suitable for multi-month range
     * chooser.
     */
    public void setMode(int mode) {
        if (mode == POPUP_MODE) {
            DefaultCalendarCellRenderer r = new DefaultCalendarCellRenderer();
            configureDefaultCalendarCellRendererForPopup(r);
            setCellRenderer(r);
        } else if (mode == MULTI_MODE) {
            // These settings make a good-looking multi-month, multi-colored display
        }
    }

    /**
     * Convenience method to configure a renderer appropriately for
     * use in a popup.
     */
    protected void configureDefaultCalendarCellRendererForPopup(DefaultCalendarCellRenderer r) {
        // These settings make for a pretty good popup calendar
        r.setHorizontalFraction(.85);
        r.setVerticalFraction(.85);
        r.setX_Margin(0);
        r.setY_Margin(0);
        r.setVerticalAlignment(SwingConstants.CENTER);
        r.setHorizontalAlignment(SwingConstants.CENTER);
        r.setHorizontalDateTextAlignment(SwingConstants.RIGHT);
        r.setSelectedBackground(new Color(0x8888ff));
        r.setSelectedBorder(null);
        r.setUnselectedBorder(null);
        r.setFont(new Font(r.getFont().getName(), Font.PLAIN, 8));
    }

    // Sets the moving model with a memory of the original date so when
    // you scan by that month it shows as selected.
    private void _setMovingModel(YearMonthDay ymd) {
        _movingModel = true;
        _movingModelDate = ymd;
    }

    /**
     * Gets the data model for this calendar.
     */
    public CalendarModel getModel() {
        return _calendarModel;
    }

    /**
     * Sets the data model for this calendar.
     */
    public void setModel(CalendarModel model) {
        _movingModel = false;
        if (_calendarModel != null && _calendarModel instanceof CalendarModel) {
            _calendarModel.removeCalendarDataListener(this);
        }
        _calendarModel = model;
        if (model != null && model instanceof CalendarModel) {
            _calendarModel.addCalendarDataListener(this);
        }
        _modelChanged();
    }

    private void _modelChanged() {
        _from = new MonthYear(getModel().getStart());
        getModel().addCalendarDataListener(this);
        getSelectionModel().clearSelection();
        // Cache the days in the months so indexes can quickly be calculated
        MonthYear start = getModel().getStart();
        MonthYear end = getModel().getEnd();
        int difference = MonthYear.difference(end, start).getMonths();

        // cache a running count of days in each month from start of model
        _daysInMonths = new int[difference + 1];
        _numberOfDays = 0;
        Calendar c = _newGregorianCalendarInstance(start.year, start.month, 1);
        for (int i = 0; i <= difference; ++i) {
            int days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            _daysInMonths[i] = days;
            _numberOfDays += days;
            c.add(Calendar.MONTH, 1);
        }
        displayChanged();
    }

    /**
     * Gets the selection model for this calendar.
     */
    public ListSelectionModel getSelectionModel() {
        return _listSelectionModel;
    }

    /**
     * Sets the selection model for this calendar.
     */
    public void setSelectionModel(ListSelectionModel model) {
        /* This was in the JList source code, I don't know why
        if (_selectionListener == null) {
            _selectionListener = new ListSelectionHandler();
            getSelectionModel().addCalendarSelectionListener(_selectionListener);
        }
        */

        if (_listSelectionModel != null && _listSelectionModel instanceof ListSelectionModel) {
            if (_selectionListener != null) {
                _listSelectionModel.removeListSelectionListener(_selectionListener);
            }
        }
        _listSelectionModel = model;
        if (model != null && model instanceof ListSelectionModel) {
            if (_selectionListener == null) {
                _selectionListener = new ListSelectionHandler();
            }
            _listSelectionModel.addListSelectionListener(_selectionListener);
        }
        markDirty();  // need to recalculate paint values
        repaint();
    }

    // The following methods were copied from JList source.
    // It offers passthroughs for its selection model.

    /**
     * Determines whether single-item or multiple-item
     * selections are allowed.
     * The following selectionMode values are allowed:
     * <ul>
     * <li> <code>SINGLE_SELECTION</code>
     *   Only one list index can be selected at a time.  In this
     *   mode the setSelectionInterval and addSelectionInterval
     *   methods are equivalent, and they only the first index
     *   argument is used.
     * <li> <code>SINGLE_INTERVAL_SELECTION</code>
     *   One contiguous index interval can be selected at a time.
     *   In this mode setSelectionInterval and addSelectionInterval
     *   are equivalent.
     * <li> <code>MULTIPLE_INTERVAL_SELECTION</code>
     *   In this mode, there's no restriction on what can be selected.
     * </ul>
     *
     * @param selectionMode an int specifying the type of selections
     *        that are permissible
     * @see #getSelectionMode
     * @beaninfo
     * description: The selection mode.
     *        enum: SINGLE_SELECTION            ListSelectionModel.SINGLE_SELECT
     ION
     *              SINGLE_INTERVAL_SELECTION   ListSelectionModel.SINGLE_INTERV
     AL_SELECTION
     *              MULTIPLE_INTERVAL_SELECTION ListSelectionModel.MULTIPLE_INTE
     RVAL_SELECTION
     */
    public void setSelectionMode(int selectionMode) {
        getSelectionModel().setSelectionMode(selectionMode);
    }

    /**
     * Returns whether single-item or multiple-item selections are allowed.
     * @return The value of the selectionMode property.
     * @see #setSelectionMode
     */
    public int getSelectionMode() {
        return getSelectionModel().getSelectionMode();
    }

    /**
     * Returns the smallest selected cell index.
     * This is a convenience method that just delegates to the selectionModel.
     *
     * @return The smallest selected cell index.
     * @see ListSelectionModel#getMinSelectionIndex
     * @see #addListSelectionListener
     */
    public int getMinSelectionIndex() {
        return getSelectionModel().getMinSelectionIndex();
    }

    /**
     * Returns the largest selected cell index.
     * This is a convenience method that just delegates to the selectionModel.
     *
     * @return The largest selected cell index.
     * @see ListSelectionModel#getMaxSelectionIndex
     * @see #addListSelectionListener
     */
    public int getMaxSelectionIndex() {
        return getSelectionModel().getMaxSelectionIndex();
    }

    /**
     * Returns true if the specified index is selected.
     * This is a convenience method that just delegates to the selectionModel.
     *
     * @return True if the specified index is selected.
     * @see ListSelectionModel#isSelectedIndex
     * @see #setSelectedIndex
     * @see #addListSelectionListener
     */
    public boolean isSelectedIndex(int index) {
        return getSelectionModel().isSelectedIndex(index);
    }

    /**
     * Returns true if nothing is selected
     * This is a convenience method that just delegates to the selectionModel.
     *
     * @return True if nothing is selected
     * @see ListSelectionModel#isSelectionEmpty
     * @see #clearSelection
     * @see #addListSelectionListener
     */
    public boolean isSelectionEmpty() {
        return getSelectionModel().isSelectionEmpty();
    }

    /**
     * Clears the selection - after calling this method isSelectionEmpty()
     * will return true.
     * This is a convenience method that just delegates to the selectionModel.
     *
     * @see ListSelectionModel#clearSelection
     * @see #isSelectionEmpty
     * @see #addListSelectionListener
     */
    public void clearSelection() {
        getSelectionModel().clearSelection();
    }

    /**
     * Select the specified interval.  Both the anchor and lead indices are
     * included.  It's not neccessary for anchor to be less than lead.
     * This is a convenience method that just delegates to the selectionModel.
     *
     * @param anchor The first index to select
     * @param lead The last index to select
     * @see ListSelectionModel#setSelectionInterval
     * @see #addSelectionInterval
     * @see #removeSelectionInterval
     * @see #addListSelectionListener
     */
    public void setSelectionInterval(int anchor, int lead) {
        getSelectionModel().setSelectionInterval(anchor, lead);
        markDirty();
        repaint();
    }

    /**
     * Set the selection to be the union of the specified interval with current
     * selection.  Both the anchor and lead indices are
     * included.  It's not neccessary for anchor to be less than lead.
     * This is a convenience method that just delegates to the selectionModel.
     *
     * @param anchor The first index to add to the selection
     * @param lead The last index to add to the selection
     * @see ListSelectionModel#addSelectionInterval
     * @see #setSelectionInterval
     * @see #removeSelectionInterval
     * @see #addListSelectionListener
     */
    public void addSelectionInterval(int anchor, int lead) {
        getSelectionModel().addSelectionInterval(anchor, lead);
    }

    /**
     * Set the selection to be the set difference of the specified interval
     * and the current selection.  Both the anchor and lead indices are
     * removed.  It's not neccessary for anchor to be less than lead.
     * This is a convenience method that just delegates to the selectionModel.
     *
     * @param anchor The first index to remove from the selection
     * @param lead The last index to remove from the selection
     * @see ListSelectionModel#removeSelectionInterval
     * @see #setSelectionInterval
     * @see #addSelectionInterval
     * @see #addListSelectionListener
     */
    public void removeSelectionInterval(int index0, int index1) {
        getSelectionModel().removeSelectionInterval(index0, index1);
        markDirty();
        repaint();
    }

    /**
     * Sets the data model's isAdjusting property true, so that
     * a single event will be generated when all of the selection
     * events have finished (for example, when the mouse is being
     * dragged over the list in selection mode).
     *
     * @param b the boolean value for the property value
     * @see ListSelectionModel#setValueIsAdjusting
     */
    public void setValueIsAdjusting(boolean b) {
        getSelectionModel().setValueIsAdjusting(b);
    }

    /**
     * Returns the value of the data model's isAdjusting property.
     * This value is true if multiple changes are being made.
     *
     * @return true if multiple selection-changes are occuring, as
     *         when the mouse is being dragged over the list
     * @see ListSelectionModel#getValueIsAdjusting
     */
    public boolean getValueIsAdjusting() {
        return getSelectionModel().getValueIsAdjusting();
    }

    /**
     * Return an array of all of the selected indices in increasing
     * order.
     *
     * @return All of the selected indices, in increasing order.
     * @see #removeSelectionInterval
     * @see #addListSelectionListener
     */
    public int[] getSelectedIndices() {
        ListSelectionModel sm = getSelectionModel();
        int iMin = sm.getMinSelectionIndex();
        int iMax = sm.getMaxSelectionIndex();
        if ((iMin < 0) || (iMax < 0)) {
            return new int[0];
        }
        int[] rvTmp = new int[1 + (iMax - iMin)];
        int n = 0;
        for (int i = iMin; i <= iMax; i++) {
            if (sm.isSelectedIndex(i)) {
                rvTmp[n++] = i;
            }
        }
        int[] rv = new int[n];
        System.arraycopy(rvTmp, 0, rv, 0, n);
        return rv;
    }

    /**
     * Select a single cell.
     *
     * @param index The index of the one cell to select
     * @see ListSelectionModel#setSelectionInterval
     * @see #isSelectedIndex
     * @see #addListSelectionListener
     * @beaninfo
     * description: The index of the selected cell.
     */
    public void setSelectedIndex(int index) {
        getSelectionModel().setSelectionInterval(index, index);
        markDirty();
        repaint();
    }

    /**
     * Select a set of cells.
     *
     * @param indices The indices of the cells to select
     * @see ListSelectionModel#addSelectionInterval
     * @see #isSelectedIndex
     * @see #addListSelectionListener
     */
    public void setSelectedIndices(int[] indices) {
        ListSelectionModel sm = getSelectionModel();
        sm.clearSelection();
        for (int i = 0; i < indices.length; i++) {
            sm.addSelectionInterval(indices[i], indices[i]);
        }
        markDirty();
        repaint();
    }

    /**
     * A convenience method that returns the first selected index.
     * Returns -1 if there is no selected item.
     *
     * @return The first selected index.
     * @see #getMinSelectionIndex
     * @see #addListSelectionListener
     */
    public int getSelectedIndex() {
        return getMinSelectionIndex();
    }

    /**
     * Returns whether the specified date is selected.
     */
    public boolean isSelected(int year, int month, int day) {
        int index = dayIndex(new YearMonthDay(year, month, day));
        if (index < 0)
            return false;
        return getSelectionModel().isSelectedIndex(index);
    }

    /**
     * Returns the index of specified day relative to start of model time range.
     */
    public int dayIndex(YearMonthDay ymd) {
        // Calculate the index of this date relative to the start
        // of the model interval.
        YearMonthDay start = new YearMonthDay(getModel().getStart().year, getModel().getStart().month, 1);
        Calendar endCal = _newGregorianCalendarInstance(getModel().getEnd().year, getModel().getEnd().month, 1);
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        YearMonthDay end = new YearMonthDay(endCal);
        if (ymd.before(start) || ymd.after(end))
            return -1;
        MonthYear my = new MonthYear(ymd.year, ymd.month);
        int dayIndex = 0;
        int monthCounter = 0;
        MonthYear localMY = new MonthYear(start.year, start.month);
        while (localMY.before(my)) {
            dayIndex += _daysInMonths[monthCounter];
            monthCounter++;
            localMY.add(MonthYear.MONTH, 1);
        }
        return dayIndex + ymd.day - 1;
    }

    /**
     * Returns the index of specified cell relative to start of model time range.
     */
    public int dayIndex(RowColumn rowColumn) {
        if (rowColumn.lessThan(_month1FirstDay) || rowColumn.greaterThan(_month2LastDay)) {
            return -1;
        }
        return rowColumn.difference(_month1FirstDay) + _month1FirstDayIndex;
    }

    /**
     * Returns the index of specified day relative to start of model time range.
     */
    public YearMonthDay dateFromIndex(int index) {
        if (index < 0)
            return null;
        int dayIndex = 0;
        int monthCounter = 0;
        MonthYear localMY = new MonthYear(getModel().getStart().year, getModel().getStart().month);
        int daysN = 0;
        while (index >= daysN && monthCounter < _daysInMonths.length) {
            daysN = _daysInMonths[monthCounter];
            if (index >= daysN) {
                index -= daysN;
                monthCounter++;
                localMY.add(MonthYear.MONTH, 1);
            }
        }
        return new YearMonthDay(localMY.year, localMY.month, index + 1);
    }

    /**
     * Gets the cell renderer for this calendar.
     */
    public CalendarCellRenderer getCellRenderer() {
        return _calendarCellRenderer;
    }

    /**
     * Sets the cell renderer for this calendar.
     */
    public void setCellRenderer(CalendarCellRenderer renderer) {
        _calendarCellRenderer = renderer;
        markDirty();  // need to recalculate paint values
        repaint();
    }

    /**
     * Gets the header renderer for this calendar.
     */
    public CalendarCellRenderer getHeaderRenderer() {
        return _calendarHeaderRenderer;
    }

    /**
     * Sets the header renderer for this calendar.
     */
    public void setHeaderRenderer(CalendarCellRenderer renderer) {
        _calendarHeaderRenderer = renderer;
        markDirty();  // need to recalculate paint values
        repaint();
    }

    /**
     * Gets the color used to render the grid and moon phases on the calendar.
     */
    public Color getGridColor() {
        return _gridColor;
    }

    /**
     * Sets the color used to render the grid and moon phases on the calendar.
     */
    public void setGridColor(Color c) {
        _gridColor = c;
        markDirty();
        repaint();
    }

    /**
     * Gets whether to show the grid lines on the calendar.
     */
    public boolean getShowGrid() {
        return _showGrid;
    }

    /**
     * Sets whether to show the grid lines on the calendar.
     */
    public void setShowGrid(boolean b) {
        if (getShowGrid() == b)
            return;
        _showGrid = b;
        displayChanged();
    }

    /**
     * Returns the grid line width.  If grid is turned off, line width is zero.
     */
    protected int getLineWidth() {
        return getShowGrid() ? LINE_WIDTH : 0;
    }

    /**
     * Returns the mode for calculating grid size.
     * See setGridSizingMode().
     */
    public int getGridSizingMode() {
        return _gridSizingMode;
    }

    /**
     * Sets the mode for calculating grid size.
     * Can be CONSTANT_SIZE or VARIABLE_SIZE
     * Defaults to CONSTANT_SIZE.
     */
    public void setGridSizingMode(int mode) {
        if (mode != CONSTANT_SIZE && mode != VARIABLE_SIZE)
            return;
        _gridSizingMode = mode;
        markDirty();
        displayChanged();
    }

    /**
     * Gets the mode for display.
     */
    public int getDisplayMode() {
        return _monthDisplayMode;
    }

    /**
     * Sets the mode for display.  Allowed values are:
     * SINGLE_MONTH_MODE - shows a single month
     * MULTI_MONTH_MODE - shows two months at a time if model's range
     * has two or more months.
     */
    public void setDisplayMode(int mode) {
        if (mode == MULTI_MONTH_MODE) {
            _setNumberOfMonths(2);
            if (_movingModel) {
                _to = new MonthYear(_from);
                _to.add(MonthYear.MONTH, 1);
            } else if (getModel().getSize() < 2) {
                return;  // can't display 2 months in 1-month calendar
            } else {
                if (atEnd()) {
                    _to = new MonthYear(_from);
                    _from.add(MonthYear.MONTH, -1);
                } else {
                    _to = new MonthYear(_from);
                    _to.add(MonthYear.MONTH, 1);
                }
            }
            _monthDisplayMode = mode;
        } else {
            _setNumberOfMonths(1);
            _to = new MonthYear(_from);
            _monthDisplayMode = SINGLE_MONTH_MODE;
        }
        if (_movingModel)
            setMovingModel();
        markDirty();
        displayChanged();
    }

    /**
     * Returns the height of a single date cell.
     */
    public int cellHeight() {
        return _rowHeight;
    }

    /**
     * Returns the number of rows currently showing on the calendar.
     * This is calculated by the calendar, it is not a property
     * that can be set.
     */
    public int numberOfRows() {
        return _weekRows;
    }

    /**
     * Returns the width of a single date cell.
     */
    public int cellWidth() {
        return _columnWidth;
    }

    /**
     * Called when the displayed month(s) changes.
     * Probably in response to previousMonth(), nextMonth(), or setModel().
     */
    protected void displayChanged() {
        // Cache values related to the displayed month.

        // Save the cell location of first and last days of each month
        _month1FirstDay = new CalendarRowColumn(0, CalendarUtil.dayOfWeekOfFirstDayOfMonth(getFrom().year, getFrom().month));
        _month1LastDay = new CalendarRowColumn(_month1FirstDay);
        _month1LastDay.add(RowColumn.COLUMN, CalendarUtil.daysInMonth(getFrom().year, getFrom().month) - 1);
        _to = new MonthYear(_from);
        if (getDisplayMode() == MULTI_MONTH_MODE && numberOfMonths() == 2) {
            _to = new MonthYear(_from);
            _to.add(MonthYear.MONTH, 1);
            _month2FirstDay = new CalendarRowColumn(_month1LastDay);
            _month2FirstDay.add(RowColumn.COLUMN, 1);
            _month2LastDay = new CalendarRowColumn(_month2FirstDay);
            _month2LastDay.add(RowColumn.COLUMN, CalendarUtil.daysInMonth(getTo().year, getTo().month) - 1);
        } else {
            _month2FirstDay = _month1FirstDay;
            _month2LastDay = _month1LastDay;
        }
        YearMonthDay ymd = cellToDay(_month1FirstDay);
        _month1FirstDayIndex = dayIndex(ymd);
        ymd = cellToDay(_month2LastDay);
        _month2LastDayIndex = dayIndex(ymd);

        // Figure whether the first displayed month is even or odd
        // wrt model interval
        _fromEven = (MonthYear.difference(getFrom(), getModel().getStart()).getMonths() % 2 == 0);
        markDirty();
        repaint();

        // notify listeners
        fireCalendarDisplayEvent();
    }

    /** Sets the model to a span containing currently displayed months,
     erases any selections. */
    protected void setMovingModel() {
        setModel(new DefaultCalendarModel(getFrom().year, getFrom().month, getTo().year, getTo().month));
        _movingModel = true;
        if (_movingModelDate != null) {
            _setDateNoNotify(_movingModelDate);
        }
    }

    /**
     * Attempts to roll the calendar forward one month.
     * Returns true if successful.
     * If calendar bumps into the end, this method returns false
     * and does not alter the calendar.
     */
    public boolean nextMonth() {
        if (getModel().getSize() == 0)
            return false;
        boolean at_limit = atEnd();
        if (!at_limit) {
            _from.add(MonthYear.MONTH, 1);
            _to.add(MonthYear.MONTH, 1);
            if (_movingModel)
                setMovingModel();
            displayChanged();
            fireActionEvent(ACTION_NEXT);
        }
        return (!at_limit);
    }

    /**
     * Attempts to roll the calendar backward one month.
     * Returns true if successful.
     * If calendar bumps into the end, this method returns false
     * and does not alter the calendar.
     */
    public boolean previousMonth() {
        if (getModel().getSize() == 0)
            return false;
        boolean at_limit = atStart();
        if (!at_limit) {
            _from.add(MonthYear.MONTH, -1);
            _to.add(MonthYear.MONTH, -1);
            if (_movingModel)
                setMovingModel();
            displayChanged();
            fireActionEvent(ACTION_PREV);
        }
        return (!at_limit);
    }

    /**
     * Attempts to roll the calendar forward by one page jump.
     * Returns true if it moves at all.  It won't move beyond the model range.
     */
    public boolean pageForward() {
        if (getModel().getSize() == 0)
            return false;
        boolean at_limit = atEnd();
        if (at_limit)
            return false;
        int i = 0;
        while (i < getPageJump() && !at_limit) {
            _from.add(MonthYear.MONTH, 1);
            _to.add(MonthYear.MONTH, 1);
            at_limit = atEnd();
            if (_movingModel)
                setMovingModel();
            ++i;
        }
        displayChanged();
        fireActionEvent(ACTION_PAGE_FORWARD);
        return true;
    }

    /**
     * Attempts to roll the calendar backward by one page jump.
     * Returns true if it moves at all.  It won't move beyond the model range.
     */
    public boolean pageBackward() {
        if (getModel().getSize() == 0)
            return false;
        boolean at_limit = atStart();
        if (at_limit)
            return false;
        int i = 0;
        while (i < getPageJump() && !at_limit) {
            _from.add(MonthYear.MONTH, -1);
            _to.add(MonthYear.MONTH, -1);
            at_limit = atStart();
            if (_movingModel)
                setMovingModel();
            ++i;
        }
        displayChanged();
        fireActionEvent(ACTION_PAGE_BACKWARD);
        return true;
    }

    /**
     * Roll the calendar to the beginning of model's range.
     */
    public void moveToStart() {
        if (getModel().getSize() == 0)
            return;
        if (atStart())
            return;
        _from = new MonthYear(getModel().getStart());
        _to = new MonthYear(_from);
        if (numberOfMonths() > 1) {
            _to.add(MonthYear.MONTH, 1);
        }
        if (_movingModel)
            setMovingModel();
        else
            displayChanged();
        fireActionEvent(ACTION_START);
    }

    /**
     * Roll the calendar to the end of model's range.
     */
    public void moveToEnd() {
        if (getModel().getSize() == 0)
            return;
        if (atEnd())
            return;
        _to = new MonthYear(getModel().getEnd());
        _from = new MonthYear(_to);
        if (numberOfMonths() > 1) {
            _from.add(MonthYear.MONTH, -1);
        }
        if (_movingModel)
            setMovingModel();
        else
            displayChanged();
        fireActionEvent(ACTION_END);
    }


    /**
     * Roll the calendar to specified month.
     * If specified month is in range, calendar rolls to that month
     * and routine returns true.  If out of range, nothing is done
     * and routine returns false.
     */
    /*public boolean
    moveTo(int year, int month)
    {
      if (numberOfMonths() == 0) return false;
      MonthYear target = new MonthYear(year, month);
      MonthYear fromLimit = getModel().getStart();
      if (target.before(fromLimit)) return false;
      MonthYear toLimit = getModel().getStart();
      if (target.after(toLimit)) return false;
      // target is in range of model
      _from = target;
      _to = new MonthYear(_from);
      if (numberOfMonths() > 1)
      {
        _to.add(MonthYear.MONTH, 1);
      }
      displayChanged();
      return true;
    }*/

    /**
     * Returns true if the calendar is at the beginning of its displayable range.
     */
    public boolean atStart() {
        if (_movingModel)
            return false;
        if (getModel().getSize() < 2)
            return true;
        return (_from.equals(getModel().getStart()));
    }

    /**
     * Returns true if the calendar is at the end of its displayable range.
     */
    public boolean atEnd() {
        if (_movingModel)
            return false;
        if (getModel().getSize() < 2)
            return true;
        return (_to.equals(getModel().getEnd()));
    }

    /**
     * Returns the number of months currently showing on the calendar.
     * This does not take into account the number of months in the range
     * of the data model.  For example, if the model has zero months in
     * range and the display mode is SINGLE_MONTH_MODE, thie routine still
     * returns 1 because that is the number of months it makes room for
     * in its display.
     */
    public int numberOfMonths() {
        return _numberOfMonths;
    }

    private void _setNumberOfMonths(int i) {
        if (i < 0)
            i = MIN_NUM_MONTHS_VISIBLE;
        if (i > 2)
            i = MAX_NUM_MONTHS_VISIBLE;
        _numberOfMonths = i;

        /*  if (i == 1) {
           _month2FirstDay = _month1FirstDay;
           _month2LastDay = _month1LastDay;
        } else {
          _month2FirstDay = new CalendarRowColumn(_month1LastDay);
          _month2FirstDay.add(RowColumn.COLUMN, 1);
          _month2LastDay = new CalendarRowColumn(_month2FirstDay);
          _month2LastDay.add(RowColumn.COLUMN,
                            CalendarUtil.daysInMonth(_to.year, _to.month) - 1);
        }
        Calendar c = cellToDay(_month2LastDay);
        _month2LastDayIndex = dayIndex(c); */
    }

    /**
     * This method calculates values needed to paint the calendar.
     * Since they may be expensive to calculate they are cached each
     * time the size is changed.
     */
    protected void cachePaintValues(Graphics g, Dimension d) {
        boolean aspectChange = false;

        // Calculate aspect ration to see if we need to cut down on the
        // number of months to see better.
        if (getDisplayMode() == MULTI_MONTH_MODE) {
            float aspectRatio = (float) (d.width) / (float) (d.height);
            //System.out.println("ratio: " + aspectRatio+ "  max: " + MAX_ASPECT_RATIO);
            /* Dynamically switch between 1 or 2 months based on aspect ratio.
            if(aspectRatio > MAX_ASPECT_RATIO)
            {
              _setNumberOfMonths(1);
            }
            else
            {
              _setNumberOfMonths(2);
              }*/
        }

        // Month mode could have changed or aspect ration could have
        // forced a change in number of displayed months.
        // Recalculate _to here.
        _to = new MonthYear(_from);
        if (numberOfMonths() == 2) {
            _to.add(MonthYear.MONTH, 1);
        }

        // We have to take the border size into account so we don't
        // paint over it.
        int borderTop = 0;
        int borderBottom = 0;
        int borderLeft = 0;
        int borderRight = 0;
        Border border = getBorder();
        if (border != null) {
            Insets insets = border.getBorderInsets(this);
            borderTop = insets.top;
            borderBottom = insets.bottom;
            borderLeft = insets.left;
            borderRight = insets.right;
        }

        // This is the width we have to work with.
        int clientWidth = d.width - borderLeft - borderRight;

        // Calculate width of calendar grid so it shows evenly spaced columns
        int targetWidth = clientWidth;
        int width = (targetWidth - (DAYS_IN_WEEK + 1) * getLineWidth()) / DAYS_IN_WEEK * DAYS_IN_WEEK + (DAYS_IN_WEEK + 1) * getLineWidth();
        // This would center the calendar, but then header doesn't know what to do
        //_calendarLeft = borderLeft + (clientWidth - width)/2;

        calendarLeft = borderLeft;
        calendarRight = calendarLeft + width; // - 1;
        // width of colum including one vertical line
        _columnWidth = (calendarRight - calendarLeft) / DAYS_IN_WEEK;

        // The header renderer needs to know only column width to do its thing.
        // So call its cachePaintValues() now.
        if (getHeaderRenderer() != null) {
            getHeaderRenderer().cachePaintValues(g, this);
        }

        // Now that we know where the bottom of the day of week text is,
        // we can decide where the top of the grid is.
        //calendarTop = _dayOfWeekTextY + fm.getDescent() + fm.getLeading();
        calendarTop = borderTop;

        // Calculate the height of rows
        if (getGridSizingMode() == CONSTANT_SIZE) {
            if (numberOfMonths() == 1) {
                _weekRows = 6;  // most weeks ever in one month
            } else {
                _weekRows = 10;  // most weeks ever in two months
            }
        } else {
            _weekRows = rowsInMonth(_from);
            if (numberOfMonths() == 2) {
                _weekRows += rowsInMonth(_to);
                if (CalendarUtil.dayOfWeekOfFirstDayOfMonth(_to.year, _to.month) != 1) {
                    _weekRows--;
                }
            }
        }
        int targetHeight = d.height - borderBottom - calendarTop;
        // real height should give even intervals for week rows
        int height = (targetHeight - (_weekRows + 1) * getLineWidth()) / _weekRows * _weekRows + (_weekRows + 1) * getLineWidth();
        calendarBottom = calendarTop + height - 1;
        // _rowHeight includes one horizontal line
        _rowHeight = (calendarBottom - calendarTop) / _weekRows;
        _markClean();  // mark that we have recalculated cached values
        if (getCellRenderer() != null)
            getCellRenderer().cachePaintValues(g, this);
    }

    private void _paintCalendarCells(Graphics g) {
        if (_calendarCellRenderer == null)
            return;
        Component component;
        int day;
        RowColumn firstDay = new CalendarRowColumn(0, RowColumn.MIN_COLUMN);
        RowColumn lastDay = new CalendarRowColumn(numberOfRows() - 1, DAYS_IN_WEEK);
        RowColumn localRowColumn = new CalendarRowColumn(firstDay);
        boolean even;
        Calendar c = _newGregorianCalendarInstance(_from.year, _from.month, 1);
        int index = _month1FirstDayIndex;
        YearMonthDay renderYMD = null;
        YearMonthDay localYMD = new YearMonthDay();
        boolean selected = false;
        int w = cellWidth();
        int h = cellHeight();
        do {
            even = _fromEven;
            if (localRowColumn.greaterThan(_month1LastDay))
                even = !even;
            renderYMD = null;
            if (localRowColumn.lessThan(_month1FirstDay) || localRowColumn.greaterThan(_month2LastDay)) {
                renderYMD = null;
                selected = false;
            } else {
                selected = isSelectedIndex(index);
                localYMD.year = c.get(c.YEAR);
                localYMD.month = c.get(c.MONTH);
                localYMD.day = c.get(c.DAY_OF_MONTH);
                renderYMD = localYMD;
            }
            component = getCellRenderer().getCalendarCellRendererComponent(this, renderYMD, even, selected, hasFocus);
            paintCell(g, component, this, cellLeft(localRowColumn), cellTop(localRowColumn), w, h);
            localRowColumn.add(RowColumn.COLUMN, 1);  // advance a day
            if (renderYMD != null) {
                c.add(Calendar.DAY_OF_MONTH, 1);
                index++;
            }
        } while (!localRowColumn.greaterThan(lastDay));
    }

    protected void paintCell(Graphics g, Component component, CalendarMonth cm, int x, int y, int w, int h) {
        _renderPane.paintComponent(g, component, cm, x, y, w, h);
    }

    private void _paintCalendarGrid(Graphics g) {
        // paint grid lines
        g.setColor(_gridColor);
        // draw vertical lines
        _columnWidth = (calendarRight - calendarLeft) / DAYS_IN_WEEK;
        for (int i = 0; i <= DAYS_IN_WEEK; ++i) {
            g.drawLine(_columnWidth * i + calendarLeft, calendarTop, _columnWidth * i + calendarLeft, calendarBottom);
        }
        // draw horizontal lines
        for (int i = 0; i <= _weekRows; ++i) {
            int y = calendarTop + i * _rowHeight;
            g.drawLine(calendarLeft, y, calendarRight, y);
        }
    }

    /**
     * This method is invoked by Swing to draw components.
     */
    public void paintComponent(Graphics g) {
        Dimension d = this.getSize();
        if (dirty())
            cachePaintValues(g, d);
        if (getCellRenderer() != null)
            getCellRenderer().cachePaintValues(g, this);
        _paintCalendarCells(g);
        if (getShowGrid())
            _paintCalendarGrid(g);
    }

    /**
     * Returns the cell corresponding to specified date.
     * If date is not in currently displayed range, returns null.
     */
    public RowColumn dayToCell(YearMonthDay ymd) {
        RowColumn returnValue = null;
        int year = ymd.year;
        int month = ymd.month;
        int day = ymd.day;
        MonthYear m = new MonthYear(year, month);
        if (m.equals(_from)) {
            returnValue = new CalendarRowColumn(_month1FirstDay);
        } else if (m.equals(_to)) {
            returnValue = new CalendarRowColumn(_month2FirstDay);
        } else {
            System.out.println(ymd + " not in range " + getModel());
            return null;
        }
        returnValue.add(RowColumn.COLUMN, day - 1);
        return returnValue;
    }

    /**
     * Returns the Calendar corresponding to specified cell.
     * If cell is not in currently displayed range, returns null.
     */
    public YearMonthDay cellToDay(RowColumn r) {
        if (r == null)
            return null;
        if (r.lessThan(_month1FirstDay))
            return null;
        if (r.greaterThan(_month2LastDay))
            return null;
        int day;
        int month;
        int year;
        if (!r.greaterThan(_month1LastDay)) {
            day = 1 + r.difference(_month1FirstDay);
            month = _from.month;
            year = _from.year;
        } else {
            day = 1 + r.difference(_month2FirstDay);
            month = _to.month;
            year = _to.year;
        }
        return new YearMonthDay(year, month, day);
    }

    /**
     * Returns the day of month corresponding to specified cell.
     * If cell is not in currently displayed range, returns -1.
     */
    public int cellToDayNumber(RowColumn r) {
        if (r == null)
            return -1;
        if (r.lessThan(_month1FirstDay))
            return -1;
        if (r.greaterThan(_month2LastDay))
            return -1;
        int day;
        if (!r.greaterThan(_month1LastDay)) {
            day = 1 + r.difference(_month1FirstDay);
        } else {
            day = 1 + r.difference(_month2FirstDay);
        }
        return day;
    }

    /**
     * Returns true if cell is in a displayed month.
     */
    public boolean cellInRange(RowColumn r) {
        if (r == null)
            return false;
        if (r.lessThan(_month1FirstDay))
            return false;
        if (r.greaterThan(_month2LastDay))
            return false;
        return true;
    }

    /**
     * Returns the cell corresponding to specified x,y location.
     * Note, columns are 1-based.
     * If x,y not in currently displayed calendar area, returns null.
     * If x,y is on a line, it returns null.  I could assign the lines
     * to cells by some convention, but I didn't yet.
     */
    public RowColumn xyToCell(int x, int y) {
        if (x <= calendarLeft || x >= calendarRight)
            return null;
        if (y <= calendarTop || y >= calendarBottom)
            return null;

        // check for being on a line
        // don't worry about that, commented out
        //if ((x-calendarLeft) % _columnWidth == 0) return null;
        //if ((y-calendarTop) % _rowHeight == 0) return null;

        int column = (x - calendarLeft) / _columnWidth + 1;
        int row = (y - calendarTop) / _rowHeight;
        return new CalendarRowColumn(row, column);
    }

    /**
     * Returns the location of the top of the cell (on the line, if any).
     * Does not check that specified RowColumn is actually in
     * the displayed area.
     */
    public int cellTop(RowColumn r) {
        return r.row * _rowHeight + calendarTop + 1;
    }

    /**
     * Returns the location of the bottom of the cell (on the line, if any).
     * Does not check that specified RowColumn is actually in
     * the displayed area.
     */
    public int cellBottom(RowColumn r) {
        return (r.row + 1) * _rowHeight + calendarTop + 1;
    }

    /**
     * Returns the location of the left side of the cell (on the line, if any).
     * Does not check that specified RowColumn is actually in
     * the displayed area.
     */
    public int cellLeft(RowColumn r) {
        // remember columns are 1-based
        return (r.column - 1) * _columnWidth + calendarLeft;
    }

    /**
     * Returns the location of the right side of the cell (on the line, if any).
     * Does not check that specified RowColumn is actually in
     * the displayed area.
     */
    public int cellRight(RowColumn r) {
        return (r.column) * _columnWidth + calendarLeft;
    }

    /**
     * Returns the number of rows needed to display indicated month.
     * This is the number of rows that would be used if the
     * grid sizing mode is set to VARIABLE_SIZE.
     */
    public int rowsInMonth(MonthYear m) {
        int daysInFirstRow = DAYS_IN_WEEK - CalendarUtil.dayOfWeekOfFirstDayOfMonth(m.year, m.month) + 1;
        int daysInMonth = CalendarUtil.daysInMonth(m.year, m.month);
        int fullWeeks = (daysInMonth - daysInFirstRow) / DAYS_IN_WEEK;
        int daysInFinalWeek = daysInMonth - (DAYS_IN_WEEK * fullWeeks + daysInFirstRow);
        int rows = 1 + fullWeeks;
        if (daysInFinalWeek > 0)
            rows++;
        return rows;
    }

    /**
     * If client calls any set methods that change the appearance of the
     * calendar, the dirty flag gets set indicating to the paint routine
     * that it has to recalculate cached values.
     */
    protected boolean dirty() {
        return _dirty;
    }

    /**
     * If client calls any set methods that change the appearance of the
     * calendar, the dirty flag gets set indicating to the paint routine
     * that it has to recalculate cached values.
     */
    protected void markDirty() {
        _dirty = true;
    }

    // Only this class has authority to mark cached values clean.
    private void _markClean() {
        _dirty = false;
    }

    // Creates a calendar with proper time zone
    protected Calendar _newGregorianCalendarInstance(Date d) {
        Calendar c = new GregorianCalendar();
        c.setTimeZone(getTimeZone());
        c.setTime(d);
        return c;
    }

    // Creates a calendar with proper time zone
    protected Calendar _newGregorianCalendarInstance(int year, int month, int day) {
        Calendar c = new GregorianCalendar();
        c.setTimeZone(getTimeZone());
        c.clear();
        c.set(c.YEAR, year);
        c.set(c.MONTH, month);
        c.set(c.DAY_OF_MONTH, day);
        return c;
    }

    private class ResizeListener extends ComponentAdapter {

        public void componentResized(ComponentEvent e) {
            displayChanged();
        }
    }

    /**
     * This method implements the CalendarDataListener interface.
     * It is called when the data model changes.
     * Note that GCalCalendar does not pass through model events.
     * A client would have to subscribe directly with the model.
     */
    public void rangeChanged(CalendarDataEvent e) {
        _modelChanged();
    }

    /**
     * A ListSelectionListener that forwards ListSelectionEvents from
     * the ListSelectionModel to the calendar ListSelectionListeners.
     * The forwarded events only differ from the originals in that their
     * source is the calendar instead of the selectionModel itself.
     */
    private class ListSelectionHandler implements ListSelectionListener, Serializable {

        public void selectBegin(ListSelectionEvent e) {
        }

        /**
         * Invoked when selection changes.
         */
        public void valueChanged(ListSelectionEvent e) {
            fireSelectionValueChanged(e.getFirstIndex(), e.getLastIndex(), e.getValueIsAdjusting());
        }
    }

    /**
     * Add a listener to the calendar that's notified each time a change
     * to the selection occurs.  Listeners added directly to the CalendarMonth
     * will have their ListSelectionEvent.getSource() == this CalendarMonth
     * (instead of the ListSelectionModel).
     *
     * @param listener The CalendarSelectionListener to add.
     * @see #getSelectionModel
     */
    public void addListSelectionListener(ListSelectionListener listener) {
        if (_selectionListener == null) {
            _selectionListener = new ListSelectionHandler();
            getSelectionModel().addListSelectionListener(_selectionListener);
        }
        listenerList.add(ListSelectionListener.class, listener);
    }

    /**
     * Remove a listener from the list that's notified each time a
     * change to the selection occurs.
     *
     * @param listener The CalendarSelectionListener to remove.
     * @see #addListSelectionListener
     * @see #getSelectionModel
     */
    public void removeListSelectionListener(ListSelectionListener listener) {
        listenerList.remove(ListSelectionListener.class, listener);
    }

    /**
     * This method notifies CalendarMonth ListSelectionListeners that
     * the selection model has changed.  It's used to forward
     * ListSelectionEvents from the selectionModel to the
     * ListSelectionListeners added directly to the CalendarMonth.
     */
    protected void fireSelectionValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
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
     * Add a listener to the calendar that's notified each time a change
     * to the selected date occurs.
     *
     * @param listener The CalendarSelectionListener to add.
     */
    public void addCalendarSelectionListener(CalendarSelectionListener listener) {
        listenerList.add(CalendarSelectionListener.class, listener);
    }

    /**
     * Remove a listener from the list that's notified each time a
     * change to the selected date occurs.
     *
     * @param listener The CalendarSelectionListener to remove.
     * @see #addCalendarSelectionListener
     */
    public void removeCalendarSelectionListener(CalendarSelectionListener listener) {
        listenerList.remove(CalendarSelectionListener.class, listener);
    }

    /**
     * This method notifies CalendarMonth CalendarSelectionListeners that
     * a date was chosen.
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
     * Convenience method to set the selection to specified day.
     * @param index 0-based index of day with respect to start of model
     * time interval.
     */
    public void setDay(int index) {
        YearMonthDay ymd = dateFromIndex(index);
        setDate(ymd);
    }

    /**
     * Convenience method to set the selection to specified day.
     * @param date A Date that will be interpreted in the default time zone.
     */
    public void setDate(Date date) {
        Calendar c = _newGregorianCalendarInstance(date);
        setDate(c.get(c.YEAR), c.get(c.MONTH), c.get(c.DAY_OF_MONTH));
    }

    /**
     * Convenience method to set the selection to specified day.
     * @param year The year.
     * @param month The month.
     * @param day The day.
     */
    public void setDate(int year, int month, int day) {
        setDate(new YearMonthDay(year, month, day));
    }

    /**
     * Convenience method to set the selection to specified day.
     * @param c Calendar specifying the date.
     */
    public void setDate(YearMonthDay ymd) {
        int index = dayIndex(ymd);
        getSelectionModel().setSelectionInterval(index, index);
        if (getSelectionMode() == ListSelectionModel.SINGLE_SELECTION) {
            fireDateSelected(ymd);
        }
        _movingModelDate = ymd;
        repaint();
    }

    /**
     * Convenience method to set the selection to specified day.
     * @param c Calendar specifying the date.
     */
    private void _setDateNoNotify(YearMonthDay ymd) {
        int index = dayIndex(ymd);
        getSelectionModel().setSelectionInterval(index, index);
        repaint();
    }

    /**
     * Convenience method to get the selected day if any.
     */
    public YearMonthDay getDate() {
        int index = getSelectionModel().getAnchorSelectionIndex();
        if (getSelectionModel().isSelectedIndex(index)) {
            return dateFromIndex(index);
        } else {
            return null;
        }
    }

    public void mouseClicked(MouseEvent event) {
    }

    public void mouseReleased(MouseEvent event) {
    }

    public void mouseEntered(MouseEvent event) {
        if (!hasFocus())
            requestFocus();
    }

    public void mouseExited(MouseEvent event) {
    }

    public void mousePressed(MouseEvent event) {
        if (!hasFocus())
            requestFocus();
        RowColumn rowColumn = xyToCell(event.getX(), event.getY());
        if (!cellInRange(rowColumn))
            return;
        int index = dayIndex(rowColumn);
        if (event.isShiftDown() || event.isControlDown()) {
            getSelectionModel().setLeadSelectionIndex(index);
        } else {
            setDay(index);
        }
        repaint();
        fireActionEvent(ACTION_CLICK);
    }

    public void mouseMoved(MouseEvent event) {
    }

    public void mouseDragged(MouseEvent event) {
        RowColumn rowColumn = xyToCell(event.getX(), event.getY());
        if (!cellInRange(rowColumn))
            return;
        int index = dayIndex(cellToDay(rowColumn));
        getSelectionModel().setLeadSelectionIndex(index);
        repaint();
    }

    public void keyTyped(KeyEvent event) {
    }

    public void keyReleased(KeyEvent event) {
    }

    public void keyPressed(KeyEvent event) {
        int key = event.getKeyCode();
        if (key == KeyEvent.VK_ENTER) {
            fireActionEvent(ACTION_ENTER);
        }
        if (key == KeyEvent.VK_HOME) {
            moveToStart();
            repaint();
        }
        if (key == KeyEvent.VK_END) {
            moveToEnd();
            repaint();
        }
        if (key == KeyEvent.VK_PAGE_DOWN) {
            pageBackward();
        }
        if (key == KeyEvent.VK_PAGE_UP) {
            pageForward();
        }
        int anchor = getSelectionModel().getAnchorSelectionIndex();
        int lead = getSelectionModel().getLeadSelectionIndex();
        if (key == KeyEvent.VK_RIGHT) {
            if (event.isShiftDown() || event.isControlDown()) {
                if (lead + 1 < _numberOfDays) {
                    getSelectionModel().setLeadSelectionIndex(lead + 1);
                    repaint();
                }
            } else if (anchor + 1 < _numberOfDays) {
                setDay(anchor + 1);
                repaint();
            }
            /* else if (anchor + 1 == _numberOfDays) {
               if (group.isLastCalendarMonth(this)) {
                  group.nextMonth(true);
                  setLastDay();
                  repaint();
               } else {
                  CalendarMonth next = group.nextCalendarMonth();
                  next.setFirstDay();
                  next.repaint();
                  repaint();
               }
               }*/
        }
        if (key == KeyEvent.VK_LEFT) {
            if (event.isShiftDown() || event.isControlDown()) {
                if (lead > 0) {
                    getSelectionModel().setLeadSelectionIndex(lead - 1);
                    repaint();
                }
            } else if (anchor > 0) {
                setDay(anchor - 1);
                repaint();
            }
            /* else if (anchor == 1) {
               if (group.isFirstCalendarMonth(this)) {
                  group.prevMonth(true);
                  setFirstDay();
                  repaint();
               } else {
                  CalendarMonth prev = group.prevCalendarMonth();
                  prev.setLastDay();
                  prev.repaint();
                  repaint();
               }
               } */
        }
        if (key == KeyEvent.VK_UP) {
            if (event.isShiftDown() || event.isControlDown()) {
                if (lead > 6) {
                    getSelectionModel().setLeadSelectionIndex(lead - 7);
                    repaint();
                }
            } else if (anchor > 6) {
                setDay(anchor - 7);
                repaint();
            }
        }
        if (key == KeyEvent.VK_DOWN) {
            if (event.isShiftDown() || event.isControlDown()) {
                if (lead < (_numberOfDays - 7)) {
                    getSelectionModel().setLeadSelectionIndex(lead + 7);
                    repaint();
                }
            } else if (anchor < (_numberOfDays - 7)) {
                setDay(anchor + 7);
                repaint();
            }
        }
    }

    /**
     * This method notifies CalendarMonth ActionListeners that an action
     * has taken place.  (selection, scrolling, ...)
     */
    public void fireActionEvent(String command) {
        // This EventListenerList is maintained in parent JComponent
        Object[] listeners = listenerList.getListenerList();
        ActionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                if (e == null) {
                    e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);
                }
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    /**
     * Add a listener to the list that's notified each time an action is performed.
     * to the selection occurs.  Listeners added directly to the CalendarMonth
     * will have their ActionEvent.getSource() == this CalendarMonth
     * (instead of the ActionModel).
     *
     * @param listener The ActionListener to add.
     */
    public void addActionListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }

    /**
     * Remove a listener from the list that's notified each time an
     * action is performed.
     *
     * @param listener The ActionListener to remove.
     * @see #addActionListener
     */
    public void removeActionListener(ActionListener listener) {
        listenerList.remove(ActionListener.class, listener);
    }

    /**
     * This method notifies CalendarMonth ActionListeners that the
     * display has changed.  (scrolling to new month, ...)
     * Listener can query the source for information.
     */
    public void fireCalendarDisplayEvent() {
        // This EventListenerList is maintained in parent JComponent
        Object[] listeners = listenerList.getListenerList();
        CalendarDisplayEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CalendarDisplayListener.class) {
                if (e == null) {
                    e = new CalendarDisplayEvent(this);
                }
                ((CalendarDisplayListener) listeners[i + 1]).displayChanged(e);
            }
        }
    }

    /**
     * Add a listener to the list that's notified each time the display changes.
     * Inspired by the weird collection of events fired by JTree.
     *
     * @param listener The CalendarDisplayListener to add.
     */
    public void addCalendarDisplayListener(CalendarDisplayListener listener) {
        listenerList.add(CalendarDisplayListener.class, listener);
    }

    /**
     * Remove a listener from the list that's notified each time display changes.
     *
     * @param listener The CalendarDisplayListener to remove.
     * @see #addCalendarDisplayListener
     */
    public void removeCalendarDisplayListener(CalendarDisplayListener listener) {
        listenerList.remove(CalendarDisplayListener.class, listener);
    }

    /*
    // This class handles mouse events on behalf of the GCalCalendar.
    private class MouseWatcher extends MouseAdapter
    {
    public void
    mouseClicked(MouseEvent e)
    {
      // Determine the cell if user clicked on a cell in displayed range.
      RowColumn r = xyToCell(e.getX(), e.getY());
      if (r == null) return;  // click was out of range

      // Calculate date corresponding to cell clicked in.
      Calendar selectedDate = cellToDay(r);  // the date chosen
      if (selectedDate == null) return;

      // See if we selected an existing range zone
      int zone = _yToZone(e.getY());
      if (getNumberOfRangeCollections() > 0 && zone != NO_RANGE_ZONE)
      {
        // user clicked in a range zone, see if there is a range there
        CalendarRange range = getRangeCollection(zone).getRange(selectedDate);
        if (range != null)
        {
          // there was a range here, select it and return
          if ((e.getModifiers() & MouseEvent.CTRL_MASK) == 0)
          {
            // No CTRL key
            _selectedRanges.clear();  // this selection replaces any others
          }
          _SelectedRange selectedRange = new _SelectedRange(range, zone);
          _selectedRanges.add(selectedRange);
          repaint();
          fireRangeCollectionRangeSelected(range, zone);
          return;
        }
      }

      // User did not click on an existing time range.

      if (_selectionMode == RANGE_SELECT_START)
      {
        // we are looking for the start of a new range
        // fire event to alert listeners we are about to select start of a range
        if (!fireRangeCollectionSelectBegin(
            selectedDate, getActiveRangeCollectionNumber()).getAllowSelection())
        {
          return; // A listener vetoed the selection
        }
        _rangeSelectionStart = selectedDate;
        _selectionMode = RANGE_SELECT_END;  // start looking for end of range
        _clearSelectedRanges();
        repaint();
      }
      else if (_selectionMode == RANGE_SELECT_END)
      {
        // we are looking for the end of a new range
        // fire event to alert listeners we are about to select end of a range
        if (!fireRangeCollectionSelectEnd(
            selectedDate, getActiveRangeCollectionNumber()).getAllowSelection())
        {
          return; // A listener vetoed the selection
        }
        RangeCollection collection = getActiveRangeCollection();
        if (collection == null) System.out.println("null collection");
        collection.add(new CalendarRange(_rangeSelectionStart, selectedDate));
        _selectionMode = RANGE_SELECT_START;  // start looking for start of range
        _rangeSelectionStart = null;
        _clearSelectedRanges();
        repaint();
      }
      else if (_selectionMode == RANGE_SELECT_NONE)
      {
      }
    }
    }
    */

    /**
     * Mouse input, and focus handling for GCalCalendar.  An instance of this
     * class is added to the appropriate java.awt.Component lists
     * at installUI() time.  Note keyboard input is handled with JComponent
     * KeyboardActions, see installKeyboardActions().
     * <p>
     */
    /*public class MouseInputHandler implements MouseInputListener
   {
       public void mouseClicked(MouseEvent e) {}

       public void mouseEntered(MouseEvent e) {}

       public void mouseExited(MouseEvent e) {}

       public void mousePressed(MouseEvent e)
       {
           if (!SwingUtilities.isLeftMouseButton(e)) {
               return;
           }

           if (!this.isEnabled()) {
               return;
           }

   // Request focus before updating the list selection.  This implies
   // that the current focus owner will see a focusLost() event
   // before the lists selection is updated IF requestFocus() is
   //
    synchronous (it is on Windows).  See bug 4122345
            //
           if (!this.hasFocus()) {
               this.requestFocus();
           }

           RowColumn r = xyToCell(e.getX(), e.getY());
           this.setValueIsAdjusting(true);

   //xxxx


           int row = convertYToRow(e.getY());
           if (row != -1) {
               this.setValueIsAdjusting(true);
               int anchorIndex = this.getAnchorSelectionIndex();
               if (e.isControlDown()) {
                   if (this.isSelectedIndex(row)) {
                       this.removeSelectionInterval(row, row);
                   }
                   else {
                       this.addSelectionInterval(row, row);
                   }
               }
               else if (e.isShiftDown() && (anchorIndex != -1)) {
                   this.setSelectionInterval(anchorIndex, row);
               }
               else {
                   this.setSelectionInterval(row, row);
               }
           }
       }

       public void mouseDragged(MouseEvent e) {
           if (!SwingUtilities.isLeftMouseButton(e)) {
               return;
           }

           if (!this.isEnabled()) {
               return;
           }

           if (e.isShiftDown() || e.isControlDown()) {
               return;
           }

           int row = convertYToRow(e.getY());
           if (row != -1) {
               Rectangle cellBounds = getCellBounds(this, row, row);
               if (cellBounds != null) {
                   this.scrollRectToVisible(cellBounds);
                   this.setSelectionInterval(row, row);
               }
           }
       }

       public void mouseMoved(MouseEvent e) {
       }

       public void mouseReleased(MouseEvent e) {
           if (!SwingUtilities.isLeftMouseButton(e)) {
               return;
           }

           this.setValueIsAdjusting(false);
       }
   }*/

    public void focusGained(FocusEvent event) {
        hasFocus = true;
        repaint();
    }

    public void focusLost(FocusEvent event) {
        hasFocus = false;
        repaint();
    }

    public boolean isFocusable() {
        return true;
    }

    /**
     * Test driver for the CalendarMonth component.
     */
    public static void main(String args[]) {
        JFrame frame = new JFrame();
        frame.setTitle("CalendarMonth Test Harness");
        frame.setBounds(50, 100, 350, 400);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        CalendarMonth cm = new CalendarMonth();
        cm.setDisplayMode(cm.MULTI_MONTH_MODE);
        //DefaultCalendarCellRenderer r =
        //   (DefaultCalendarCellRenderer)cm.getCellRenderer();
        //r.setBackground(r.DEFAULT_BACKGROUND1);
        //r.setBackground2(r.DEFAULT_MULTI_MONTH_BACKGROUND2);
        //r.setBackdrop(new Color(0xcc9999));

        frame.getContentPane().add("Center", cm);

        //frame.pack();
        frame.setVisible(true);
    }

}

