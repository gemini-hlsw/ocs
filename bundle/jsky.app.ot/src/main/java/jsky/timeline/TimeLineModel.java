package jsky.timeline;

import jsky.science.Time;

import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.Iterator;

/**
 *
 * The model for a time line.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		05/19/99
 * @author		M. Fishman
 **/
public interface TimeLineModel {

    String NODE_ADDED = "node added";
    String NODE_REMOVED = "node removed";
    String ALL_NODES_REMOVED = "all nodes removed";

    /**
     *
     * add a time line node to the time line without checking its legality
     *
     **/
    void addTimeLineNode(TimeLineNodeModel node);


    /**
     *
     * remove a time line node from the time line
     *
     **/
    void removeTimeLineNode(TimeLineNodeModel node);

    /**
     *
     * remove all time line nodes from time line
     *
     **/
    void removeAllTimeLineNodes();


    /**
     *
     * get the number of intervals in the time line
     *
     **/
    int getIntervalCount();


    /**
     *
     * get an iterator for the time line nodes
     *
     **/
    Iterator<TimeLineNodeModel> getTimeLineNodesIterator();

    /**
     *
     * add a  property change listener to the time line.
     *
     **/
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     *
     * remove a property change listener from the time line
     *
     **/
    void removePropertyChangeListener(PropertyChangeListener listener);


    /**
     *
     * takes a time and converts it into a date where the new date is
     * equal to the start date + the time
     *
     **/
    Date getDateForTime(Time time);

    /**
     *
     * takes a date and convert it to a time where the new time is
     * equal to the date - start date
     *
     **/
    Time getTimeForDate(Date date);

    /**
     *
     * set the date from which the timeline should start
     *
     *  Note: if the date is not null then all time values are considered offsets from it
     *
     **/
    void setStartDate(Date date);


    /**
     *
     * get the start date
     *
     **/
    Date getStartDate();


    /**
     *
     * get the starting value in the timeline
     *
     **/
    Time getStartTime();

    /**
     *
     * get the ending value of the timeline
     *
     **/
    Time getEndTime();


    /**
     *
     * returns whether or not the model contains the specified node
     */
    boolean contains(TimeLineNodeModel model);


}
