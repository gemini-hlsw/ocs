//
// $Id: Session.java 872 2007-06-03 21:34:08Z gillies $
//
package edu.gemini.wdba.session;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.event.*;
import edu.gemini.wdba.glue.api.WdbaDatabaseAccessService;
import edu.gemini.wdba.xmlrpc.ServiceException;
import edu.gemini.wdba.shared.QueuedObservation;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Implementation of the OCS Session time-management related functions.
 * <p/>
 * This class receives events from the web service and distributes them to
 * the subscribers.
 *
 * @author K.Gillies
 */
public class Session {
    private static final Logger LOG = Logger.getLogger(Session.class.getName());

    // When synthesizing events use this delay between events
    private static final long EVENT_DELTA = 1L;

    // The delay for closing out a session
    private static final long AUTO_END_VISIT_DELAY = 2 * 60 * 60 * 1000; // 2 hours

    // The EventHandler instance handles events arriving from SessionXmlRpcHandler
    private final EventHandler _sessionEventHandler = new EventHandler();

    private final List<String> _observationIDs = Collections.synchronizedList(new ArrayList<>());

    private final String _sessionID;
    // Interface to database methods
    private final WdbaDatabaseAccessService _dbAccess;
    private final Consumer<ExecEvent> _eventConsumer;

    /**
     * @param sessionID a unique name used to identify the session
     */
    public Session(
        String sessionID,
        WdbaDatabaseAccessService dbAccess,
        Consumer<ExecEvent> eventConsumer
    ) {
        if (sessionID == null) throw new NullPointerException("sessionID");
        if (dbAccess == null) throw new NullPointerException("dbAccess");
        if (eventConsumer == null) throw new NullPointerException("eventConsumer");

        _sessionID     = sessionID;
        _dbAccess      = dbAccess;
        _eventConsumer = eventConsumer;
    }

    public String getSessionID() {
        return _sessionID;
    }

    /**
     * A private method to return ids list.
     * @return a <tt>List</tt> of observation IDs in the session as Strings
     */
    private List<String> _getObservationList() {
        return _observationIDs;
    }

    // This looks through the returned  list to ensure it should remain in the obsId list
    private boolean _checkForId(String obsId, List<QueuedObservation> qobs) {
        for (QueuedObservation qo: qobs) {
            if (obsId.equals(qo.getId().stringValue())) {
                return true;
            }
        }
        return false;
    }

    // Look through the list of current ids.  If any of them have now been deleted, remove them from the
    // internal list
    private void _fixObservationList(List<QueuedObservation> result) {
        List<String> removers = new ArrayList<>();
        for (String obsId: _observationIDs) {
            if (!_checkForId(obsId, result)) {
                LOG.info("Removing deleted observation from session: " + obsId);
                removers.add(obsId);
            }
        }
        if (removers.size() == 0) return;

        _observationIDs.removeAll(removers);
    }

    /**
     * Add the observation ID to this session's ID list.  Adds the observation only if it's not in the session.
     *
     * @param observationID the id of the observation that should be added
     * @return true if the observationID was successfully added.
     * @throws NullPointerException if the observationID is null
     */
    public boolean addObservation(String observationID) {
        if (observationID == null) throw new NullPointerException();
        if (isObservation(observationID)) return false;
        return _getObservationList().add(observationID);
    }

    /**
     * Removes an observation from the session by its observation ID.
     *
     * @param observationID the id of the observation that should be removed.
     * @return <code>true</code> if successful or <code>false</code> if the id
     *         is not found.
     */
    public boolean removeObservation(String observationID) {
        if (observationID == null) throw new NullPointerException();
        return _getObservationList().remove(observationID);
    }

    /**
     * Is there an observation with this id in this session?
     * @param observationID an observationID as a String to use to check for presence in the session
     * @return true if there is an observation with ID observationID in the session
     */
    public boolean isObservation(String observationID) {
        if (observationID == null) throw new NullPointerException();
        return _getObservationList().contains(observationID);
    }


