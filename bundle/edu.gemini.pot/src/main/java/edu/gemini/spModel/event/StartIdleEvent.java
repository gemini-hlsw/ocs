//
// $Id: StartIdleEvent.java 6272 2005-06-02 05:39:33Z shane $
//

package edu.gemini.spModel.event;

import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioParseException;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

/**
 * Event indicating that a paused observation has been continued.
 */
public final class StartIdleEvent extends ExecEvent {

    public static final String REASON_PARAM = "reason";

    private final String _reason;

    public StartIdleEvent(long time, String reason) {
        super(time);
        _reason = reason;
    }

    public StartIdleEvent(ParamSet paramSet) throws PioParseException {
        super(paramSet);

        _reason = Pio.getValue(paramSet, REASON_PARAM);
    }

    public String getReason() {
        return _reason;
    }

    public void doAction(ExecAction action) {
        action.startIdle(this);
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = super.toParamSet(factory);

        if (_reason != null) {
            Pio.addParam(factory, paramSet, REASON_PARAM, _reason);
        }
        return paramSet;
    }

    public boolean equals(Object other) {
        boolean res = super.equals(other);
        if (!res) return false;

        StartIdleEvent that = (StartIdleEvent) other;
        if (_reason == null) {
            if (that._reason != null) return false;
        } else if (that._reason == null) {
            return false;
        } else {
            if (!_reason.equals(that._reason)) return false;
        }

        return true;
    }

    public int hashCode() {
        int res = super.hashCode();
        res = 37 * res + _reason.hashCode();
        return res;
    }

    public String getKind() {
        return "StartIdle";
    }

    public String getName() {
        return "Start Idle";
    }

    @Override
    public String toStringProperties() {
        return String.format(
            "reason=%s",
            ImOption.apply(_reason).map(s -> String.format("'%s'", s)).getOrNull()
        );
    }

}
