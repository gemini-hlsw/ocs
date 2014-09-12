/*
===================================================================

    CalendarGroup.java

    Creaded by Claude Duguay
    Copyright (c) 1999

===================================================================
*/

package edu.gemini.shared.gui.calendar;

import java.awt.*;
import java.util.*;

public class CalendarGroup {

    protected Component parent;

    protected Vector group;

    protected int active;

    public CalendarGroup() {
        this(null);
    }

    public CalendarGroup(Component parent) {
        this.parent = parent;
        group = new Vector();
        active = 0;
    }

    public void setParent(Component parent) {
        this.parent = parent;
    }

    public void setActiveMonth(int index) {
        if (index > group.size())
            throw new IndexOutOfBoundsException("Out of range CalendarGroup index");
        active = index;
        /*      for (int i = 0; i < group.size(); i++) {
           getCalendarMonth(i).setActive(i == active);
           if (i == active)
              getCalendarMonth(i).requestFocus();
              }*/
    }

    public void setActiveMonth(CalendarMonth month) {/*
       for (int i = 0; i < group.size(); i++)
          {
             if (getCalendarMonth(i) == month)
                {
                   setActiveMonth(i);
                   parent.repaint();
                   break;
                }
                }*/
    }

    public void add(CalendarMonth month) {
        group.addElement(month);
        active = group.size() - 1;
    }

    public CalendarMonth getActiveMonth() {
        return getCalendarMonth(active);
    }

    public CalendarMonth getCalendarMonth(int index) {
        return (CalendarMonth) group.elementAt(index);
    }

    public CalendarMonth nextCalendarMonth() {
        active++;
        if (active >= group.size())
            active = 0;
        setActiveMonth(active);
        return getCalendarMonth(active);
    }

    public CalendarMonth prevCalendarMonth() {
        active--;
        if (active < 0)
            active = group.size() - 1;
        setActiveMonth(active);
        return getCalendarMonth(active);
    }

    public boolean isFirstCalendarMonth(CalendarMonth month) {
        return month == getCalendarMonth(0);
    }

    public boolean isLastCalendarMonth(CalendarMonth month) {
        return month == getCalendarMonth(group.size() - 1);
    }

    public void nextMonth() {
        for (int i = 0; i < group.size(); i++) {
            getCalendarMonth(i).nextMonth();
            getCalendarMonth(i).invalidate();
            //if (repaint && parent != null)
            //   parent.repaint();
        }
    }

    public void prevMonth() {
        for (int i = 0; i < group.size(); i++) {
            getCalendarMonth(i).previousMonth();
            getCalendarMonth(i).invalidate();
            //if (repaint && parent != null)
            //   parent.repaint();
        }
    }

    public void pageForward() {
        for (int i = 0; i < group.size(); i++) {
            getCalendarMonth(i).pageForward();
            getCalendarMonth(i).invalidate();
            //if (repaint && parent != null)
            //   parent.repaint();
        }
    }

    public void pageBackward() {
        for (int i = 0; i < group.size(); i++) {
            getCalendarMonth(i).pageBackward();
            getCalendarMonth(i).invalidate();
            //if (repaint && parent != null)
            //   parent.repaint();
        }
    }
}