    // A private method to test the
    private List<QueuedObservation> _getCheckedObservationList() throws ServiceException {
        List<QueuedObservation> checkedResult;
        try {
            checkedResult = _dbAccess.getCheckedObservationList(_getObservationList());
        } catch (Exception ex) {
            throw new ServiceException("Check of observation list yielded exception: " + ex);
        }
        // Remove any obs ids that have been deleted somehow
        _fixObservationList(checkedResult);
        return checkedResult;
    }

    /**
     * Return a copy of the current list of observations for this session.
     * This list is run through the <tt>QueuedObservationFunctor</tt> to ensure that all observations exist, and to
     * retrieve the titles.
     *
     * @return an array of the current observation ids.  This might be zero length.
     * @throws ServiceException when database functor for checking observation existence fails
     */
    public String[] getObservations() throws ServiceException {
        List<QueuedObservation> checkedResult = _getCheckedObservationList();

        // At this time, throw away the titles since the public interface does not support it
        List<String> results = new ArrayList<>();
        for (QueuedObservation qobs : checkedResult) {
            results.add(qobs.getId().stringValue());
        }
        return results.toArray(new String[0]);
    }

    /**
     * Returns a <tt>List</tt> of <tt>QueuedObservation</tt> objects for direct use of the OT.
     * @return a List of observation ids and titles as QueuedObservation objects.
     * @throws ServiceException if the functor fails
     */
    public List<QueuedObservation> getObservationsAndTitles() throws ServiceException {
        return _getCheckedObservationList();
    }

    /**
     * Remove all observation ids from the session.
     * @return return true if successful
     */
    public void removeAllObservations() {
        _getObservationList().clear();
    }

    /**
     * Execute an action based upon the Session event.  This is called from SessionXmlRpcHandler
     * @param evt is the {@link SessionEvent} that should be processed by the <tt>Session</tt>.
     * @throws ServiceException if the processing of the event throws a ServiceException.
     */
    public void doEventMsg(SessionEvent evt) throws ServiceException {
        EventMsg msg = evt.getMsg();
        msg.doAction(_sessionEventHandler, evt);
    }

    /**
     * This method is the low level support for firing events to services associated with the session.
     *
     * @param evt the {@link ExecEvent} that is being passed to the associated services.
     */
    private void _fireEvent(ExecEvent evt) {
        _eventConsumer.accept(evt);
    }

    /**
     * OpenObservations is a class that handles the multiple ongoing observations that this Session may have.  It is
     * responsible for figuring out when to open and close visits based upon events
     */
    private class OpenObservations {
        // Indicates the timer can be a daemon
        private final Timer _timer = new Timer(true);

        /**
         * This small class contains the values for a single open observation
         * The datasetInProgress flag is true when a datasetStart event has been received but not an end dataset event
         * The overlap flag is true when this observation was in a dataset and a second observation was started
         * This generally results in the overlapped observation ending as soon as the second observation starts
         * a sequence or a dataset.
         *
         * The OpenObservation class also takes care of ending visits at the end of the night
         * or any time that there is a long pause between activity.  For the most
         * part, for every event that it receives, it just passes it along to the
         * db update service, etc.  However, a few event types are designated
         * potential "terminating" events.  Those are abort, stop, and endSequence.
         * These may be the last event we receive for a long time (because the
         * night ended or whatever).  For these event types a timer is started.
         * When the timer goes off, we generate an automatic EndVisitEvent at the
         * time of the last real event we received.  That closes out the visit.
         * for an open visit
         */
        private class OpenObservation implements ExecAction {
            private TimerTask _task;
            private boolean _datasetInProgress = false;
            private boolean _overlap = false;
            private final SPObservationID _observationID;

            OpenObservation(SPObservationID observationID) {
                _observationID = observationID;
            }

            SPObservationID getObservationID() {
                return _observationID;
            }

            synchronized boolean noDatasetInProgress() {
                return !_datasetInProgress;
            }

            synchronized void setDatasetInProgress(boolean state) {
                _datasetInProgress = state;
            }

            synchronized void setOverlap() {
                _overlap = true;
            }

            synchronized boolean isOverlap() {
                return _overlap;
            }

