package edu.gemini.wdba.session;

import edu.gemini.wdba.xmlrpc.ServiceException;

//
// Gemini Observatory/AURA
// $Id: EventMsgAction.java 756 2007-01-08 18:01:24Z gillies $
//

/**
 * An interface for perfoming actions based upon an event value.
 * Use of this interface and the {@link EventMsg#doAction} method
 * allows switch statements to be avoided.
 */
public interface EventMsgAction {
    /**
     * Performs the action associated with a OBSERVATION_START status.
     */
    void observationStart(SessionEvent evt) throws ServiceException;

    /**
     * Performs the action associated with a OBSEVATION_END status.
     */
    void observationEnd(SessionEvent evt) throws ServiceException;

    /**
     * Performs the action associated with a SET_IDLE_CAUSE status.
     */
    void setIdleCause(SessionEvent evt) throws ServiceException;

    /**
     * Performs the action associated with a SEQUENCE_START status.
     */
    void sequenceStart(SessionEvent evt) throws ServiceException;

    /**
     * Performs the action associated with a SEQUENCE_END status.
     */
    void sequenceEnd(SessionEvent evt) throws ServiceException;

    /**
     * Performs the action associated with a OBSERVATION_ABORT status.
     */
    void observationAbort(SessionEvent evt) throws ServiceException;

    /**
     * Performs the action associated with a OBSERVATION_PAUSE status.
     */
    void observationPause(SessionEvent evt) throws ServiceException;

    /**
     * Performs the action associated with a OBSERVATION_CONTINUE status.
     */
    void observationContinue(SessionEvent evt) throws ServiceException;

    /**
     * Performs the action associated with a OBSERVATION_STOP status.
     */
    void observationStop(SessionEvent evt) throws ServiceException;

    /**
     * Performs the action associated with a DATASET_START status.
     */
    void datasetStart(SessionEvent evt) throws ServiceException;

    /**
     * Performs the action associated with a DATASET_COMPLETE status.
     */
    void datasetComplete(SessionEvent evt) throws ServiceException;
}
