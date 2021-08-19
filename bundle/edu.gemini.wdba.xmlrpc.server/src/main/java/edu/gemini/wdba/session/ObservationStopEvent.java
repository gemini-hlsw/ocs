//
// $Id: ObservationStopEvent.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.shared.util.immutable.ImOption;

import java.time.Instant;

/**
 * Specialized <tt>{@link SessionEvent}</tt> used to indicate that a session
 * observation has stopped with a reason why.
 *
 */
public final class ObservationStopEvent extends SessionEvent {

    // The optional reason for the stopping
    private final String _reason;

    /**
     * Specialized <tt>{@link SessionEvent}</tt> used to indicate that a session
     * sequence has been stopped
     *
     * @param src source of the event
     * @param observationID the  observation ID the event is associated with
     * @param reason the optional reason on the premature stop
     */
    public ObservationStopEvent(Object src, SPObservationID observationID, String reason) {
        this(src, observationID, Instant.now(), reason);
    }

    public ObservationStopEvent(Object src, SPObservationID observationID, Instant when, String reason) {
        super(src, observationID, EventMsg.OBSERVATION_STOP, when);
        _reason = reason;
    }

    /**
     * Returns the possible reason for stopping.
     * When no reason is set, it returns empty string.
     * @return the reason, if any for the event as a String
     */
    public String getReason() {
        return _reason == null ? "" : _reason;
    }
}
