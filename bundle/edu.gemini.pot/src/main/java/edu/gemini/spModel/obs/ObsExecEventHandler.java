package edu.gemini.spModel.obs;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.event.ExecAction;
import edu.gemini.spModel.event.ExecEvent;
import edu.gemini.spModel.event.ObsExecEvent;
import edu.gemini.spModel.event.StartDatasetEvent;
import edu.gemini.spModel.obslog.ObsExecLog;


import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles {@link edu.gemini.spModel.event.ObsExecEvent ObsExecEvent}s, updating
 * the observation and its contents as necessary.
 */
public final class ObsExecEventHandler {
    private static final Logger LOG = Logger.getLogger(ObsExecEventHandler.class.getName());

    public static void handle(ObsExecEvent evt, IDBDatabaseService db)  {
        ISPObservation obs = db.lookupObservationByID(evt.getObsId());
        if (obs == null) {
            LOG.log(Level.WARNING, "Cannot handle event for '" + evt.getObsId() + "'. No observation with this id.");
        } else {
            Action act = new Action(evt, db, obs);
            try {
                evt.doAction(act);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Problem handling observation exec event.", ex);
            }
        }
    }

    private static final class Action implements ExecAction {
        private final ObsExecEvent evt;
        private final IDBDatabaseService db;
        private final ISPObservation obs;

        Action(ObsExecEvent evt, IDBDatabaseService db, ISPObservation obs) {
            this.evt  = evt;
            this.db   = db;
            this.obs  = obs;
        }

        @Override public void startVisit(ExecEvent event) { updateObsLog(); }
        @Override public void endVisit(ExecEvent event) { updateObsLog(); }
        @Override public void slew(ExecEvent event) { updateObsLog(); }
        @Override public void abortObserve(ExecEvent event) { updateObsLog(); }
        @Override public void pauseObserve(ExecEvent event) { updateObsLog(); }
        @Override public void continueObserve(ExecEvent event) { updateObsLog(); }
        @Override public void stopObserve(ExecEvent event) { updateObsLog(); }
        @Override public void overlap(ExecEvent evt) { updateObsLog(); }
        @Override public void startIdle(ExecEvent event) { /* ignore */ }
        @Override public void endIdle(ExecEvent event) { /* ignore */ }
        @Override public void startSequence(ExecEvent event) { updateObsLog(); }
        @Override public void endSequence(ExecEvent event) { updateObsLog(); }

        @Override public void startDataset(ExecEvent event) {
            StartDatasetEvent sde = (StartDatasetEvent) event;
            DatasetLabel    label = sde.getDataset().getLabel();
            updateObsLog(new Some<>(label));
        }

        @Override public void endDataset(ExecEvent event) { updateObsLog(); }

        private void updateObsLog() { updateObsLog(None.instance()); }

        private void updateObsLog(Option<DatasetLabel> label) {
            ObsExecLog.updateObsLog(db, obs.getObservationID(), label, new Some<>(evt));
        }
    }

}
