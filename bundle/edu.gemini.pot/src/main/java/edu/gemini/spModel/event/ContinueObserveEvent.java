//
// $Id: ContinueObserveEvent.java 6272 2005-06-02 05:39:33Z shane $
//

package edu.gemini.spModel.event;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioParseException;

/**
 * Event indicating that a paused observation has been continued.
 */
public final class ContinueObserveEvent extends ObsExecEvent {
    public ContinueObserveEvent(long time, SPObservationID obsId) {
        super(time, obsId);
    }

    public ContinueObserveEvent(ParamSet paramSet) throws PioParseException {
        super(paramSet);
    }

    public void doAction(ExecAction action) {
        action.continueObserve(this);
    }

    public String getKind() {
        return "ContinueObserve";
    }

    public String getName() {
        return "Continue Observe";
    }
}