            private synchronized void _handlePotentialTerminatingEvent(ExecEvent event) {
                // Send the event as normal.
                _handleNormalEvent(event);

                // Start the timer that would close out the current visit if we
                // don't get any more events in a while.  Remember what time we
                // should use for the generated EndVisitEvent.
                final long timestamp = event.getTimestamp() + EVENT_DELTA;
                _task = new TimerTask() {
                    public void run() {
                         // Generate a EndVisit event
                         remove(_observationID);

                         EndVisitEvent eve = new EndVisitEvent(timestamp, _observationID);
                         _handleNormalEvent(eve);
                    }
                };
                _timer.schedule(_task, AUTO_END_VISIT_DELAY);
            }

            private synchronized void _handleNormalEvent(ExecEvent event) {
                // Cancel the timer task since we just got a normal, non
                // potentially visit ending event.
                if (_task != null) _task.cancel();
                _task = null;

                // fire the event
                _fireEvent(event);
            }

            public void abortObserve(ExecEvent event) {
                setDatasetInProgress(false);
                _handlePotentialTerminatingEvent(event);
            }

            public void overlap(ExecEvent event) {
                _handleNormalEvent(event);
            }

            public void pauseObserve(ExecEvent event) {
                _handleNormalEvent(event);
            }

            public void continueObserve(ExecEvent event) {
                _handleNormalEvent(event);
            }

            public void stopObserve(ExecEvent event) {
                _handlePotentialTerminatingEvent(event);
            }

            public void startVisit(ExecEvent event) {
                setDatasetInProgress(false);
                _handleNormalEvent(event);
            }

            public void slew(ExecEvent event) {
                setDatasetInProgress(false);
                _handleNormalEvent(event);
            }

            public void startSequence(ExecEvent event) {
                setDatasetInProgress(false);
                _handleNormalEvent(event);
            }

            public void startDataset(ExecEvent event) {
                setDatasetInProgress(true);
                _handleNormalEvent(event);
            }

            public void endDataset(ExecEvent event) {
                setDatasetInProgress(false);
                _handleNormalEvent(event);
            }

            public void endSequence(ExecEvent event) {
                setDatasetInProgress(false);
                _handlePotentialTerminatingEvent(event);
            }

            public void endVisit(ExecEvent event) {
                setDatasetInProgress(false);
                _handleNormalEvent(event);
            }

            public void startIdle(ExecEvent event) {
                _handleNormalEvent(event);
            }

            public void endIdle(ExecEvent event) {
                _handleNormalEvent(event);
            }
        }

        // A list to hold OpenObservations
        private final List<OpenObservation> _openObservations = new ArrayList<>();

        /**
         * This method is called to generate a specific {@link ExecEvent}.  It also checks for a start and
         * end dataset event and sets it in the open observations.
         *
         * @param evt the {@link ExecEvent} that is to be posted.
         * @param obsID The observation ID of the observation the event belongs to.
         */
        private void _postEvent(ExecEvent evt, SPObservationID obsID) {
            OpenObservation obs = get(obsID);
            evt.doAction(obs);
        }

        /**
         * Return the open observation with observation ID
         * @param obsID the observation id
         * @return the <code>OpenObservation</code> instance
         */
        private OpenObservation get(SPObservationID obsID) {
            synchronized (_openObservations) {
                for (OpenObservation obs : _openObservations) {
                    if (obs.getObservationID().equals(obsID)) return obs;
                }
            }
            return null;
        }

        /**
         * Is the observation currently open
         * @param obsID the observation id
         * @return true if the observation is in the open observation list
         */
        private boolean isOpen(SPObservationID obsID) {
            return get(obsID) != null;
        }

        /**
         * Is the open observation list empty?
         * @return true if the list is empty
         */
        boolean isEmpty() {
            synchronized (_openObservations) {
                return _openObservations.isEmpty();
            }
        }

        /**
         * How many observations are currently open
         * @return  the number of observations that are open
         */
        int getOpenObservationCount() {
            synchronized (_openObservations) {
                return _openObservations.size();
            }
        }

