package edu.gemini.shared.gui.calendar;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Set;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JFrame;
import javax.swing.ListSelectionModel;
import edu.gemini.shared.util.DiscreteRange;
import edu.gemini.shared.util.DiscreteRangeModel;
import edu.gemini.shared.util.DefaultDiscreteRangeModel;
import edu.gemini.shared.util.DiscreteRangeModelEvent;
import edu.gemini.shared.util.DiscreteRangeModelListener;

/**
 * A CalendarMonth allows the selection of date ranges and keeps them
 * all in a single collection of ranges, analogous to a JList and
 * ListSelectionModel having a single collection.
 * This subclass of CalendarMonth adds the concept of multiple independent
 * collections of ranges.  Each range collection is given its own horizontal
 * slice of the calendar cells.  Mouse clicks are checked to see which
 * slice or "zone" it falls in.  CellRenderers for RangeCalendar must
 * also be familiar with this zone concept.
 */

// Note - inheriting from JPanel will not clear the inside of the component
// on resize.  JLabel is convenient for this purpose.  There is no other
// reason to inherit from JLabel.

public class RangeCalendar extends CalendarMonth {

    /** Number of days in a week. */
    public static final int DAYS_IN_WEEK = 7;

    // Constant to keep track of state of selection
    private static final int RANGE_SELECT_NONE = 0;

    private static final int RANGE_SELECT_START = 1;

    private static final int RANGE_SELECT_END = 2;

    private static final int NO_ACTIVE_RANGE = -1;

    private static final int NO_RANGE_ZONE = -1;

    // These constants determine the size of arrows on the ends of ranges.
    private static final float ARROW_WIDTH_FRACTION = (float) 0.15;

    private static final float ARROW_HEIGHT_FRACTION = (float) 0.10;

    private int _rangeCount = 0;

    private int _activeRangeCollectionIndex = NO_ACTIVE_RANGE;

    private DiscreteRangeModel[] _discreteRangeModels = null;

    // A parallel set of models stores ranges selected by the user for deletion.
    private DiscreteRangeModel[] _discreteRangeSelectionModels = null;

    // A quick way to check whether there are any selections at all.
    private boolean _rangeSelectionEmpty = true;

    // Remember that JComponent has this protected member:
    // protected EventListenerList listenerList = new EventListenerList();

    // This determines where ranges are displayed in a cell as a fraction
    // of cell height from top of cell.
    private static final double RANGE_ZONE_HEIGHT_FRACTION = 0.45;

    private double _rangeZoneHeightFraction = RANGE_ZONE_HEIGHT_FRACTION;

    private int[] _rangeZoneBoundaries;

    private RangeInfo _NEGATIVE_RANGE_QUERY;

    private RangeInfo _BOTH_ENDS_RANGE_QUERY;

    private RangeInfo _LEFT_END_RANGE_QUERY;

    private RangeInfo _RIGHT_END_RANGE_QUERY;

    private RangeInfo _INTERIOR_RANGE_QUERY;

    private boolean _anchorSelected = false;

    /**
     * This class is used to return the results of a query on the relationships
     * of dates and ranges.
     * A primary use is for cell renderers to get the complete story on
     * a cell date's relationship to the range collection in a single query
     * rather than many queries.
     */
    public class RangeInfo {

        private boolean _leftEnd;

        private boolean _rightEnd;

        private boolean _inRange;

        /**
         * Constructs a RangeInfo.
         * @param inRange true if subject is in range,
         * it will automatically be set to true if either leftEnd
         * or rightEnd are true
         * @param leftEnd true if subject is a left endpoint in range
         * @param rightEnd true if subject is a right endpoint in range
         */
        public RangeInfo(boolean inRange, boolean leftEnd, boolean rightEnd) {
            _inRange = (inRange || leftEnd || rightEnd);
            _leftEnd = leftEnd;
            _rightEnd = rightEnd;
        }

        /**
         * Returns true if subject was in range.  Note that being a left or
         * right endpoint implies being in range.
         */
        boolean inRange() {
            return _inRange;
        }

        /**
         * Returns true if subject was the left end of a range.
         * Note that being a left or right endpoint implies being in range.
         */
        boolean isLeftEnd() {
            return _leftEnd;
        }

        /**
         * Returns true if subject was the right end of a range.
         * Note that being a left or right endpoint implies being in range.
         */
        boolean isRightEnd() {
            return _rightEnd;
        }

        public String toString() {
            return "RangeInfo - inRange " + inRange() + " left: " + isLeftEnd() + " right end: " + isRightEnd();
        }
    }

