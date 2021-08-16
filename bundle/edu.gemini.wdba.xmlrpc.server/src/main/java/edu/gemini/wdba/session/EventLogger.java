//
// $Id: AllEventsLoggingService.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.event.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * This class responds to session events and writes a log message using LOG4J.
 */
public final class EventLogger implements ExecAction {

    private static final Logger LOG = Logger.getLogger(EventLogger.class.getName());

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    private static String formatTime(ExecEvent event) {
        return DATE_FORMAT.format(Instant.ofEpochMilli(event.getTimestamp()));
    }

    private static void logEvent(ExecEvent event) {
        LOG.info(String.format("%s:%s", formatTime(event), event.getName()));
    }

    private static void logEvent(ExecEvent event, String detail) {
        LOG.info(String.format("%s:%s:%s", formatTime(event), event.getName(), detail));
    }

    private void logObsExecEvent(ObsExecEvent event) {
        logEvent(event, event.getObsId().stringValue());
    }

    public void startVisit(ExecEvent event) {
        LOG.info("--------------------");
        logObsExecEvent((StartVisitEvent) event);
    }

    public void slew(ExecEvent event) {
        logObsExecEvent((SlewEvent) event);
    }

    public void endVisit(ExecEvent event) {
        logObsExecEvent((EndVisitEvent) event);
    }

    public void startIdle(ExecEvent event) {
        logEvent(event, ((StartIdleEvent) event).getReason());
    }

    public void endIdle(ExecEvent event) {
        logEvent(event);
    }

    public void startSequence(ExecEvent event) {
        logObsExecEvent((StartSequenceEvent) event);
    }

    public void endSequence(ExecEvent event) {
        logObsExecEvent((EndSequenceEvent) event);
    }

    public void abortObserve(ExecEvent event) {
        logEvent(event, ((AbortObserveEvent) event).getReason());
    }

    public void overlap(ExecEvent event) {
        logObsExecEvent((OverlapEvent) event);
    }

    public void pauseObserve(ExecEvent event) {
        logEvent(event, ((PauseObserveEvent) event).getReason());
    }

    public void continueObserve(ExecEvent event) {
        logObsExecEvent((ContinueObserveEvent) event);
    }

    public void stopObserve(ExecEvent event) {
        logObsExecEvent((StopObserveEvent) event);
    }

    public void startDataset(ExecEvent evt) {
        final Dataset ds = ((StartDatasetEvent) evt).getDataset();
        logEvent(evt, ds.getLabel().toString() + ":" + ds.getDhsFilename());
    }

    public void endDataset(ExecEvent event) {
        logEvent(event, ((EndDatasetEvent) event).getDatasetLabel().toString());
    }
}