        /**
         * Attempt to remove the observation with observation ID obsID from the open list
         * @param obsID the observation ID
         * @return the <code>OpenObservation</code> with obsID or null if it's not in the list
         */
        private OpenObservation remove(SPObservationID obsID) {
            OpenObservation openObs;
            synchronized (_openObservations) {
                openObs = get(obsID);
                if (openObs == null) return null;

                _openObservations.remove(openObs);
            }
            return openObs;
        }

        /**
         * Add the observation with observation ID obsID to the open list
         * @param obsID the observation ID
         */
        private void add(SPObservationID obsID) {
            synchronized (_openObservations) {
                _openObservations.add(new OpenObservation(obsID));
            }
        }

        /**
         * Convenience method to start a visit using the given time for observation given by observation ID.
         * Note this method also adds the observation to the open observation list
         * @param originalEventTime an event time (note start visit is posted one EVENT_DELTA before the event
         * @param obsID the observation ID
         */
        private void startVisit(long originalEventTime, SPObservationID obsID) {
            // start a new visit on the new ID
            StartVisitEvent nevt = new StartVisitEvent(originalEventTime - EVENT_DELTA, obsID);

            add(obsID);

            // Note this must come after the add so it's there to handle the posting
            _postEvent(nevt, obsID);
        }

        /**
         * Convenience method to end a visit using a given event time and observation ID.
         * This method also removes the observation from the open observation list
         *
         * @param originalEventTime event time to use (NOTE: end visit is posted 2*EVENT_DELTA behind this time
         * @param obsID            observation ID of the observation with a visit ending
         */
        private void endVisit(long originalEventTime, SPObservationID obsID) {
            // start a new visit on the new ID
            EndVisitEvent nevt = new EndVisitEvent(originalEventTime - 2 * EVENT_DELTA, obsID);
            _postEvent(nevt, obsID);

            remove(obsID);
        }

        /**
         * Convenience method to set the overlap for the observation with ID obsID and generate an OverlapEvent
         * @param originalEventTime the time of the event that causes an overlap
         * @param obs the <tt>OpenObservation</tt> that is being overlapped
         */
        private void setOverlap(long originalEventTime, OpenObservation obs) {
            if (obs.isOverlap()) return; // already overlapped
            SPObservationID obsID = obs.getObservationID();

            // Log this interesting event!
            LOG.info("OVERLAP: Observation has overlap set: " + obsID);
            OverlapEvent nevt = new OverlapEvent(originalEventTime, obsID);
            _postEvent(nevt, obsID);
            obs.setOverlap();
        }

        /**
         * Convenience method to test an event to be one that starts data acquisition.  These events are:
         * <code>StartSequenceEvent</code> and <code>StartDatasetEvent</code>.
         * @param evt the event to test for start-ness
         * @return true if it's a start event, else false
         */
        private boolean isStartEvent(ExecEvent evt) {
            return (evt instanceof StartSequenceEvent || evt instanceof StartDatasetEvent);
        }

        /**
         * Method to handle overlap issues when more than one observation is open.
         * @param evtObsID the observaton ID of the observation that received the event
         * @param evt the event that was received and resulting in the overlap check
         */
        private void _handleOverlap(SPObservationID evtObsID, ExecEvent evt) {
            // Get a copy of the list of open observations
            List<OpenObservation> opens;
            synchronized (_openObservations) {
                opens = new ArrayList<>(_openObservations);
            }

            for (OpenObservation obs : opens) {
                SPObservationID obsID = obs.getObservationID();

                // We're only concerned with closing out overlapped observations
                // in this method.
                if (!obs.isOverlap()) continue;

                // If this is an event for this observation, then we don't need
                // to send end visit for it.
                if (evtObsID.equals(obsID)) continue;

                // We only care about start events in other observations.
                if (!isStartEvent(evt)) continue;

                // Definitely close out this overlapped observation if it
                // doesn't have a dataset in progress.  Even if it does have
                // a dataset "in progress", if this is a startDataset for
                // another obs something has gone wrong since there can't be
                // two datasets being collected at once.  So close out this
                // observation.
                if (obs.noDatasetInProgress() || (evt instanceof StartDatasetEvent)) {
                    endVisit(evt.getTimestamp(), obs.getObservationID());
                }
            }
        }

