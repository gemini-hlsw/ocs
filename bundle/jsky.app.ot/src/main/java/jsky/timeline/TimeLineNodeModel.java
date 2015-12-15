package jsky.timeline;

import jsky.science.Time;

import java.beans.PropertyChangeListener;
import java.util.Comparator;

/**
 *
 *  The model for a time line node.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		12/10/99
 * @author		M. Fishman
 **/
public interface TimeLineNodeModel {

    // property types
    String START_TIME = "StartTime";
    String END_TIME = "EndTime";
    String NODE = "Node";
    String NAME = "Name";
    String PARENT = "Parent";


    /**
     *
     * get the time that this node starts
     *
     **/
    Time getStartTime();

    /**
     *
     * set the point on the time line that this node starts
     *
     **/
    void setStartTime(Time time);

    /**
     *
     * get the time that this node ends
     *
     **/
    Time getEndTime();

    /**
     *
     * set the time that this node ends
     *
     **/
    void setEndTime(Time time);

    /**
     *
     * move node by specified time
     *
     **/
    void moveTimeLineNodeBy(Time time);

    /**
     *
     * get the duration of the time line node
     *
     **/
    Time getDuration();


    /**
     *
     * set the duration of the time line node
     *
     **/
    void setDuration(Time durationLength);


    /**
     *
     * give the time line node a name
     *
     **/
    void setTimeLineNodeName(String name);

    /**
     *
     * get the name of the time line node
     *
     **/
    String getTimeLineNodeName();

    /**
     *
     * returns whether the node intersects the passed in node
     *
     **/
    boolean intersects(TimeLineNodeModel node);


    /**
     *
     * move node to a specified location
     *
     **/
    void setTimeLineNode(Time start, Time end);

    /**
     *
     * add a property change listener to the model
     *
     **/
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     *
     * remove a property change listener from the model
     *
     **/
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     *
     * get the time line node's parent
     *
     */
    TimeLineModel getParent();

    /**
     *
     * set the time line node's parent
     *
     */
    void setParent(TimeLineModel parent);

    /**
     *
     * is the node considered a constant
     *
     */
    boolean isConstant();

    /**
     *
     * set whether or not the node is considered a constant or not
     *
     */
    void setConstant(boolean isConstant);


    /**
     *
     * inner class used for sorting time line nodes
     *
     **/
    class TimeLineNodeComparator implements Comparator<TimeLineNodeModel> {

        @Override
        public int compare(TimeLineNodeModel o1,
                    TimeLineNodeModel o2) {
            double start1 = o1.getStartTime().getValue(Time.SECOND);
            double start2 = o2.getStartTime().getValue(Time.SECOND);
            return (int) Math.round(start1 - start2);

        }

    }

}