    // Since RangeCalendar does not pass through DiscreteRangeEvents,
    // there is currently no need for this relay object.
    private class RangeListener implements DiscreteRangeModelListener {

        private int _range;

        RangeListener(int range) {
            _range = range;
        }

        /**
         * Invoked before an attempt to add a range to a collection.
         * This gives a client a chance to validate the range
         * and possibly call setAllowOperation() to deny it.
         */
        public void addBegin(DiscreteRangeModelEvent e) {
            // Do nothing
            System.out.println("addBegin " + _range + "  " + e);
        }

        /**
         * Invoked after a DiscreteRangeCollection has changed.
         * This event could represent either addition or deletion.
         * Note that a single add operation may result in several deletion
         * events followed by a single add event due to range merging.
         */
        public void modelChanged(DiscreteRangeModelEvent e) {
            System.out.println("collectionChanged " + _range + "  " + e);
        }
    }

    /**
     * Constructs a CalendarMonth with the default model, selection model,
     * and cell renderer.
     * Selection model defaults to SINGLE_SELECTION.
     */
    public RangeCalendar() {
        this(1);
    }

    public RangeCalendar(int rangeCount) {
        super();
        _initRanges(rangeCount);
    }

    /**
     * Constructs a RangeCalendar with the default model spanning single month
     * containing date, the specified selection model and cell renderer.
     * Selection model defaults to SINGLE_SELECTION.
     */
    public RangeCalendar(Date date, int rangeCount) {
        super(date);
        _initRanges(rangeCount);
    }

    /**
     * Constructs a CalendarMonth with the specified model.
     */
    public RangeCalendar(CalendarModel model, int rangeCount) {
        super(model);
        _initRanges(rangeCount);
    }

    /**
     * Constructs CalendarMonth.
     */
    public RangeCalendar(CalendarModel model, ListSelectionModel selector, CalendarCellRenderer renderer, int rangeCount) {
        super(model, selector, renderer);
        _initRanges(rangeCount);
    }

    private void _initRanges(int rangeCount) {
        //CalendarCellRenderer renderer = getCellRenderer();
        DefaultRangeCellRenderer rangeRenderer = new DefaultRangeCellRenderer(rangeCount);
        setCellRenderer(rangeRenderer);

        // We won't render the ordinay selected dates in the usual way.
        // Need the single interval mode.
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        _rangeCount = rangeCount;
        _rangeZoneBoundaries = new int[rangeCount + 1];
        _discreteRangeModels = new DiscreteRangeModel[rangeCount];
        _discreteRangeSelectionModels = new DiscreteRangeModel[rangeCount];
        for (int i = 0; i < rangeCount; ++i) {
            // This holds the date ranges.
            setRangeModel(i, new DefaultDiscreteRangeModel());

            // There are two models for every range, one to hold the ranges,
            // the other to hold the ranges that are selected.
            // This model holding selected ranges is an implementation detail
            // and needs no listeners or client access.
            // It is simply supporting delete capability.
            _discreteRangeSelectionModels[i] = new DefaultDiscreteRangeModel();
        }

        // Ranges are queried very often by cell renderers.
        // We don't want to create millions of RangeInfo objects, so reuse these.
        _NEGATIVE_RANGE_QUERY = new RangeInfo(false, false, false);
        _BOTH_ENDS_RANGE_QUERY = new RangeInfo(true, true, true);
        _LEFT_END_RANGE_QUERY = new RangeInfo(true, true, false);
        _RIGHT_END_RANGE_QUERY = new RangeInfo(true, false, true);
        _INTERIOR_RANGE_QUERY = new RangeInfo(true, false, false);
        if (getRangeCount() > 0)
            setActiveRangeIndex(0);
    }

    /**
     * Gets the active range collection.
     */
    public int getActiveRangeIndex() {
        return _activeRangeCollectionIndex;
    }

    /**
     * Sets the active range collection.
     * @param i the index of the range collection to become active.
     */
    public void setActiveRangeIndex(int i) {
        if (i >= 0 && i < getRangeCount()) {
            _activeRangeCollectionIndex = i;
            getSelectionModel().clearSelection();
            _anchorSelected = false;
        }
    }

    /**
     * Gets the number of range collections.
     */
    public int getRangeCount() {
        return _rangeCount;
    }

    /**
     * Gets the top range zone height as a fraction of cell height.
     */
    public double getRangeZoneHeightFraction() {
        return _rangeZoneHeightFraction;
    }