        /**
         * This method handles ending visits.  The algorithm is stated below.
         *
         * @param evtObsID the {@link SPObservationID} of the observation associated with the new event
         * @param evt      the {@link ExecEvent} to send to listeners
         */
        private void _handleClosing(ExecEvent evt, SPObservationID evtObsID) {
            // Get a copy of the list of open observations
            List<OpenObservation> opens;
            synchronized (_openObservations) {
                opens = new ArrayList<>(_openObservations);
            }

            // This bit considers whether a new visit is needed (is a dataset in progress?)
            for (OpenObservation obs : opens) {
                // If an open observation has no dataset ongoing, close it and start a new one
                if (obs.noDatasetInProgress()) {
                    endVisit(evt.getTimestamp(), obs.getObservationID());
                } else {
                    // It does have a dataset open so mark it as having an overlap
                    setOverlap(evt.getTimestamp(), obs);
                }
            }
            // Start a visit for the new obsID
            startVisit(evt.getTimestamp(), evtObsID);
        }

        /**
         * Check visit handles whether the current visit should be closed before starting a new one.
         * It does the following:
         * <ul>
         * <li>If the current observation ID and the next observation ID are the same, it does nothing and returns.
         * <li>If the currentID is not null, it ends the current visit for the current ID with a new event
         * <li>and starts a new visit for the nextID
         * <li>and sets the currentID to the nextID
         * </ul>
         *
         * @param evtObsID the observation ID in the next event as an <code>SPObservationID</code>
         * @param evt      the <code>ExecEvent</code> that should be sent to the database after sifting through
         *                 issues related to visits.
         */
        void checkVisit(SPObservationID evtObsID, ExecEvent evt) {
            // if list is empty start a visit and post original event
            if (isEmpty()) {
                startVisit(evt.getTimestamp(), evtObsID);
                // Post the original event
                _postEvent(evt, evtObsID);
                return;
            }

            // Get the number of open observations
            int openCount = getOpenObservationCount();

            // If the event is for an observation that is not already open, decide if a visit should be closed or
            // if a new visit is needed.
            if (!isOpen(evtObsID)) {
                // So it's not on the list of open observations yet,
                _handleClosing(evt, evtObsID);
                // And post the original event
                _postEvent(evt, evtObsID);
                return;
            }

            // We are here so we know the event is for an observation that is already on the open list
            // If there is only one open, just return since there can be no overlaps
            _postEvent(evt, evtObsID);
            if (openCount == 1) return;

            // If we are here, there is more than one observation open so we see if we can close one.
            _handleOverlap(evtObsID, evt);
        }
    }

    /**
     * This class provides the interface needed to accept messages from {@link SessionXmlRpcHandler}.
     */
    public final class EventHandler extends EventMsg.AbstractAction {
        // This is the session's list of observation's underway
        private final OpenObservations _openObservations = new OpenObservations();

        /**
         * Performs the action associated with OBSERVATION_START event.
         */
        public void observationStart(SessionEvent evt) {
            SPObservationID evtObsID = evt.getObservationID();

            // observationStart can only come from the TCC as a slew event
            SlewEvent stEvent = new SlewEvent(evt.getTime(), evtObsID);
            _openObservations.checkVisit(evtObsID, stEvent);
        }

        /**
         * Performs the action associated with a SEQUENCE_START event.
         */
        public void observationEnd(SessionEvent evt) throws ServiceException {

            throw new ImproperEventException("observation End can't be received but it has been!");
        }

        /**
         * Performs the action associated with a SEQUENCE_START event.
         */
        public void sequenceStart(SessionEvent evt) {
            SPObservationID evtObsID = evt.getObservationID();

            // Post a sequenceStart event for the observation
            StartSequenceEvent stEvent = new StartSequenceEvent(evt.getTime(), evtObsID);
            _openObservations.checkVisit(evtObsID, stEvent);
        }

