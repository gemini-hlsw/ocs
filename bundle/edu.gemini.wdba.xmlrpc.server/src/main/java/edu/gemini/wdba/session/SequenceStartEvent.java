//
// $Id: SequenceStartEvent.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.pot.sp.SPObservationID;

/**
 * Specialized <tt>{@link SessionEvent}</tt> used to indicate the start of an
 * observation's sequence.
 */
public final class SequenceStartEvent extends SessionEvent {

    // The starting filename
    private final String _startFileName;

    /**
     * Specialized <tt>{@link SessionEvent}</tt> used to indicate that a dataset
     * has been completed and saved.
     *
     * @param src the source of this event
     * @param observationID the observation ID associated with this event
     * @param startFileName the dhs filename for the completed observation
     */
    public SequenceStartEvent(Object src, SPObservationID observationID, String startFileName) {
        super(src, observationID, EventMsg.SEQUENCE_START);
        if (startFileName == null) throw new NullPointerException();
        _startFileName = startFileName;
    }

    /**
     * Returns the starting filename for the sequence
     * @return the first file name as a String
     */
    public String getStartFileName() {
        return _startFileName;
    }
}
