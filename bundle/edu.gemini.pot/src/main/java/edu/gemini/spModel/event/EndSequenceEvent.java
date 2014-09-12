//
// $Id: EndSequenceEvent.java 6272 2005-06-02 05:39:33Z shane $
//

package edu.gemini.spModel.event;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioParseException;

/**
 * Event sent by the sequence executor to indicate that a sequence execution
 * has completed.  There may be multiple sequence start/end events within a
 * single visit.
 */
public final class EndSequenceEvent extends ObsExecEvent {
    public EndSequenceEvent(long time, SPObservationID obsId) {
        super(time, obsId);
    }

    public EndSequenceEvent(ParamSet paramSet) throws PioParseException {
        super(paramSet);
    }

    public void doAction(ExecAction action) {
        action.endSequence(this);
    }

    public String getKind() {
        return "EndSequence";
    }

    public String getName() {
        return "End Sequence";
    }

}
