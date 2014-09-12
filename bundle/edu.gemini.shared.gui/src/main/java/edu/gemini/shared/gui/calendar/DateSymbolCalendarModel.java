package edu.gemini.shared.gui.calendar;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.awt.Graphics;
import javax.swing.Icon;

import edu.gemini.shared.util.CalendarUtil;

/**
 * This class supplies services of DateSymbolSupplier as well as
 * CalendarModel.
 * The reason to combine these roles is that the calendar classes
 * are already set up to set the range on the model.
 * If you had a separate DateSymbolSupplier out there, nobody would
 * know to call its setRange() method.
 * This class implements DateSymbolSupplier by deferring to a
 * member DateSymbolSupplier delegate object.
 * This class overrides CalendarModel.setRange() to call setRange
 * on this delegate.
 * Note that the client has to set the DateSymbolSupplier delegate
 * with a call to setDateSymbolSupplier or no symbols will be returned.
 */

public class DateSymbolCalendarModel extends DefaultCalendarModel implements DateSymbolSupplier {

    private DateSymbolSupplier _dateSymbolSupplier = null;

    /**
     * Constructs a default model.
     */
    public DateSymbolCalendarModel() {
        super();
    }

    /**
     * Constructs a model with the specified start and end months.
     * @param startYear - full 4-digit year
     * @param startMonth - one-based month number (e.g. March = 3)
     * @param endYear - full 4-digit year
     * @param endMonth - one-based month number (e.g. March = 3)
     */
    public DateSymbolCalendarModel(int startYear, int startMonth, int endYear, int endMonth) {
        super(startYear, startMonth, endYear, endMonth);
    }

    /**
     * Sets the DateSymbolSupplier.
     */
    public void setDateSymbolSupplier(DateSymbolSupplier d) {
        _dateSymbolSupplier = d;
        if (getDateSymbolSupplier() == null)
            return;
        getDateSymbolSupplier().setRange(getStartYear(), getStartMonth(), getEndYear(), getEndMonth());
    }

    /**
     * Gets the DateSymbolSupplier.
     */
    public DateSymbolSupplier getDateSymbolSupplier() {
        return _dateSymbolSupplier;
    }

    /**
     * Returns an Icon object if specified day corresponds
     * to a date symbol.  Returns null otherwise.
     */
    public Icon getDateSymbolIcon(int year, int month, int day) {
        if (getDateSymbolSupplier() == null)
            return null;
        return getDateSymbolSupplier().getDateSymbolIcon(year, month, day);
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
        if (getDateSymbolSupplier() == null)
            return;
        getDateSymbolSupplier().cachePaintValues(g, width, height);
    }

    /**
     * Sets the span of time served by this model.
     */
    public void setRange(int startYear, int startMonth, int endYear, int endMonth) {
        super.setRange(startYear, startMonth, endYear, endMonth);
        if (_dateSymbolSupplier != null) {
            _dateSymbolSupplier.setRange(startYear, startMonth, endYear, endMonth);
        }
    }

    public String toString() {
        String s = "DateSymbolCalendarModel ";
        s += getStart().toString() + getEnd().toString();
        return s;
    }

}
