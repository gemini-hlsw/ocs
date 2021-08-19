//
// $Id: IdleCauseEvent.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import java.time.Instant;

/**
 * Specialized <tt>{@link SessionEvent}</tt> used to indicate that a session
 * is idle and the reason why.
 */
public class IdleCauseEvent extends SessionEvent {
    // The type of idleness (
    private final String _category;
    // The optinoal reason for the idleness
    private final String _comment;

    /**
     * Specialized <tt>{@link SessionEvent}</tt> used to indicate that a session
     * is idle and the reason why.
     *
     * @param src source of the event
     * @param category type of idle cause
     * @param comment the optional comment on the idleness
     */
    public IdleCauseEvent(Object src, String category, String comment) {
        this(src, Instant.now(), category, comment);
    }

    public IdleCauseEvent(Object src, Instant when, String category, String comment) {
        super(src, null, EventMsg.SET_IDLE_CAUSE, when);
        _category = category;
        _comment  = comment;
    }

    /**
     * Gets the idle category.
     * @return a String indicating the category of idleness
     */
    public String getCategory() {
        return _category;
    }

    /**
     * Returns the possible reason for idleness.  This will also return something.
     * When no reason is set, it returns empty string.
     * @return the possible reason for being idle
     */
    public String getComment() {
        return _comment == null ? "" : _comment;
    }
}
