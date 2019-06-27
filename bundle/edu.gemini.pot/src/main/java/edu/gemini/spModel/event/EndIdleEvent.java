//
// $Id: EndIdleEvent.java 6272 2005-06-02 05:39:33Z shane $
//

package edu.gemini.spModel.event;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioParseException;

/**
 * Event indicating that a paused observation has been continued.
 */
public final class EndIdleEvent extends ExecEvent {

    public EndIdleEvent(long time) {
        super(time);
    }

    public EndIdleEvent(ParamSet paramSet) throws PioParseException {
        super(paramSet);
    }

    public void doAction(ExecAction action) {
        action.endIdle(this);
    }

    public String getKind() {
        return "EndIdle";
    }

    public String getName() {
        return "End Idle";
    }

    @Override
    public String toStringProperties() {
        return "";
    }

}
