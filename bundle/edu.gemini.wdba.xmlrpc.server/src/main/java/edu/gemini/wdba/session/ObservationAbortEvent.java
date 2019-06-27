//
// $Id: ObservationAbortEvent.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.pot.sp.SPObservationID;

/**
 * Specialized <tt>{@link SessionEvent}</tt> used to indicate that a session
 * observation has aborted with a reason why.
 *
 */
public final class ObservationAbortEvent extends SessionEvent {

    // The optional reason for the abort
    private final String _reason;

    /**
     * Specialized <tt>{@link SessionEvent}</tt> used to indicate that a session
     * sequence has been abandoned and why.
     *
     * @param src source of the event
     * @param observationID the ID of the observation associated with this event
     * @param reason the optional reason on the premature pause
     */
    public ObservationAbortEvent(Object src, SPObservationID observationID, String reason) {
        super(src, observationID, EventMsg.OBSERVATION_ABORT);
        _reason = reason;
    }

    /**
     * Returns the possible reason for stopping.
     * When no reason is set, it returns empty string.
     * @return a reason for the abort as a String.
     */
    public String getReason() {
        return _reason == null ? "" : _reason;
    }
}