    /**
     * This method sets the spacing of the range zones in the cells.
     * Sets the height of the center of the first range zone as a fraction of
     * cell height.
     * In the following diagram, it would be setting the position of the
     * midpoint between bndry 0 and bndry1 relative to the top of the cell.
     * (Remember that the y axis increases downward).
     * <br>
     * <pre>
     * +-----------+              e.g.  bndry 2 < y            -> no zone
     * |           |                    bndry 1 < y <= bndry 2 -> zone 1
     * |- - - - - -| bndry 0            bndry 0 < y <= bndry 1 -> zone 0
     * |  zone 0   |                              y < bndry  0 -> no zone
     * |- - - - - -| bndry 1
     * |  zone 1   |
     * |- - - - - -| bndry 2
     * +-----------+
     * </pre>
     */
    public void setRangeZoneHeightFraction(double fraction) {
        _rangeZoneHeightFraction = fraction;
    }

    /**
     * Gets the range data model for specified range index.
     */
    public DiscreteRangeModel getRangeModel(int i) {
        if (i < 0 || i >= getRangeCount())
            return null;
        return _discreteRangeModels[i];
    }

    /**
     * Sets the range data model for this calendar.  Note that the number of
     * ranges is set during construction.
     * @param i The range index, 0 <= i < getRangeCount()
     * @param model The DiscreteRangeModel
     */
    public void setRangeModel(int i, DiscreteRangeModel model) {
        /*   if (_discreteRangeModels[i] != null &&
            _discreteRangeModels[i] instanceof DiscreteRangeModel) {
           _discreteRangeModels[i].removeDiscreteRangeListener());
           }*/
        _discreteRangeModels[i] = model;
        /*if (model != null && model instanceof DiscreteRangeModel) {
           _discreteRangeModels[i].addDiscreteRangeListener(new RangeListener(i));
           }*/
        repaint();
    }

    /**
     * Gets the range data model for specified range index.
     */
    public DiscreteRangeModel getRangeSelectionModel(int i) {
        if (i < 0 || i >= getRangeCount())
            return null;
        return _discreteRangeSelectionModels[i];
    }

    /**
     * Returns whether all range selection models are empty.
     */
    public boolean isRangeSelectionModelEmpty() {
        return _rangeSelectionEmpty;
    }

    // Adds a selected range to the specified model.
    private void _addSelectedRange(int range, DiscreteRange r) {
        getRangeSelectionModel(range).add(r);
        _rangeSelectionEmpty = false;
    }

    /**
     * Returns information on whether the specified date is contained in
     * a range of the specified index.  The RangeInfo object returned indicates
     * whether the date is contained and whether the date is the left or
     * right end of the range.
     * This is intended for cell renderers to get quick information on how
     * to draw the range in a cell.
     */
    public RangeInfo rangeQuery(int range, YearMonthDay ymd) {
        DiscreteRange r = getRangeModel(range).findContainingRange(ymd);
        if (r != null) {
            boolean left = ymd.equals(r.getStart());
            boolean right = ymd.equals(r.getEnd());
            //      boolean left = CalendarUtil.equals((Calendar)(r.getStart()), d);
            //boolean right = CalendarUtil.equals((Calendar)(r.getEnd()), d);
            if (left && right)
                return _BOTH_ENDS_RANGE_QUERY;
            else if (left)
                return _LEFT_END_RANGE_QUERY;
            else if (right)
                return _RIGHT_END_RANGE_QUERY;
            else
                return _INTERIOR_RANGE_QUERY;
        } else {
            return _NEGATIVE_RANGE_QUERY;
        }
    }

    /**
     * Returns information on whether the specified date is contained in
     * a selected range of the specified index.
     * This is intended for cell renderers to get quick information on how
     * to draw the range in a cell.  This method returns a boolean instead of
     * a RangeInfo because the client can call rangeQuery() for more detailed info.
     *
     */
    public boolean selectedRangeQuery(int range, Calendar d) {
        if (isRangeSelectionModelEmpty())
            return false;
        return getRangeSelectionModel(range).contains(d);
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
        return (_anchorSelected && getSelectionModel().getAnchorSelectionIndex() == index);
    }

    /** Sets the model to a span containing currently displayed months,
     erases any selections. */
    protected void setMovingModel() {
        super.setMovingModel();
        clearRangeModels();
    }

