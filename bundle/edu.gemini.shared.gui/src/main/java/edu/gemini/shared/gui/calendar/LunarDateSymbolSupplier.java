// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: LunarDateSymbolSupplier.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.gui.calendar;

import edu.gemini.shared.util.CalendarUtil;
import edu.gemini.skycalc.MoonCalc;

import javax.swing.Icon;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 * This class supplies icons for drawing lunar phase symbols.
 * It actually implements Icon itself, but that is an implementation detail.
 */
public class LunarDateSymbolSupplier implements DateSymbolSupplier, Icon {

    /** Constant indicating a full moon event. */
    public static final int FULL_MOON = 0;

    /** Constant indicating a first quarter moon event. */
    public static final int FIRST_QUARTER_MOON = 1;

    /** Constant indicating a new moon event. */
    public static final int NEW_MOON = 2;

    /** Constant indicating a last quarter moon event. */
    public static final int LAST_QUARTER_MOON = 3;

    private CalendarModel _calendarModel;

    private int _width = 0, _height = 0, _dateSymbolDimension = 0;

    private int _phase = FULL_MOON;

    private Color _color;

    private DateSymbolIndex _index;

    // This class encapsulates a symbol and the day it corresponds to.
    private class DateSymbol implements Comparable {

        private int _symbol;

        private YearMonthDay _date;

        /**
         * Constructor.
         * @param symbol - number for the symbol, client must know the context
         */
        public DateSymbol(int symbol, int year, int month, int day) {
            _symbol = symbol;
            _date = new YearMonthDay(year, month, day);
        }

        /**
         * Copy Constructor.
         * @param dateSymbol - copy the contents of this object
         */
        public DateSymbol(DateSymbol dateSymbol) {
            _symbol = dateSymbol.getSymbol();
            _date = dateSymbol.getDate();
        }

        /** Gets the symbol number. */
        public int getSymbol() {
            return _symbol;
        }

        /** Sets the symbol number. */
        public void setSymbol(int symbol) {
            _symbol = symbol;
        }

        /** Gets the symbol's date. */
        public YearMonthDay getDate() {
            return _date;
        }

        /** Sets the symbol's date. */
        public void setDate(YearMonthDay date) {
            _date = (YearMonthDay) date.clone();
        }

        /**
         * This method is needed for sorting in containers.
         */
        public int compareTo(Object o) {
            if (!(o instanceof DateSymbol))
                return 0;
            DateSymbol d = (DateSymbol) o;
            return getDate().compareTo(d.getDate());
        }
    }

    // This index serves the following request -
    //   Given a date (year, month, day) what icon if any corresponds?
    // This index will trade space for speed.  It is done quick and dirty
    // to get this thing working.
    private class DateSymbolIndex {

        int _startYear;

        int _yearCount;

        List _years;  // each entry is a List of Months

        public void setRange(int startYear, int startMonth, int endYear, int endMonth) {
            _startYear = startYear;
            _yearCount = endYear - startYear + 1;
        }

        public void createIndex(Set dateSymbols) {
            // create the index and fill it with null pointers
            _years = new ArrayList();
            for (int year = 0; year < _yearCount; ++year) {
                List months = new ArrayList();
                for (int m = 0; m < 12; ++m) {
                    List days = new ArrayList();
                    for (int i = 0; i <= 31; ++i) {
                        days.add(null);
                    }
                    months.add(days);
                }
                _years.add(months);
            }

            // now walk through the dateSymbols Set and set index elements
            for (Iterator itr = dateSymbols.iterator(); itr.hasNext();) {
                DateSymbol ds = (DateSymbol) itr.next();
                YearMonthDay ymd = ds.getDate();
                List yearList = (List) _years.get(ymd.year - _startYear);
                List monthList = (List) yearList.get(ymd.month);
                monthList.set(ymd.day, ds);
            }
        }

        /**
         * Returns a DateSymbol for specified day or null if there is
         * no symbol on that day.
         */
        DateSymbol getDateSymbol(int year, int month, int day) {
            List months = (List) _years.get(year - _startYear);
            List days = (List) months.get(month);
            return (DateSymbol) days.get(day);
        }
    }

    /**
     * Constructs a default LunarDateSymbolSupplier DateSymbolSupplier that will
     * create DateSymbols for phases of the moon.
     */
    public LunarDateSymbolSupplier() {
        _calendarModel = new DefaultCalendarModel();
        _index = new DateSymbolIndex();
        _cacheSymbols();
    }

