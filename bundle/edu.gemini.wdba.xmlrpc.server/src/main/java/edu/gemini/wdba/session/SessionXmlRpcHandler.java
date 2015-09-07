// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SessionXmlRpcHandler.java 846 2007-05-19 02:55:18Z gillies $
//
package edu.gemini.wdba.session;

import edu.gemini.wdba.glue.api.WdbaContext;
import edu.gemini.wdba.xmlrpc.ISessionXmlRpc;
import edu.gemini.wdba.xmlrpc.ServiceException;

import java.util.List;
import java.util.Map;

/**
 * Implementation of the OCS Session functionality.
 * An implementation of the ISessionXmlRpc interface.
 *
 * @author K.Gillies
 */
public final class SessionXmlRpcHandler implements ISessionXmlRpc {

    // Create the "SessionManagement" instance when we have the WdbaContext.
    // Unfortunately SessionXmlRpcHandler has to have a no-args constructor so
    // this has to be set from the activator and then later used by the
    // SessionXmlRpcHandler that is eventually created.
    private static SessionManagement sm = null;
    public static synchronized void setContext(WdbaContext context) {
        sm = (context == null) ? null : new SessionManagement(context, new ProductionSessionConfiguration(context));
    }

    private static synchronized SessionManagement sm() throws ServiceException {
        if (sm == null) throw new ServiceException("db not available");
        return sm;
    }

    /**
     * Factory to create a new session.
     *
     * @return the id of the new session, which must be used in the other
     *         methods.
     */
    public String createSession() throws ServiceException {
        return sm().createSession();
    }

    /**
     * Factory to create a new session (may not be used in OCS 1)
     *
     * @param sessionId name of the session (same as return value.)
     * @return the id of the new session, which must be used in the other
     *         methods.
     */
    public String createSession(String sessionId) throws ServiceException {
        return sm().createSession(sessionId);
    }

    /**
     * Remove a session and all its contents.
     *
     * @param sessionId is the id of the session to be used to remove.
     * @return true if the operation is successful, else false
     *         methods.
     */
    public boolean removeSession(String sessionId) throws ServiceException {
        return sm().removeSession(sessionId);
    }

    /**
     * Remove all sessions and contents.
     */
    public boolean removeAllSessions() throws ServiceException {
        return sm().removeAllSessions();
    }

    /**
     * Return the number of sessions in use.
     *
     * @return the number of sessions
     */
    public int getSessionCount() throws ServiceException {
        return sm().getSessionCount();
    }

    /**
     * Add a specific observation id to a session
     * param sessionId is the id of the session to be used to add the observation
     * param observationId is the observation to add
     * return true if the operation is successful, else false
     */
    public boolean addObservation(String sessionId, String observationId) throws ServiceException {
        return sm().addObservation(sessionId, observationId);
    }

    /**
     * Remove a specific observation id from a session
     * param sessionId is the id of the session to be used to remove the observation
     * param observationId is the observation to remove
     * return true if the operation is successful, else false
     */
    public boolean removeObservation(String sessionId, String observationId) throws ServiceException {
        return sm().removeObservation(sessionId, observationId);
    }

    /**
     * Returns an array of Strings that is the observation ids that are
     * currently in the session pool.
     * <p/>
     * param sessionId is the id of the session that should be listed
     *
     * @return an array of observations.  If no observations are resent an
     *         empty array is returned never null.
     */
    public String[] getObservations(String sessionId) throws ServiceException {
        return sm().getObservations(sessionId);
    }

    /**
     * Returns a list of <tt>Maps</tt> containing observationIDs and observation titles.
     * The attribute names in the map are "id" and "title".
     * Note that this call requires that the database be contacted to verify the existence of the observations.
     *
     * @param sessionId The observation information returned will come from the session with this name
     * @return an ordered <tt>List</tt> of <tt>Map</tt>s with two members each.
     * @throws ServiceException if a problem is found --
     */
    public List<Map<String, String>> getObservationsAndTitles(String sessionId) throws ServiceException {
        return sm().getObservationsAndTitles(sessionId);
    }

    /**
     * Returns the number of observation ids in a particular session.
     */
    public int getObservationCount(String sessionId) throws ServiceException {
        return sm().getObservations(sessionId).length;
    }

    /**
     * Removes all the observations in a session, leaving an empty session.
     */
    public boolean removeAllObservations(String sessionId) throws ServiceException {
        sm().removeAllObservations(sessionId);
        // This is to make XmlRpc happy
        return true;
    }

    /** ------------------- Time Accounting Events ----------------------- */

    /**
     * Sender indicates that an observation has started.
     */
    public boolean observationStart(String sessionId, String observationId) throws ServiceException {
        return sm().observationStart(sessionId, observationId);
    }

    /**
     * Sending TCC indicates that an ongoing observation has ended and entered
     * the idle state.
     */
    public boolean observationEnd(String sessionId, String observationId) throws ServiceException {
        return sm().observationEnd(sessionId, observationId);
    }

    /**
     * The sending application indciates that the session is now idle and includes
     * a category for why it is idle.
     */
    public boolean setIdleCause(String sessionId, String category, String comment) throws ServiceException {
        return sm().setIdleCause(sessionId, category, comment);
    }

    /**
     * Indicate that the sequence of the observation in the session is started.
     */
    public boolean sequenceStart(String sessionId, String observationId, String startFileName) throws ServiceException {
        return sm().sequenceStart(sessionId, observationId, startFileName);
    }

    /**
     * Indicates that the sequence for the observation has ended.
     */
    public boolean sequenceEnd(String sessionId, String observationId) throws ServiceException {
        return sm().sequenceEnd(sessionId, observationId);
    }

    /**
     * Indicate that the sequence or observation has been abandoned for a specific reason.
     */
    public boolean observationAbort(String sessionId, String observationId, String reason) throws ServiceException {
        return sm().observationAbort(sessionId, observationId, reason);
    }

    /**
     * Indicate that the sequence or observation has been paused for a specific reason.
     */
    public boolean observationPause(String sessionId, String observationId, String reason) throws ServiceException {
        return sm().observationPause(sessionId, observationId, reason);
    }

    /**
     * Indicate that the sequence or observation has been stopped for a specific reason.
     */
    public boolean observationStop(String sessionId, String observationId, String reason) throws ServiceException {
        return sm().observationStop(sessionId, observationId, reason);
    }

    /**
     * Indicate that the sequence or observation has been paused for a specific reason.
     */
    public boolean observationContinue(String sessionId, String observationId) throws ServiceException {
        return sm().observationContinue(sessionId, observationId);
    }

    /**
     * Indicates that the observe to collect a dataset has started
     */
    public boolean datasetStart(String sessionId, String observationId, String datasetId, String fileName) throws ServiceException {
        return sm().datasetStart(sessionId, observationId, datasetId, fileName);
    }

    /**
     * Indicates that one of the datasets for the observation has been completed.
     */
    public boolean datasetComplete(String sessionId, String observationId, String datasetId, String fileName) throws ServiceException {
        return sm().datasetComplete(sessionId, observationId, datasetId, fileName);
    }
}
