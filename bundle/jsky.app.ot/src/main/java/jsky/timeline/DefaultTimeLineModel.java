//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	DefaultTimeLineModel
//
//--- Description -------------------------------------------------------------
//	the default time line model
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	12/27/99	M. Fishman
//
//		Original implementation.
//
//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//
//=== End File Prolog =========================================================
//package gov.nasa.gsfc.util.gui;

package jsky.timeline;

import jsky.science.Time;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;


/**
 * The default model of a time line.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		12/27/99
 * @author		M. Fishman
 **/
public class DefaultTimeLineModel implements TimeLineModel {


    protected Comparator _comparator = new TimeLineNodeModel.TimeLineNodeComparator();
    protected TreeSet _nodes;
    protected List _changeListeners;
    private int _intervalCount;


    private Time _startTime;
    private Time _endTime;
    private Date _startDate;


    protected PropertyChangeListener fMyChildListener = new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {
            evt.setPropagationId(DefaultTimeLineModel.this);
            firePropertyChange(evt);
        }
    };

    /**
     *
     * constructor
     *
     * @param interval the number of intervals in a 50 minute time line
     *
     **/
    public DefaultTimeLineModel(int interval) {
        this(0, 50, interval);
    }

    /**
     *
     * constructor
     *
     * @param start the starting minute of the timeline
     * @param end the ending minute of the timeline
     * @param intervals the number of intervals on the timeline
     *
     **/
    public DefaultTimeLineModel(int start, int end, int intervals) {
        this(new Time((double) start, Time.MINUTE), new Time((double) end, Time.MINUTE), intervals);
    }

    /**
     *
     * constructor
     *
     * @param start the starting time of the timeline
     * @param end the ending time of the timeline
     * @param intervals the number of intervals on the timeline
     *
     **/
    public DefaultTimeLineModel(Time start, Time end, int intervals) {
        super();
        _intervalCount = intervals;
        _startTime = start;
        _endTime = end;


        _nodes = new TreeSet(_comparator);
        _changeListeners = new ArrayList(5);
    }


    /**
     *
     * add a time line node to the time line without checking its legality
     *
     **/
    public void addTimeLineNode(TimeLineNodeModel node) {
        if (!_nodes.contains(node)) {
            node.setParent(this);
            _nodes.add(node);
            node.addPropertyChangeListener(fMyChildListener);
            firePropertyChange(new PropertyChangeEvent(this, TimeLineModel.NODE_ADDED, null, node));

        }
    }


    /**
     *
     * remove a time line node from the time line
     *
     **/
    public synchronized void removeTimeLineNode(TimeLineNodeModel node) {
        if (_nodes.contains(node)) {
            node.removePropertyChangeListener(fMyChildListener);
            node.setParent(null);
            _nodes.remove(node);
            firePropertyChange(new PropertyChangeEvent(node, NODE_REMOVED, node, null));

        }
    }

    /**
     *
     * remove all time line nodes from time line
     *
     **/
    public void removeAllTimeLineNodes() {
        for (Iterator iter = _nodes.iterator(); iter.hasNext();) {
            TimeLineNodeModel node = (TimeLineNodeModel) iter.next();
            node.removePropertyChangeListener(fMyChildListener);
            iter.remove();
        }
        firePropertyChange(new PropertyChangeEvent(this, ALL_NODES_REMOVED, this, null));
    }


    /**
     *
     * get the number of intervals in the time line
     *
     **/
    public int getIntervalCount() {
        return _intervalCount;
    }


    /**
     *
     * get an iterator for the time line node models
     *
     **/
    public Iterator getTimeLineNodesIterator() {
        return _nodes.iterator();
    }


    /**
     *
     * add a property change listener to the time line.
     *
     **/
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (!_changeListeners.contains(listener)) {
            _changeListeners.add(listener);
        }

    }

    /**
     *
     * remove a property change listener from the time line
     *
     **/
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        _changeListeners.remove(listener);
    }


    /**
     *
     * takes a time and converts it into a date where the new date is
     * equal to the start date + the time
     *
     **/
    public Date getDateForTime(Time time) {
        Date result = null;
        if (_startDate != null) {
            long dateMilliSecs = _startDate.getTime();
            long timeMilliSecs = (long) (time.getValue(Time.SECOND) * 1000.0);
            result = new Date(dateMilliSecs + timeMilliSecs);
        }
        return result;
    }

    /**
     *
     * takes a date and convert it to a time where the new time is
     * equal to the date - start date
     *
     **/
    public Time getTimeForDate(Date date) {
        Time result = null;
        if (_startDate != null) {
            double startMilliSecs = (double) _startDate.getTime();
            double dateMilliSecs = (double) date.getTime();
            result = new Time((dateMilliSecs - startMilliSecs) / 1000.0, Time.SECOND);
        }
        return result;
    }

    /**
     *
     * set the date from which the timeline should start
     *
     *  Note: if the date is not null then all time values are considered offsets from it
     *
     **/
    public void setStartDate(Date date) {
        _startDate = date;
    }

    /**
     *
     * get the start date
     *
     **/
    public Date getStartDate() {
        return _startDate;
    }


    /**
     *
     * get the starting value in the timeline
     *
     **/
    public Time getStartTime() {
        return _startTime;
    }

    /**
     *
     * get the ending value of the timeline
     *
     **/
    public Time getEndTime() {
        return _endTime;
    }


    /**
     *
     * fires a change event to all listeners of the timeline
     *
     **/
    protected void firePropertyChange(PropertyChangeEvent evt) {
        for (Iterator listIterator = _changeListeners.iterator(); listIterator.hasNext();) {
            PropertyChangeListener listener = (PropertyChangeListener) listIterator.next();
            if (listener != evt.getSource()) {
                listener.propertyChange(evt);
            }
        }
    }

    /**
     *
     * returns whether or not the model contains the specified node
     *
     */
    public boolean contains(TimeLineNodeModel model) {
        return _nodes.contains(model);
    }
}
