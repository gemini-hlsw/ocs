//
// $Id: StartVisitEvent.java 6272 2005-06-02 05:39:33Z shane $
//

package edu.gemini.spModel.event;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioParseException;

/**
 * Every contiguous period of time spent on a particular observation will be
 * preceeded by a start visit event.  There can be multiple start visit
 * events if the observation is visited multiple times over the course of its
 * life.
 */
public final class StartVisitEvent extends ObsExecEvent {
    public StartVisitEvent(long time, SPObservationID obsId) {
        super(time, obsId);
    }

    public StartVisitEvent(ParamSet paramSet) throws PioParseException {
        super(paramSet);
    }

    public void doAction(ExecAction action) {
        action.startVisit(this);
    }

    public String getKind() {
        return "StartVisit";
    }

    public String getName() {
        return "Start Visit";
    }
}
