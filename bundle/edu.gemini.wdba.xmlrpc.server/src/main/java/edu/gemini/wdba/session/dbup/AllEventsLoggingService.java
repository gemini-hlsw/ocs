//
// $Id: AllEventsLoggingService.java 756 2007-01-08 18:01:24Z gillies $
//

package edu.gemini.wdba.session.dbup;

import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.event.*;
import edu.gemini.wdba.session.AbstractSessionEventConsumer;
import edu.gemini.wdba.session.ISessionEventListener;
import edu.gemini.wdba.session.ISessionEventProducer;
import edu.gemini.wdba.glue.api.WdbaGlueException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This class responds to session events and writes a log message using LOG4J.
 */
public class AllEventsLoggingService extends AbstractSessionEventConsumer implements ISessionEventListener {

    private static final Logger LOG = Logger.getLogger(AllEventsLoggingService.class.getName());
    private  final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");

    private UpdateAction _action = new UpdateAction();

    public AllEventsLoggingService(ISessionEventProducer sep) {
        super(sep);
        Thread t = new Thread(this, "AllEventsLogging");
        t.start();
    }

    private String _doFormatUTCTime(long time) {
        Date date = new Date(time);
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        return DATE_FORMAT.format(date);
    }

    private void _logReasonEvent(StartIdleEvent sevt) {
        LOG.info(_doFormatUTCTime(sevt.getTimestamp()) + ':' + sevt.getName() + ':' + sevt.getReason());
    }

    private void _logEndIdleEvent(EndIdleEvent sevt) {
        LOG.info(_doFormatUTCTime(sevt.getTimestamp()) + ':' + sevt.getName());
    }

    private void _logObsExecEvent(ObsExecEvent sevt) {
        LOG.info(_doFormatUTCTime(sevt.getTimestamp()) + ':' + sevt.getName() + ':' + sevt.getObsId().stringValue());
    }

    private void _logStartDatasetEvent(StartDatasetEvent sevt) {
        Dataset ds = sevt.getDataset();
        LOG.info(_doFormatUTCTime(sevt.getTimestamp()) + ':' + sevt.getName() + ':' + ds.getLabel().toString() + ':' + ds.getDhsFilename());
    }

    private class UpdateAction implements ExecAction {

        public void startVisit(ExecEvent evt) {
            LOG.info("--------------------");
            _logObsExecEvent((StartVisitEvent) evt);
        }

        public void slew(ExecEvent evt) {
            _logObsExecEvent((SlewEvent) evt);
        }

        public void endVisit(ExecEvent evt) {
            _logObsExecEvent((EndVisitEvent) evt);
        }

        public void startIdle(ExecEvent evt) {
            _logReasonEvent((StartIdleEvent) evt);
        }

        public void endIdle(ExecEvent evt) {
            _logEndIdleEvent((EndIdleEvent) evt);
        }

        public void startSequence(ExecEvent evt) {
            StartSequenceEvent sevt = (StartSequenceEvent) evt;
            _logObsExecEvent(sevt);
        }

        public void endSequence(ExecEvent evt) {
            _logObsExecEvent((EndSequenceEvent) evt);
        }

        public void abortObserve(ExecEvent evt) {
            AbortObserveEvent sevt = (AbortObserveEvent) evt;
            LOG.info(_doFormatUTCTime(sevt.getTimestamp()) + ':' + sevt.getName() + ':' + sevt.getReason());
        }

        public void overlap(ExecEvent evt) {
            _logObsExecEvent((OverlapEvent) evt);
        }

        public void pauseObserve(ExecEvent evt) {
            PauseObserveEvent sevt = (PauseObserveEvent) evt;
            LOG.info(_doFormatUTCTime(sevt.getTimestamp()) + ':' + sevt.getName() + ':' + sevt.getReason());
        }

        public void continueObserve(ExecEvent evt) {
            _logObsExecEvent((ContinueObserveEvent) evt);
        }

        public void stopObserve(ExecEvent evt) {
            _logObsExecEvent((StopObserveEvent) evt);
        }

        public void startDataset(ExecEvent evt) {
            _logStartDatasetEvent((StartDatasetEvent) evt);
        }

        public void endDataset(ExecEvent evt) {
            EndDatasetEvent sevt = (EndDatasetEvent) evt;
            LOG.info(_doFormatUTCTime(sevt.getTimestamp()) + ':' + sevt.getName() + ':' + sevt.getDatasetLabel().toString());
        }
    }

    public void doMsgUpdate(ExecEvent evt) throws WdbaGlueException {
        try {
            evt.doAction(_action);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
