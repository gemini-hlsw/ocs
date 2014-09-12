//
// $Id: StartSequenceEvent.java 6272 2005-06-02 05:39:33Z shane $
//

package edu.gemini.spModel.event;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioParseException;

/**
 * Event sent by the sequence executor to indicate that a sequence execution
 * is beginning.  There may be multiple sequence start/end events within a
 * single visit.
 */
public final class StartSequenceEvent extends ObsExecEvent {
    public StartSequenceEvent(long time, SPObservationID obsId) {
        super(time, obsId);
    }

    public StartSequenceEvent(ParamSet paramSet) throws PioParseException {
        super(paramSet);
    }

    public void doAction(ExecAction action) {
        action.startSequence(this);
    }

    public String getKind() {
        return "StartSequence";
    }

    public String getName() {
        return "Start Sequence";
    }

}
