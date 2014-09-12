//
// $Id: EndVisitEvent.java 6272 2005-06-02 05:39:33Z shane $
//

package edu.gemini.spModel.event;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioParseException;

/**
 * Every contiguous period of time spent on a particular observation will be
 * terminated by an end visit event.  There can be multiple end visit
 * events if the observation is visited multiple times over the course of its
 * life.
 */
public final class EndVisitEvent extends ObsExecEvent {
    public EndVisitEvent(long time, SPObservationID obsId) {
        super(time, obsId);
    }

    public EndVisitEvent(ParamSet paramSet) throws PioParseException {
        super(paramSet);
    }

    public void doAction(ExecAction action) {
        action.endVisit(this);
    }

    public String getKind() {
        return "EndVisit";
    }

    public String getName() {
        return "End Visit";
    }

}
