//
// $Id: ObservationPauseEvent.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.pot.sp.SPObservationID;

/**
 * Specialized <tt>{@link SessionEvent}</tt> used to indicate that a session
 * observation has aborted with a reason why.
 *
 */
public final class ObservationPauseEvent extends SessionEvent {

    // The optional reason for the abort
    private final String _reason;

    /**
     * Specialized <tt>{@link SessionEvent}</tt> used to indicate that a session
     * sequence has been abandoned and why.
     *
     * @param src source of the event
     * @param observationID the ID of the observation associated with the event as a {@link SPObservationID} object.
     * @param reason the optional reason on the premature pause
     */
    public ObservationPauseEvent(Object src, SPObservationID observationID, String reason) {
        super(src, observationID, EventMsg.OBSERVATION_PAUSE);
        _reason = reason;
    }

    /**
     * Returns the possible reason for stopping.
     * When no reason is set, it returns empty string.
     * @return the reason as a String
     */
    public String getReason() {
        return _reason == null ? "" : _reason;
    }
}
