//
// $Id: DBUpdateService.java 887 2007-07-04 15:38:49Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.event.*;
import edu.gemini.spModel.gemini.plan.NightlyRecord;
import edu.gemini.spModel.obs.ObsExecEventFunctor;
import edu.gemini.spModel.util.NightlyProgIdGenerator;
import edu.gemini.wdba.glue.api.WdbaContext;
import edu.gemini.wdba.glue.api.WdbaGlueException;


import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class responds to session events and updates the database as needed.
 */
public final class DBUpdateService implements Runnable {

    private static final Logger LOG =
            Logger.getLogger(DBUpdateService.class.getName());

    private static final EventLogger EVENT_LOGGER = new EventLogger();

    private static final int QUEUE_CAPACITY = 10000;

    private static final class UnhandledEvent {
        public final ExecEvent event;
        public final CompletableFuture<ExecEvent> future = new CompletableFuture<>();

        public UnhandledEvent(ExecEvent event) {
            this.event = event;
        }
    }

    private final ServiceExecutor exec =
        new ServiceExecutor("DBUpdateService", this);

    private final ArrayBlockingQueue<UnhandledEvent> queue =
        new ArrayBlockingQueue<>(QUEUE_CAPACITY, true);

    private final WdbaContext ctx;

    public DBUpdateService(WdbaContext ctx) {
        this.ctx  = ctx;
    }

    public synchronized void start() {
        exec.start();
    }

    public synchronized void stop() {
        exec.stop();
    }

    public CompletableFuture<ExecEvent> handleEvent(ExecEvent event) throws InterruptedException {

        LOG.info(String.format("%s: enqueueing event: %s", getName(), event));

        // Place (event, future) pair on the blocking work queue.
        final UnhandledEvent ue = new UnhandledEvent(event);
        queue.put(ue);

        // The future will be completed when the job is done, letting the
        // next stage of handling events continue.  It's important that the
        // event be recorded in the database before anything else happens
        // because computations like completed step count depend on it.
        return ue.future;
    }


    // Add the given obs id to nightly log, creating the log if necessary
    // for the current night.
    private void addToNightlyRecord(SPObservationID obsId) {
        final SPProgramID recordId = NightlyProgIdGenerator.getProgramID(NightlyProgIdGenerator.PLAN_ID_PREFIX, ctx.getSite());

        final ISPNightlyRecord nightlyRecordNode;
        try {
            nightlyRecordNode = ctx.getWdbaDatabaseAccessService().getNightlyRecord(recordId);
        } catch (Exception ex) {
            // Messages are logged at lower level
            return;
        }

        final NightlyRecord nightlyRecord = (NightlyRecord) nightlyRecordNode.getDataObject();
        nightlyRecord.addObservation(obsId);
        nightlyRecordNode.setDataObject(nightlyRecord);

        LOG.info("Added observation ID to nightly record: " + obsId.stringValue());
    }

    public String getName() {
        return "DBUpdateService";
    }

    private void doMsgUpdate(ExecEvent event) {

        event.doAction(EVENT_LOGGER);

        Optional<IDBDatabaseService> db = Optional.empty();
        try {
            db = Optional.ofNullable(ctx.getWdbaDatabaseAccessService().getDatabase());
        } catch (WdbaGlueException e) {
            LOG.log(Level.WARNING, "Exception getting database from WDBA Access", e);
        }

        try {

            if (event instanceof ObsExecEvent) {
                final ObsExecEvent obsExecEvent = (ObsExecEvent) event;

                if (event instanceof StartSequenceEvent) {
                    addToNightlyRecord(obsExecEvent.getObsId());
                }

                db.ifPresent(d -> ObsExecEventFunctor.handle(obsExecEvent, d, ctx.getUser()));
            }

        } catch (Throwable ex) {
            LOG.log(Level.INFO, ex.getMessage(), ex);
        }
    }

    // Gets the next event when it becomes available.  Returns None if
    // interrupted.
    private Option<UnhandledEvent> nextEvent() {
        Option<UnhandledEvent> ue = ImOption.empty();
        try {
            ue = ImOption.apply(queue.take());
        } catch (InterruptedException ex) {
            LOG.info("Stopping DBUpdateService");
        }
        return ue;
    }

    public void run() {
        // Loops removing events from the work queue and recording them.
        while (!Thread.currentThread().isInterrupted()) {
            nextEvent().foreach(ue -> {
                try {
                    LOG.info(String.format("%s: start processing event: %s", getName(), ue.event));
                    doMsgUpdate(ue.event);
                    LOG.info(String.format("%s: done processing event: %s", getName(), ue.event));
                    ue.future.complete(ue.event);
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, ex.getMessage(), ex);
                    ue.future.completeExceptionally(ex);
                }
            });
        }
    }

}
