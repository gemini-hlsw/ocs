//
// $Id: ObsEventMsg.java 4726 2004-05-14 16:50:12Z brighton $
//

package edu.gemini.spModel.gemini.obscomp;

import java.io.Serializable;
import java.io.ObjectStreamException;


/**
 * An enumerated type to express the state of a session event.
 */
public class ObsEventMsg implements Serializable {

    public static final ObsEventMsg OBSERVATION_START = new ObsEventMsg("Start Observation");
    public static final ObsEventMsg OBSERVATION_END = new ObsEventMsg("End Observation");
    public static final ObsEventMsg SET_IDLE_CAUSE = new ObsEventMsg("Set Idle Cause");
    public static final ObsEventMsg SEQUENCE_START = new ObsEventMsg("Start Sequence");
    public static final ObsEventMsg OBSERVATION_ABORT = new ObsEventMsg("Abort Observation");
    public static final ObsEventMsg SEQUENCE_PAUSE = new ObsEventMsg("Pause Sequence");
    public static final ObsEventMsg SEQUENCE_END = new ObsEventMsg("End Sequence");
    public static final ObsEventMsg DATASET_COMPLETE = new ObsEventMsg("Dataset Complete");
    public static final ObsEventMsg PROGRAM_FETCH = new ObsEventMsg("Program Fetched");
    public static final ObsEventMsg PROGRAM_STORE = new ObsEventMsg("Program Stored");

    /**
     * All statuses.
     */
    public static final ObsEventMsg[] STATES = new ObsEventMsg[]{
        OBSERVATION_START, OBSERVATION_END, SET_IDLE_CAUSE, SEQUENCE_START,
        OBSERVATION_ABORT, SEQUENCE_PAUSE, SEQUENCE_END, DATASET_COMPLETE,
        PROGRAM_FETCH, PROGRAM_STORE
    };

    private static int _nextOrdinal = 0;

    private final int _ordinal = _nextOrdinal++;
    private String _msgStr;

    private ObsEventMsg(String msgStr) {
        _msgStr = msgStr;
    }

    public String getMsgName() {
        return _msgStr;
    }

    public String toString() {
        return getMsgName();
    }

    public static ObsEventMsg fromString(String name) {
        for (int i = 0; i < STATES.length; i++) {
            if (name.equals(STATES[i]._msgStr))
                return STATES[i];
        }
        return null;
    }

    // Guarantee that no duplicate copies are created via serialization.
    Object readResolve() throws ObjectStreamException {
        return STATES[_ordinal];
    }
}

