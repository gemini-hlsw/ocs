//
// $Id: ObsExecEvent.java 6272 2005-06-02 05:39:33Z shane $
//

package edu.gemini.spModel.event;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioParseException;

/**
 * The base class for {@link ExecEvent}s that pertain to a particular
 * observation.  They each maintain the observation id of the observation to
 * which they pertain.
 */
public abstract class ObsExecEvent extends ExecEvent {
    public static final ObsExecEvent[] EMPTY_ARRAY = new ObsExecEvent[0];

    public static final String OBS_ID_PARAM = "obsid";

    private static final long serialVersionUID = 1L;

    private final SPObservationID _obsId;

    /**
     * Creates the ObsExecEvent with the observation id and the event time.
     *
     * @param time  absolute time at which the event occurred
     * @param obsId id of the observation to which the event applies
     */
    ObsExecEvent(long time, SPObservationID obsId) {
        super(time);
        if (obsId == null) throw new NullPointerException("obsId == null");

        _obsId = obsId;
    }

    ObsExecEvent(ParamSet paramSet) throws PioParseException {
        super(paramSet);

        final String obsIdStr = Pio.getValue(paramSet, OBS_ID_PARAM);
        if (obsIdStr == null) {
            throw new PioParseException("missing '" + OBS_ID_PARAM + "'");
        }

        try {
            _obsId = new SPObservationID(obsIdStr);
        } catch (SPBadIDException ex) {
            throw new PioParseException("bad obs id: " + obsIdStr, ex);
        }
    }

    /**
     * Gets the observation id.
     */
    public SPObservationID getObsId() {
        return _obsId;
    }

    public ParamSet toParamSet(PioFactory factory) {
        final ParamSet paramSet = super.toParamSet(factory);
        Pio.addParam(factory, paramSet, OBS_ID_PARAM, _obsId.toString());
        return paramSet;
    }

    public boolean equals(Object other) {
        if (!super.equals(other)) return false;

        final ObsExecEvent that = (ObsExecEvent) other;
        if (!_obsId.equals(that._obsId)) return false;

        return true;
    }

    public int hashCode() {
        final int res = super.hashCode();
        return 37 * res + _obsId.hashCode();
    }

    @Override
    protected String toStringProperties() {
        return String.format("obsId=%s", _obsId);
    }

}