        /**
         * Performs the action associated with a SEQUENCE_END status.
         */
        public void sequenceEnd(SessionEvent evt) {
            SPObservationID evtObsID = evt.getObservationID();

            // Post an observationStart event for the observation
            EndSequenceEvent stEvent = new EndSequenceEvent(evt.getTime(), evtObsID);
            _openObservations.checkVisit(evtObsID, stEvent);

            // Remove the observation from the queue.
            removeObservation(evtObsID.stringValue());
        }

        /**
         * Performs the action associated with a OBSERVATION_ABORT status.
         */
        public void observationAbort(SessionEvent evt) {
            SPObservationID evtObsID = evt.getObservationID();

            // Post an observationAbort event for the observation
            ObservationAbortEvent oaEvt = (ObservationAbortEvent) evt;
            AbortObserveEvent stEvent = new AbortObserveEvent(evt.getTime(), evtObsID, oaEvt.getReason());
            _openObservations.checkVisit(evtObsID, stEvent);
        }

        /**
         * Performs the action associated with a OBSERVATION_PAUSE status.
         */
        public void observationPause(SessionEvent evt) {
            SPObservationID evtObsID = evt.getObservationID();

            // Post an observationPause event for the observation
            ObservationPauseEvent oaEvt = (ObservationPauseEvent) evt;
            PauseObserveEvent stEvent = new PauseObserveEvent(evt.getTime(), evtObsID, oaEvt.getReason());
            _openObservations.checkVisit(evtObsID, stEvent);
        }

        /**
         * Performs the action associated with a OBSERVATION_CONTINUE status.
         */
        public void observationContinue(SessionEvent evt) {
            SPObservationID evtObsID = evt.getObservationID();

            // Post an observationContinue event for the observation
            ContinueObserveEvent stEvent = new ContinueObserveEvent(evt.getTime(), evtObsID);
            _openObservations.checkVisit(evtObsID, stEvent);
        }

        /**
         * Performs the action associated with a OBSERVATION_STOP status.
         */
        public void observationStop(SessionEvent evt) {
            SPObservationID evtObsID = evt.getObservationID();

            // Post an observationStop event for the observation
            StopObserveEvent stEvent = new StopObserveEvent(evt.getTime(), evtObsID);
            _openObservations.checkVisit(evtObsID, stEvent);
        }

        private DatasetLabel _makeDatasetLabel(String datasetID) throws ServiceException {
            DatasetLabel label;
            try {
                label = new DatasetLabel(datasetID);
            } catch (java.text.ParseException ex) {
                throw new ServiceException("Improper datasetID found: " + datasetID);
            }
            return label;
        }

        private Dataset _makeDataset(long time, String datasetID, String fileName) throws ServiceException {
            DatasetLabel label = _makeDatasetLabel(datasetID);
            // Retarding the time stamp of the dataset so it will be a bit behind the StartDatasetEvent
            return new Dataset(label, fileName, time - EVENT_DELTA);
        }

        /**
         * Performs the action associated with a DATASET_START status.
         */
        public void datasetStart(SessionEvent evt) throws ServiceException {
            SPObservationID evtObsID = evt.getObservationID();

            // Post an datasetStart event for the observation
            DatasetStartEvent oaEvt = (DatasetStartEvent) evt;
            Dataset ds = _makeDataset(oaEvt.getTime(), oaEvt.getDataLabel(), oaEvt.getFileName());

            StartDatasetEvent stEvent = new StartDatasetEvent(evt.getTime(), ds);
            _openObservations.checkVisit(evtObsID, stEvent);
        }

        /**
         * Performs the action associated with a DATASET_COMPLETE status.
         */
        public void datasetComplete(SessionEvent evt) throws ServiceException {
            SPObservationID evtObsID = evt.getObservationID();

            // Post an datasetStart event for the observation
            DatasetCompleteEvent oaEvt = (DatasetCompleteEvent) evt;

            DatasetLabel dsLabel = _makeDatasetLabel(oaEvt.getDataLabel());
            EndDatasetEvent stEvent = new EndDatasetEvent(evt.getTime(), dsLabel);
            _openObservations.checkVisit(evtObsID, stEvent);
        }

    }


}
