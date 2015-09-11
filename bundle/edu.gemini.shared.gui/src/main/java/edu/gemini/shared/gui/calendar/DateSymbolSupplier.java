// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DateSymbolSupplier.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.gui.calendar;

import java.awt.Graphics;
import javax.swing.Icon;

/**
 * This interface describes an object that knows what symbols
 * belong on a calendar.
 */
public interface DateSymbolSupplier {

    /*
     * Serves as a hint to the supplier so it can pre-calculate
     * date symbols if it needs to.
     * If client fails to call this then the supplier may just
     * return null from any call to getDateSymbolIcon().
     */
    public abstract void setRange(int startYear, int startMonth, int endYear, int endMonth);

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
    public abstract void cachePaintValues(Graphics g, int width, int height);

    /**
     * Returns an Icon object if specified day corresponds
     * to a date symbol.  Returns null otherwise.
     */
    public abstract Icon getDateSymbolIcon(int year, int month, int day);
}