    /**
     * Clears the range models and selection range models of all entries.
     */
    public void clearRangeModels() {
        for (int i = 0; i < getRangeCount(); ++i) {
            getRangeModel(i).clear();
            clearRangeSelections();
        }
        repaint();
    }

    /**
     * Clears the range selections (does not delete)
     */
    protected void clearRangeSelections() {
        for (int i = 0; i < getRangeCount(); ++i) {
            getRangeSelectionModel(i).clear();
        }
        _rangeSelectionEmpty = true;
    }

    /**
     * Deletes any selected ranges in any range collection index.
     */
    public void removeRangeSelections() {
        for (int i = 0; i < getRangeCount(); ++i) {
            DiscreteRangeModel model = getRangeModel(i);
            DiscreteRangeModel selectionModel = getRangeSelectionModel(i);
            Set ranges = selectionModel.getDiscreteRanges();
            for (Iterator itr = ranges.iterator(); itr.hasNext();) {
                DiscreteRange r = (DiscreteRange) itr.next();
                model.remove(r);
            }
        }
        clearRangeSelections();
        repaint();
    }

    /**
     * This method calculates values needed to paint the calendar.
     * Since they may be expensive to calculate they are cached each
     * time the size is changed.
     */
    protected void cachePaintValues(Graphics g, Dimension d) {
        super.cachePaintValues(g, d);
        int fontSize = 0;
        FontMetrics fm;

        // set zone spacing

        if (getRangeCount() > 0) {
            int range0 = (int) (cellHeight() * getRangeZoneHeightFraction());
            // calculate twice the spacing between zones
            double spacing = (cellHeight() - range0) / (getRangeCount() * 2.0);
            _rangeZoneBoundaries[0] = (int) (range0 - spacing / 2.0);
            for (int i = 1; i <= getRangeCount(); ++i) {
                _rangeZoneBoundaries[i] = (int) (_rangeZoneBoundaries[i - 1] + 2 * spacing);
            }
        }
        getCellRenderer().cachePaintValues(g, this);
    }

    /**
     * This method is invoked by Swing to draw components.
     */
    /*public void
    paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Dimension d = this.getSize();
        if (dirty()) cachePaintValues(g, d);

        }*/

    /*protected void
       paintCell(Graphics g, Component component, CalendarMonth cm,
                 int x, int y, int w, int h)
    {
       super.paintCell(g, component, cm, x, y, w, h);
       System.out.println("Painting in RangeCalendar"); // ***STUB***
    }*/

    /**
     * Returns the range number associated with the given pixel height in the cell.
     * This is the pixel heights of range zone boundaries relative
     * to the bottom of a cell.  Example:
     * <pre>
     * +-----------+              e.g.  bndry 2 < y            -> no zone
     * |           |                    bndry 1 < y <= bndry 2 -> zone 1
     * |- - - - - -| bndry 0            bndry 0 < y <= bndry 1 -> zone 0
     * |  zone 0   |                              y < bndry  0 -> no zone
     * |- - - - - -| bndry 1
     * |  zone 1   |
     * |- - - - - -| bndry 2
     * +-----------+
     * </pre>
     */

    /**
     * Returns the specified range zone boundary height in pixels from cell top.
     * Returns NO_RANGE_ZONE if range is out of range.
     */
    public int rangeZoneBoundary(int range, boolean upper) {
        if (range < 0 || range >= getRangeCount())
            return NO_RANGE_ZONE;
        if (upper) {
            return _rangeZoneBoundaries[range];
        } else {
            return _rangeZoneBoundaries[range + 1];
        }
    }

    /**
     * Returns the range number corresponding to the given pixel height
     * (relative to top of cell).
     * @param height Pixel height relative to bottom of cell.
     */
    public int heightToRangeNumber(int height) {
        if (getRangeCount() == 0)
            return NO_RANGE_ZONE;
        if (height < _rangeZoneBoundaries[0] || height > _rangeZoneBoundaries[getRangeCount()])
            return NO_RANGE_ZONE;
        for (int i = getRangeCount() - 1; i >= 0; --i) {
            if (height > _rangeZoneBoundaries[i])
                return i;
        }
        return NO_RANGE_ZONE;
    }

    /**
     * Returns the range zone corresponding to the y pixel height.
     * Returns NO_RANGE_ZONE if not in a range zone.
     */
    public int yToZone(int y) {
        if (y <= calendarTop || y >= calendarBottom)
            return NO_RANGE_ZONE;
        int row = (y - calendarTop) / cellHeight();
        return heightToRangeNumber((y - calendarTop) % cellHeight());
    }


    //******************************************************


