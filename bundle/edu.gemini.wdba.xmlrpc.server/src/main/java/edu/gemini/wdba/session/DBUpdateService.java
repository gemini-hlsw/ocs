//
// $Id: DBUpdateService.java 887 2007-07-04 15:38:49Z gillies $
//

package edu.gemini.wdba.session;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.event.*;
import edu.gemini.spModel.gemini.plan.NightlyRecord;
import edu.gemini.spModel.obs.ObsExecEventFunctor;
import edu.gemini.spModel.util.NightlyProgIdGenerator;
import edu.gemini.wdba.glue.api.WdbaContext;
import edu.gemini.wdba.glue.api.WdbaDatabaseAccessService;
import edu.gemini.wdba.glue.api.WdbaGlueException;


import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class responds to session events and updates the database as needed.
 */
public final class DBUpdateService implements ISessionEventListener, Runnable {

    private static final Logger LOG =
            Logger.getLogger(DBUpdateService.class.getName());

    private static final EventLogger EVENT_LOGGER = new EventLogger();

    private static final int QUEUE_CAPACITY = 10000;



    private final ArrayBlockingQueue<ExecEvent> queue =
        new ArrayBlockingQueue<>(QUEUE_CAPACITY, true);

    private final WdbaContext _context;

    public DBUpdateService(WdbaContext context) {
        assert context != null : "Session Context is null";
        _context = context;
    }

    /**
     * Implements <code>ISessionEventListener</code> to place events on the
     * queue.
     *
     * @param event the execution event to enqueue</code>
     */
    public void sessionUpdate(ExecEvent event) throws InterruptedException {

        LOG.info(String.format("%s: enqueueing event: %s", getName(), event));

        // Place on queue
        queue.put(event);
    }


    // Add the given obs id to nightly log, creating the log if necessary
    // for the current night.
    private void addToNightlyRecord(
        WdbaDatabaseAccessService dbAccess,
        SPObservationID obsId
    ) {
        final Site site = _context.getSite();

        final SPProgramID recordId = NightlyProgIdGenerator.getProgramID(NightlyProgIdGenerator.PLAN_ID_PREFIX, site);

        final ISPNightlyRecord nightlyRecordNode;
        try {
            nightlyRecordNode = dbAccess.getNightlyRecord(recordId);
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

        final Optional<WdbaDatabaseAccessService> dbAccess =
                Optional.ofNullable(_context.getWdbaDatabaseAccessService());

        final Optional<IDBDatabaseService> db;
        db = dbAccess.flatMap(a -> {
            try {
                return Optional.ofNullable(a.getDatabase());
            } catch (WdbaGlueException e) {
                LOG.log(Level.WARNING, "Exception getting database from WDBA Access", e);
                return Optional.empty();
            }
        });

        try {

            if (event instanceof ObsExecEvent) {
                final ObsExecEvent obsExecEvent = (ObsExecEvent) event;

                if (event instanceof StartSequenceEvent) {
                    dbAccess.ifPresent(a -> addToNightlyRecord(a, obsExecEvent.getObsId()));
                }

                db.ifPresent(d -> ObsExecEventFunctor.handle(obsExecEvent, d, _context.user));
            }

        } catch (Throwable ex) {
            LOG.log(Level.INFO, ex.getMessage(), ex);
        }
    }

        // Runnable
    public void run() {
        // Remove from queue and run
        while (true) {
            try {
                final ExecEvent event = queue.take();
                LOG.info(String.format("%s: start processing event: %s", getName(), event));
                doMsgUpdate(event);
                LOG.info(String.format("%s: done processing event: %s", getName(), event));
            } catch (InterruptedException ex) {
                LOG.log(Level.INFO, "Stopping session event consumer: " + ex.getMessage(), ex);
                return;
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

}
