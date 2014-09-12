//
// $Id: OverlapEvent.java 6750 2005-11-20 22:23:04Z shane $
//

package edu.gemini.spModel.event;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioParseException;

/**
 * Event representing a time overlap with another observation.
 */
public final class OverlapEvent extends ObsExecEvent {
    public OverlapEvent(long time, SPObservationID obsId) {
        super(time, obsId);
    }

    public OverlapEvent(ParamSet paramSet) throws PioParseException {
        super(paramSet);
    }

    public String getKind() {
        return "Overlap";
    }

    public void doAction(ExecAction action) {
        action.overlap(this);
    }

    public String getName() {
        return "Overlap";
    }
}