    public void mousePressed(MouseEvent event) {
        if (!hasFocus())
            requestFocus();
        RowColumn rowColumn = xyToCell(event.getX(), event.getY());
        if (!cellInRange(rowColumn))
            return;
        int index = dayIndex(rowColumn);
        int zone = yToZone(event.getY());
        YearMonthDay selectedYMD = null;
        DiscreteRange selectedRange = null;
        if (zone != NO_RANGE_ZONE) {
            selectedYMD = dateFromIndex(index);
            /*      Calendar c = _newGregorianCalendarInstance(selectedYMD.year,
                                                       selectedYMD.month,
                                                       selectedYMD.day);*/
            selectedRange = getRangeModel(zone).findContainingRange(selectedYMD);
        }
        if (event.isShiftDown() || event.isControlDown()) {
            if (selectedRange == null) {
                if (_anchorSelected) {
                    // Extend current selection.
                    _addRange(index);
                } else {
                    // Treat this like an unmodified click
                    getSelectionModel().setSelectionInterval(index, index);
                    _anchorSelected = true;
                }
                clearRangeSelections();
            } else {
                // Selected an existing range.  Add to selected range collection.
                clearSelection();
                _anchorSelected = false;
                _addSelectedRange(zone, selectedRange);
            }
        } else {
            if (selectedRange == null) {
                if (_anchorSelected) {
                    // This is second click making a range.
                    _addRange(index);
                } else {
                    // This is first click defining a range.
                    getSelectionModel().setSelectionInterval(index, index);
                    _anchorSelected = true;
                }
                clearRangeSelections();
            } else {
                // Selected an existing range.  Add to selected range collection.
                clearSelection();
                clearRangeSelections();
                _addSelectedRange(zone, selectedRange);
                _anchorSelected = false;
            }
        }
        repaint();
        fireActionEvent(ACTION_CLICK);
    }

    // This processes a second click making a range.
    // Pass in the index of the new lead.
    // It is assumed there is an anchor already.
    private void _addRange(int index) {
        clearRangeSelections();
        getSelectionModel().setLeadSelectionIndex(index);
        YearMonthDay anchor = dateFromIndex(getSelectionModel().getAnchorSelectionIndex());
        YearMonthDay lead = dateFromIndex(getSelectionModel().getLeadSelectionIndex());
        YearMonthDayRange range = new YearMonthDayRange(anchor, lead);
        getRangeModel(getActiveRangeIndex()).add(range);
        _anchorSelected = false;
    }


    /*public void mouseDragged(MouseEvent event)
    {
       RowColumn rowColumn = xyToCell(event.getX(), event.getY());
       if (!cellInRange(rowColumn)) return;
       Calendar c = cellToDay(rowColumn);
       int index = dayIndex(cellToDay(rowColumn));
       getSelectionModel().setLeadSelectionIndex(index);
       repaint();
       }*/

    public void keyPressed(KeyEvent event) {
        int key = event.getKeyCode();
        if (key == KeyEvent.VK_DELETE) {
            removeRangeSelections();
            repaint();
        } else if (key == KeyEvent.VK_ESCAPE) {
            clearSelection();
            clearRangeSelections();
            _anchorSelected = false;
            repaint();
        } else {
            super.keyPressed(event);
        }
    }

    /*public void keyPressed(KeyEvent event)
    {
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
          for (int i = 0; i < PAGE_JUMP; ++i) {
             nextMonth();
          }
       }
       if (key == KeyEvent.VK_PAGE_UP) {
          for (int i = 0; i < PAGE_JUMP; ++i) {
             previousMonth();
          }
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
          }*/
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
    /* }
       if (key == KeyEvent.VK_LEFT) {
          if (event.isShiftDown() || event.isControlDown()) {
             if (lead > 0) {
                getSelectionModel().setLeadSelectionIndex(lead - 1);
                repaint();
             }
          } else if (anchor > 0) {
             setDay(anchor - 1);
             repaint();
             } */
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
    /* }
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
       }*/

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

    /**
     * Test driver for the CalendarMonth component.
     */
    public static void main(String args[]) {
        JFrame frame = new JFrame();
        frame.setTitle("RangeCalendar Test Harness");
        frame.setBounds(50, 100, 350, 400);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        RangeCalendar cm = new RangeCalendar(3);
        cm.setDisplayMode(cm.MULTI_MONTH_MODE);
        frame.getContentPane().add("Center", cm);

        //frame.pack();
        frame.setVisible(true);
    }

}