    /**
     * Constructs a LunarDateSymbolSupplier DateSymbolSupplier that will
     * create DateSymbols for phases of the moon.
     */
    public LunarDateSymbolSupplier(int startYear, int startMonth, int endYear, int endMonth) {
        _calendarModel = new DefaultCalendarModel(startYear, startMonth, endYear, endMonth);
        _index = new DateSymbolIndex();
        _cacheSymbols();
    }

    /*
     * Serves as a hint to the supplier so it can pre-calculate
     * date symbols if it needs to.
     * If client fails to call this then the supplier may just
     * return null from any call to getDateSymbolRenderer().
     */
    public void setRange(int startYear, int startMonth, int endYear, int endMonth) {
        _calendarModel.setRange(startYear, startMonth, endYear, endMonth);
        _cacheSymbols();
    }

    /**
     * This method is called by the calendar renderer once before a paint cycle
     * as a hint to cache values that apply to all cells of the calendar.
     * The calendar has already cached its information for the paint cycle.
     * If you are implementing this interface you can always just implement
     * this method with an empty body if you don't want to bother with it.
     * The call is purely for the benefit of the renderer to speed up rendering.
     * @param g - the Graphics context
     * @param width - the width of the icon
     * @param height - the height of the icon
     */
    public void cachePaintValues(Graphics g, int width, int height) {
        _width = width;
        _height = height;
        _dateSymbolDimension = Math.min(_width, _height);
    }

    /**
     * Returns an Icon object if specified day corresponds
     * to a date symbol.  Returns null otherwise.
     */
    public Icon getDateSymbolIcon(int year, int month, int day) {
        DateSymbol ds = _index.getDateSymbol(year, month, day);
        if (ds == null)
            return null;
        _phase = ds.getSymbol();
        return this;
    }

    // Calculates the symbols for the current time range and
    // caches them for fast retrieval.
    private void _cacheSymbols() {
        Calendar start = new GregorianCalendar();
        start.clear();
        start.set(Calendar.YEAR, _calendarModel.getStartYear());
        start.set(Calendar.MONTH, _calendarModel.getStartMonth());
        start.set(Calendar.DAY_OF_MONTH, 1);
        Calendar end = new GregorianCalendar();
        end.clear();
        end.set(Calendar.YEAR, _calendarModel.getEndYear());
        end.set(Calendar.MONTH, _calendarModel.getEndMonth());
        end.set(Calendar.DAY_OF_MONTH, 1);  // first day of last month
        end.add(Calendar.MONTH, 1);         // first day of month after last
        end.add(Calendar.DAY_OF_MONTH, -1); // last day of last month
        List fullMoons = _getMoonSymbols(start, end, FULL_MOON);
        List firstQuarterMoons = _getMoonSymbols(start, end, FIRST_QUARTER_MOON);
        List newMoons = _getMoonSymbols(start, end, NEW_MOON);
        List lastQuarterMoons = _getMoonSymbols(start, end, LAST_QUARTER_MOON);

        // now collate these lists by adding them to a TreeSet that will
        // sort them (based on compareTo() method of DateSymbol.

        Set sortedDateSymbols = new TreeSet();
        for (Iterator itr = fullMoons.iterator(); itr.hasNext();) {
            DateSymbol d = (DateSymbol) itr.next();
            sortedDateSymbols.add(d);
        }
        for (Iterator itr = newMoons.iterator(); itr.hasNext();) {
            DateSymbol d = (DateSymbol) itr.next();
            sortedDateSymbols.add(d);
        }
        for (Iterator itr = firstQuarterMoons.iterator(); itr.hasNext();) {
            DateSymbol d = (DateSymbol) itr.next();
            sortedDateSymbols.add(d);
        }
        for (Iterator itr = lastQuarterMoons.iterator(); itr.hasNext();) {
            DateSymbol d = (DateSymbol) itr.next();
            sortedDateSymbols.add(d);
        }
        _index.setRange(_calendarModel.getStartYear(), _calendarModel.getStartMonth(), _calendarModel.getEndYear(), _calendarModel.getEndMonth());
        _index.createIndex(sortedDateSymbols);
    }

