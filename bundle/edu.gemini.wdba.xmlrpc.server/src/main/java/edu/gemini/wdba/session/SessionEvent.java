//
// $Id: SessionEvent.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.pot.sp.SPObservationID;

import java.time.Instant;
import java.util.EventObject;

/**
 * An event object that represents a state update for something, like a task,
 * that generates state information.
 * <p>
 * This is the class that satisfies most event, but can be extended for events
 * that require more information.
 */
public class SessionEvent extends EventObject {

    private final EventMsg _msg;
    private final SPObservationID _observationID;
    private final long _time;

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
        this(source, observationID, msg, Instant.now());
    }

    public SessionEvent(Object source, SPObservationID observationID, EventMsg msg, Instant when) {
        super(source);

        _observationID = observationID;
        _msg           = msg;
        _time          = when.toEpochMilli();
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
     * Gets the time that the event occurred.
     * @return the time the event was created as a long from the familiar epoch
     */
    public long getTime() {
        return _time;
    }
}

