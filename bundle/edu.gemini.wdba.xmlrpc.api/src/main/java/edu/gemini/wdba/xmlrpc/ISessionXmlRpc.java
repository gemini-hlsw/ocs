// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ISessionXmlRpc.java 846 2007-05-19 02:55:18Z gillies $
//
package edu.gemini.wdba.xmlrpc;

import java.util.List;
import java.util.Map;

/**
 * In the OCS a session is starged for each team that is observing on the
 * telescope system.  The OT can add and remove observations from a session.
 * The TCC and remove observations from their session.
 */
public interface ISessionXmlRpc {

    String NAME = "WDBA_Session";

    /**
     * Factory to create a new session (may not be used in OCS 1)
     *
     * @return the id of the new session, which must be used in the other
     *         methods.
     */
    String createSession() throws ServiceException;

    /**
     * Factory to create a new session (may not be used in OCS 1)
     *
     * @param sessionId the name of the session (same as return value.)
     * @return the id of the new session, which must be used in the other
     *         methods.
     */
    String createSession(String sessionId) throws ServiceException;

    /**
     * Remove a session and all its contents.
     *
     * @param sessionId is the id of the session to be used to remove.
     * @return true if the operation is successful, else false
     *         methods.
     */
    boolean removeSession(String sessionId) throws ServiceException;

    /**
     * Remove all sessions and contents.
     *
     * @return true if the operation is successful, else false
     *         methods.
     */
    void removeAllSessions() throws ServiceException;

    /**
     * Return the number of sessions in use.
     *
     * @return the number of sessions
     */
    int getSessionCount() throws ServiceException;

    /**
     * Add a specific observation id to a session
     *
     * @param sessionId     is the id of the session to be used to add the observation
     * @param observationId is the observation to add
     * @return true if the operation is successful, else false
     */
    boolean addObservation(String sessionId, String observationId) throws ServiceException;

    /**
     * Remove a specific observation id from a session
     *
     * @param sessionId     is the id of the session to be used to remove the observation
     * @param observationId is the observation to remove
     * @return true if the operation is successful, else false
     */
    boolean removeObservation(String sessionId, String observationId) throws ServiceException;

    /**
     * Returns an array of Strings that is the observation ids that are
     * currently in the session pool.
     * * Note that this call requires that the database be contacted to verify the existence of the observations.
     *
     * @param sessionId is the id of the session that should be listed
     * @return an array of observations.  If no observations are resent an
     *         empty array is returned never null.
     */
    String[] getObservations(String sessionId) throws ServiceException;

    /**
     * Returns a list of <tt>Maps</tt> containing observationIDs and observation titles.
     * The attribute names in the map are "id" and "title".
     * Note that this call requires that the database be contacted to verify the existence of the observations.
     *
     * @param sessionId The observation information returned will come from the session with this name
     * @return an ordered <tt>List</tt> of <tt>Map</tt>s with two members each.
     * @throws ServiceException if a problem is found --
     */
    List<Map<String, String>> getObservationsAndTitles(String sessionId) throws ServiceException;

    /**
     * Returns the number of observation ids in a particular session.
     */
    int getObservationCount(String sessionId) throws ServiceException;

    /**
     * Removes all the observations in a session, leaving an empty session.
     *
     * @param sessionId is the id of the session that should be cleared
     * @return true if all observations were removed without a problem, false otherwise
     */
    boolean removeAllObservations(String sessionId) throws ServiceException;

    /** ----------------------------------------------------------------------
     * Time managment functionality.
     * The following methods are used by the TCC to tell the rest of the system
     * when it has started actions related to the session.
     * <p>
     * The session contains an internal state machine having to do with time accounting.
     * The session must be in some state at all times to account for all the time.
     * At all times the session is in one of two states: idle or observing.
     */

    /**
     * Sending TCC indicates that an observation has started.
     * <p/>
     * This means:
     * <ul>
     * <li>The TCC has started the slew process for a specific observation.</li>
     * </ul>
     * <p/>
     * If the session state is currently observing and an obsStart is received it
     * indicates that the last observation ended and a new one is starting.
     */
    boolean observationStart(String SessionId, String observationId) throws ServiceException;

    /**
     * Sending TCC indicates that an ongoing observation has ended and entered
     * the idle state.  Since only one observation can be running at any time in
     * a session.  This is a short-cut for startIdle(
     * <p/>
     * This means:
     * <ul>
     * <li>For some reason the observation has ended.  When the session receives this
     * the following can happen:</li>
     * <ul>
     * <li>If it is "idle" nothing happens.</li>
     * <li>If it is currently observing, the observation is marked as ending and the
     * accounting system assumes that the session is inactive because the
     * observers are inactive.</li>
     * </ul>
     * </li>
     */
    boolean observationEnd(String sessionId, String observationId) throws ServiceException;

    static final String CAT_TELESCOPE_SYSTEM = "telescope";
    static final String CAT_INSTRUMENT_SYSTEM = "instrument";
    static final String CAT_NETWORK_COMP_SYSTEM = "network";
    static final String CAT_SOFTWARE_SYSTEM = "software";
    static final String CAT_WEATHER = "weather";
    static final String CAT_HUMAN_ERR = "humanError";
    static final String CAT_INACTIVE = "inactive";

    /**
     * The sending application indciates that the session is now idle and includes
     * a category for why it is idle.  Categories are indicated with an optional
     * comment.
     * <p/>
     * Once in the idle state, the only way to leave is with a <tt>startObservation</tt>.
     * It is possible to send consequitive startIdle
     */
    boolean setIdleCause(String sessionId, String category, String comment) throws ServiceException;

    /**
     * Indicate that the sequence of the observation in the session is started.
     */
    boolean sequenceStart(String sessionId, String observationId, String firstFileId) throws ServiceException;

    /**
     * Indicates that the sequence for the observation has ended.
     */
    boolean sequenceEnd(String sessionId, String observationId) throws ServiceException;

    /**
     * Indicate that the sequence of the obesrvation has been abandoned for a specific
     * reason.
     */
    boolean observationAbort(String sessionId, String observationId, String reason) throws ServiceException;

    /**
     * Indicate that the sequence of the obesrvation has been paused for a specific
     * reason.
     */
    boolean observationPause(String sessionId, String observationId, String reason) throws ServiceException;

    /**
     * Indicate that the sequence of the obesrvation has been stopped for a specific
     * reason.
     */
    boolean observationStop(String sessionId, String observationId, String reason) throws ServiceException;

    /**
     * Indicate that the sequence of the obesrvation has been continued after pausing
     */
    boolean observationContinue(String sessionId, String observationId) throws ServiceException;

    /**
     * Indicates that one of the observe for a dataset has started
     */
    boolean datasetStart(String sessionId, String observationId, String datasetId, String fileName) throws ServiceException;

    /**
     * Indicates that one of the datasets for the observation has been completed.
     */
    boolean datasetComplete(String sessionId, String observationId, String datasetId, String fileName) throws ServiceException;

}
