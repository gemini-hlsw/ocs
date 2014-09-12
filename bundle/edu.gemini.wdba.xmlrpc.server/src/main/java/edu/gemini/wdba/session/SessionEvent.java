//
// $Id: SessionEvent.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.pot.sp.SPObservationID;

import java.util.EventObject;

/**
 * An event object that represents a state update for something, like a task,
 * that generates state information.
 * <p>
 * This is the class that satisfies most event, but can be extended for events
 * that require more information.
 */
public class SessionEvent extends EventObject {
    // Used when the observation id isn't needed

    private EventMsg _msg;
    private SPObservationID _observationID;
    private long _time;

    /**
     * Creates with the source the state itself, the sessionId, and the observationId.
     *
     * @param source source of the event
     * @param observationID observationID this event belongs to.
     * @param msg session message received (may not be <code>null</code>)
     *
     * @throws NullPointerException if state, sessionId, or observationId
     * is <code>null</code>
     */
    public SessionEvent(Object source, SPObservationID observationID, EventMsg msg) {
        super(source);
        _observationID = observationID;
        _msg = msg;
        _time = System.currentTimeMillis();
    }

    /**
     * A copy constructor that takes a current event and a new message.  Used for creating new events
     * from ones that have been received.  Note the time is that of the current event.
     * @param evt A <code>SessionEvent</code> or subclass
     * @param msg An <code>EventMsg</code>
     */
    public SessionEvent(SessionEvent evt, EventMsg msg) {
        super(evt.getSource());
        //_observationID = evt.getObservationID();
        _time = evt.getTime();
        _msg = msg;
    }

    /**
     * Gets the new event message.
     * @return the event's message
     */
    public EventMsg getMsg() {
        return _msg;
    }

    /**
     * Gets the observation Id for this event.
     * @return the SessionEvent's observation ID as a {@link SPObservationID} object.
     */
    public SPObservationID getObservationID() {
        return _observationID;
    }

    /**
     * Return the observation ID as a string for compatibility.
     * @return the SessionEvent's observations ID as a String
     */
    public String getObservationIDasString() {
        return _observationID.stringValue();
    }

    /**
     * Gets the time that the event occured.
     * @return the time the event was created as a long from the familiar epoch
     */
    public long getTime() {
        return _time;
    }
}