    /**
     * Returns a list of DateSymbols for a particular phase of the moon.
     * @param start Start calculating from this date.
     * @param start Stop calculating at this date.
     * @param type  The phase.
     * @return A list of DateSymbol objects for the specified lunar phase.
     */
    private List _getMoonSymbols(Calendar start, Calendar end, int type) {
        List dateSymbols = new ArrayList();
        Calendar c = (Calendar) start.clone();  // we will alter c
        // roll back a few moon phases before the start of the time range
        c.add(Calendar.MONTH, -2);  // two months should do it
        int period = MoonCalc.approximatePeriod(c.getTimeInMillis());
        while (!CalendarUtil.after(c, end)) {
            // Get the next time the moon will be in this phase.
            Date phase;
            final MoonCalc.Phase phase_constant;
            if (type == FULL_MOON) {
                phase_constant = MoonCalc.Phase.FULL;
            } else if (type == FIRST_QUARTER_MOON) {
                phase_constant = MoonCalc.Phase.FIRST_QUARTER;
            } else if (type == NEW_MOON) {
                phase_constant = MoonCalc.Phase.NEW;
            } else if (type == LAST_QUARTER_MOON) {
                phase_constant = MoonCalc.Phase.LAST_QUARTER;
            } else
                return dateSymbols;
            phase = new Date(MoonCalc.getMoonTime(period, phase_constant));
            period++;  // advance to next moon period
            c.setTime(phase);
            if (!CalendarUtil.before(c, start) && !CalendarUtil.after(c, end)) {
                //System.out.println("Phase peaks at: " + CalendarUtil.toString(c.getTime()));
                // this event is in range of the time span
                // truncate the hours, minutes, seconds.
                // ***there could be a smarter way to do it***
                Calendar roundedTime = CalendarUtil.newGregorianCalendarInstance(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                dateSymbols.add(new DateSymbol(type, roundedTime.get(Calendar.YEAR), roundedTime.get(Calendar.MONTH), roundedTime.get(Calendar.DAY_OF_MONTH)));
            }
            // The astro method will just return the same date unless we
            // push it forward a bit.
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dateSymbols;
    }

    // The following methods implement the Icon interface

    /**
     * Implements Icon interface
     */
    public int getIconWidth() {
        return _width;
    }

    /**
     * Implements Icon interface
     */
    public int getIconHeight() {
        return _height;
    }

    /**
     *Implements Icon interface
     * x,y defines upper left corner of the icon
     */
    public void paintIcon(Component c, Graphics g, int x_ulcorner, int y_ulcorner) {
        // A hack for now.  Figure out the right way to center it
        x_ulcorner -= 2;
        Graphics2D g2 = (Graphics2D) g;
        // Tell it to do a quality job drawing circles
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        // Draw an open circle that every moon symbol uses.
        g2.draw(new Ellipse2D.Double(x_ulcorner, y_ulcorner, _dateSymbolDimension, _dateSymbolDimension));
        if (_phase == NEW_MOON) {
            g2.fill(new Arc2D.Double(x_ulcorner, y_ulcorner, _dateSymbolDimension, _dateSymbolDimension, 0, 360, Arc2D.OPEN));
        } else if (_phase == LAST_QUARTER_MOON) {
            g2.fill(new Arc2D.Double(x_ulcorner, y_ulcorner, _dateSymbolDimension, _dateSymbolDimension, 270, 180, Arc2D.OPEN));
        } else if (_phase == FIRST_QUARTER_MOON) {
            g2.fill(new Arc2D.Double(x_ulcorner, y_ulcorner, _dateSymbolDimension, _dateSymbolDimension, 90, 180, Arc2D.OPEN));
        }
    }

    /**
     * Test driver for LundarDateSymbolSupplier using JCalendar
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
        DateSymbolCalendarModel model = new DateSymbolCalendarModel(1999, 0, 1999, 6);
        LunarDateSymbolSupplier l = new LunarDateSymbolSupplier();
        //  new LunarDateSymbolSupplier(1999, 0, 1999, 6);
        model.setDateSymbolSupplier(l);
        JPanel pan = new JPanel(new BorderLayout());
        CalendarMonth cm = new CalendarMonth(model);
        DefaultCalendarCellRenderer r = (DefaultCalendarCellRenderer) cm.getCellRenderer();
        cm.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        cm.setDisplayMode(cm.MULTI_MONTH_MODE);
        r.setBackground1(r.DEFAULT_MULTI_MONTH_BACKGROUND1);
        r.setBackground2(r.DEFAULT_MULTI_MONTH_BACKGROUND2);
        r.setDateSymbolSupplier(l);
        JCalendar jcal = new JCalendar(cm, true, true, true, true);
        jcal.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                System.out.println("Got selection event: " + e.toString());
            };
        });
        pan.add(jcal);
        frame.getContentPane().add("Center", pan);

        //frame.pack();
        frame.setVisible(true);
    }

}
