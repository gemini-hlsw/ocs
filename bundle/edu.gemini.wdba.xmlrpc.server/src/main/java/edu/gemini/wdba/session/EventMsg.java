//
// $Id: EventMsg.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.wdba.xmlrpc.ServiceException;

import java.io.ObjectStreamException;
import java.io.Serializable;


/**
 * An enumerated type to express the state of a session event.
 */
public abstract class EventMsg implements Serializable {

    /**
     * A do-nothing implementation of the DpStatus.Action interface intended
     * for subclass implementations that need to perform an action for a
     * subset of the status types.
     */
    public static abstract class AbstractAction implements EventMsgAction {
        /**
         * Does nothing.  Subclasses may override if an action is required.
         */
        public void observationStart(SessionEvent evt) throws ServiceException {
            throw new ImproperEventException();
        }

        /**
         * Does nothing.  Subclasses may override if an action is required.
         */
        public void observationEnd(SessionEvent evt) throws ServiceException {
            throw new ImproperEventException();
        }

        /**
         * Does nothing.  Subclasses may override if an action is required.
         */
        public void setIdleCause(SessionEvent evt) throws ServiceException {
            throw new ImproperEventException();
        }

        /**
         * Does nothing.  Subclasses may override if an action is required.
         */
        public void sequenceStart(SessionEvent evt) throws ServiceException {
            throw new ImproperEventException();
        }

        /**
         * Does nothing.  Subclasses may override if an action is required.
         */
        public void sequenceEnd(SessionEvent evt) throws ServiceException {
            throw new ImproperEventException();
        }

        /**
         * Does nothing.  Subclasses may override if an action is required.
         */
        public void observationAbort(SessionEvent evt) throws ServiceException {
            throw new ImproperEventException();
        }

        /**
         * Does nothing.  Subclasses may override if an action is required.
         */
        public void observationPause(SessionEvent evt) throws ServiceException {
            throw new ImproperEventException();
        }

        /**
         * Does nothing.  Subclasses may override if an action is required.
         */
        public void observationContinue(SessionEvent evt) throws ServiceException {
            throw new ImproperEventException();
        }

        /**
         * Does nothing.  Subclasses may override if an action is required.
         */
        public void observationStop(SessionEvent evt) throws ServiceException {
            throw new ImproperEventException();
        }

        /**
         * Does nothing.  Subclasses may override if an action is required.
         */
        public void datasetComplete(SessionEvent evt) throws ServiceException {
            throw new ImproperEventException();
        }

        /**
         * Does nothing.  Subclasses may override if an action is required.
         */
        public void datasetStart(SessionEvent evt) throws ServiceException {
            throw new ImproperEventException();
        }

    }

    public static final EventMsg OBSERVATION_START = new EventMsg("observationStart") {
        public void doAction(EventMsgAction action, SessionEvent evt) throws ServiceException {
            action.observationStart(evt);
        }
    };

    public static final EventMsg OBSERVATION_END = new EventMsg("observationEnd") {
        public void doAction(EventMsgAction action, SessionEvent evt) throws ServiceException {
            action.observationEnd(evt);
        }
    };

    public static final EventMsg SET_IDLE_CAUSE = new EventMsg("setIdleCause") {
        public void doAction(EventMsgAction action, SessionEvent evt) throws ServiceException {
            action.setIdleCause(evt);
        }
    };

    public static final EventMsg SEQUENCE_START = new EventMsg("sequenceStart") {
        public void doAction(EventMsgAction action, SessionEvent evt) throws ServiceException {
            action.sequenceStart(evt);
        }
    };

    public static final EventMsg SEQUENCE_END = new EventMsg("sequenceEnd") {
        public void doAction(EventMsgAction action, SessionEvent evt) throws ServiceException {
            action.sequenceEnd(evt);
        }
    };

    public static final EventMsg OBSERVATION_ABORT = new EventMsg("observationAbort") {
        public void doAction(EventMsgAction action, SessionEvent evt) throws ServiceException {
            action.observationAbort(evt);
        }
    };

    public static final EventMsg OBSERVATION_PAUSE = new EventMsg("observationPause") {
        public void doAction(EventMsgAction action, SessionEvent evt) throws ServiceException {
            action.observationPause(evt);
        }
    };

    public static final EventMsg OBSERVATION_CONTINUE = new EventMsg("observationContinue") {
        public void doAction(EventMsgAction action, SessionEvent evt) throws ServiceException {
            action.observationContinue(evt);
        }
    };

    public static final EventMsg OBSERVATION_STOP = new EventMsg("observationStop") {
        public void doAction(EventMsgAction action, SessionEvent evt) throws ServiceException {
            action.observationStop(evt);
        }
    };

    public static final EventMsg DATASET_START = new EventMsg("datasetStart") {
        public void doAction(EventMsgAction action, SessionEvent evt) throws ServiceException {
            action.datasetStart(evt);
        }
    };

    public static final EventMsg DATASET_COMPLETE = new EventMsg("datasetComplete") {
        public void doAction(EventMsgAction action, SessionEvent evt) throws ServiceException {
            action.datasetComplete(evt);
        }
    };

    /**
     * All statuses.
     */
    public static final EventMsg[] STATES = new EventMsg[]{OBSERVATION_START, OBSERVATION_END, SET_IDLE_CAUSE,
                                                           SEQUENCE_START, SEQUENCE_END,
                                                           OBSERVATION_ABORT, OBSERVATION_PAUSE, OBSERVATION_CONTINUE,
                                                           OBSERVATION_STOP, DATASET_START, DATASET_COMPLETE};

    private static int _nextOrdinal = 0;

    private final int _ordinal = _nextOrdinal++;
    private final String _msgStr;

    private EventMsg(String msgStr) {
        _msgStr = msgStr;
    }

    public String getMsgName() {
        return _msgStr;
    }

    public String toString() {
        return getMsgName();
    }

    public abstract void doAction(EventMsgAction action, SessionEvent evt) throws ServiceException;

    // Guarantee that no duplicate copies are created via serialization.
    Object readResolve() throws ObjectStreamException {
        return STATES[_ordinal];
    }
}

