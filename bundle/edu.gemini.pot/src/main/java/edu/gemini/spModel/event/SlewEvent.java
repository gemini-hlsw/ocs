//
// $Id: SlewEvent.java 6272 2005-06-02 05:39:33Z shane $
//

package edu.gemini.spModel.event;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.pio.PioParseException;
import edu.gemini.spModel.pio.ParamSet;

/**
 * Event representing a telescope slew.
 */
public final class SlewEvent extends ObsExecEvent {
    public SlewEvent(long time, SPObservationID obsId) {
        super(time, obsId);
    }

    public SlewEvent(ParamSet paramSet) throws PioParseException {
        super(paramSet);
    }

    public void doAction(ExecAction action) {
        action.slew(this);
    }

    public String getKind() {
        return "Slew";
    }

    public String getName() {
        return "Slew";
    }

}
