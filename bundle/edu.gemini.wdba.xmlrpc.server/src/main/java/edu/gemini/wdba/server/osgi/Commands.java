package edu.gemini.wdba.server.osgi;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.event.*;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obslog.ObsLog;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A command to simulate a visit with datasets.
 */
final class Commands {
    private final ServiceTracker<IDBDatabaseService, IDBDatabaseService> tracker;

    Commands(ServiceTracker<IDBDatabaseService, IDBDatabaseService> tracker) {
        this.tracker = tracker;
    }

    private IDBDatabaseService db() {
        final IDBDatabaseService db = tracker.getService();
        if (db == null) throw new IllegalStateException("no database available");
        return db;
    }

    public String simWackyVisit(final String obsId) throws Throwable {
        final Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    doSimWackyVisit(obsId);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });
        t.setDaemon(true);
        t.start();

        return "simulating visit for " + obsId;

    }

    private void doSimWackyVisit(String obsId) throws Throwable {
        final SPObservationID oid = new SPObservationID(obsId.trim());
        final ISPObservation obs = db().lookupObservationByID(oid);
        if (obs == null) throw new IllegalStateException("Could not find an observation with id '" + obsId + "'");

        addEvent(oid, null, new StartVisitEvent(System.currentTimeMillis(), oid));
        addEvent(oid, null, new StartSequenceEvent(System.currentTimeMillis(), oid));

        addDataset(oid, 73);
        addDataset(oid,  3);
        addDataset(oid,  1);
        addDataset(oid,  3);
        addDataset(oid,  2);
        addDataset(oid, -4);
        addDataset(oid,  5);
        addDataset(oid,  8);

        addEvent(oid, null, new EndSequenceEvent(System.currentTimeMillis(), oid));
        addEvent(oid, null, new EndVisitEvent(System.currentTimeMillis(), oid));
    }


    public String simVisit(String obsId) throws Throwable {
        return simVisit(obsId, 10);
    }

    public String simVisit(final String obsId, final int count) throws Throwable {

        final Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    doSimVisit(obsId, count);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });
        t.setDaemon(true);
        t.start();

        return "simulating visit for " + obsId;
    }

    private ISPObservation obs(SPObservationID oid) {
        final ISPObservation obs = db().lookupObservationByID(oid);
        if (obs == null) throw new IllegalStateException("Could not find an observation with id '" + oid + "'");
        return obs;
    }

    private void doSimVisit(String obsId, int count) throws Throwable {
        final SPObservationID oid = new SPObservationID(obsId.trim());
        final ISPObservation obs = db().lookupObservationByID(oid);
        if (obs == null) throw new IllegalStateException("Could not find an observation with id '" + obsId + "'");

        addEvent(oid, null, new StartVisitEvent(System.currentTimeMillis(), oid));
        addEvent(oid, null, new StartSequenceEvent(System.currentTimeMillis(), oid));

        for (int i=0; i<count; ++i) addDataset(oid);

        addEvent(oid, null, new EndSequenceEvent(System.currentTimeMillis(), oid));
        addEvent(oid, null, new EndVisitEvent(System.currentTimeMillis(), oid));
    }

    private void addDataset(SPObservationID oid) throws Exception {
        // Get a copy of the data object.
        final ObsLog log = ObsLog.getIfExists(obs(oid));
        final int next = (log == null) ? 1 : log.getDatasetLabels().size() + 1;
        addDataset(oid, next);
    }

    private void addDataset(SPObservationID oid, int index) throws Exception {
        final DatasetLabel lab = new DatasetLabel(oid, index);

        final Dataset dataset = new Dataset(lab, lab.toString(), System.currentTimeMillis());
        addEvent(oid, lab, new StartDatasetEvent(System.currentTimeMillis(), dataset));
        addEvent(oid, lab, new EndDatasetEvent(System.currentTimeMillis(), dataset.getLabel()));

    }

    private void addEvent(SPObservationID oid, final DatasetLabel lab, final ObsExecEvent evt) throws Exception {
        System.out.println("Adding event: "+ evt);

        ObsExecLog.updateObsLog(db(), oid, ImOption.apply(lab), ImOption.apply(evt));
//        ObsLog.update(db(), oid, new ObsLog.UpdateOp() {
//            @Override public void apply(ISPObservation obs, ObsLog log) {
//                log.getExecRecord().addEvent(evt, new DefaultConfig());
//            }
//        });

        // Sleep a while so it's not totally nuts
        Thread.sleep(2000);
    }
}
