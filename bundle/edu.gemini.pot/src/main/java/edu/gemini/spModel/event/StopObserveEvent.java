//
// $Id: StopObserveEvent.java 6272 2005-06-02 05:39:33Z shane $
//

package edu.gemini.spModel.event;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioParseException;

/**
 * Event indicating that the current observation was stopped.
 */
public final class StopObserveEvent extends ObsExecEvent {
    public StopObserveEvent(long time, SPObservationID obsId) {
        super(time, obsId);
    }

    public StopObserveEvent(ParamSet paramSet) throws PioParseException {
        super(paramSet);
    }

    public void doAction(ExecAction action) {
        action.stopObserve(this);
    }

    public String getKind() {
        return "StopObserve";
    }

    public String getName() {
        return "Stop Observe";
    }

}
