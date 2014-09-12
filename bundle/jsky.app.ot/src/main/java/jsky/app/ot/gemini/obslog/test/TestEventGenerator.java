package jsky.app.ot.gemini.obslog.test;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.event.*;
import edu.gemini.spModel.obslog.ObsLog;

/**
 * A test program for testing interaction in the obslog gui while events are arriving.
 * @author rnorris (hacked by swalker)
 * @version $Id$
 */
public final class TestEventGenerator implements Runnable {
    private final IDBDatabaseService odb;
    private final SPObservationID obsId;

    public TestEventGenerator(IDBDatabaseService odb, SPObservationID obsId) {
        this.odb   = odb;
        this.obsId = obsId;
    }

	private void addEvent(final ObsExecEvent event) {
		try {
            System.out.println("Adding event: "+ event);

			// Get a copy of the data object.
            ObsLog.update(odb, obsId, new ObsLog.UpdateOp() {
                @Override public void apply(ISPObservation obs, ObsLog log) {
                    // Now add the event and update the UI.
                    log.getExecRecord().addEvent(event, new DefaultConfig());
                }
            });

			// Sleep a while so it's not totally nuts
			Thread.sleep(2000);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

    public void run() {
		int index = 0;

		// Now add some visits, sequences, and data sets.
		addEvent(new StartVisitEvent(System.currentTimeMillis(), obsId));
		addEvent(new StartSequenceEvent(System.currentTimeMillis(), obsId));

		// Add a good one.
		{
			final Dataset dataset = new Dataset(new DatasetLabel(obsId, ++index), "GOOD-" + index, System.currentTimeMillis());
			addEvent(new StartDatasetEvent(System.currentTimeMillis(), dataset));
			addEvent(new EndDatasetEvent(System.currentTimeMillis(), dataset.getLabel()));
		}

		// Add a bad one.
		{
			final Dataset dataset = new Dataset(new DatasetLabel(obsId, ++index), "BAD-" + index, System.currentTimeMillis());
			addEvent(new StartDatasetEvent(System.currentTimeMillis(), dataset));
//			addEvent(new EndDatasetEvent(System.currentTimeMillis(), dataset.getLabel()));
		}

		for (int i = 0; i < 10; i++) {

		// And another good one
		{
			final Dataset dataset = new Dataset(new DatasetLabel(obsId, ++index), "GOOD-" + index, System.currentTimeMillis());
			addEvent(new StartDatasetEvent(System.currentTimeMillis(), dataset));
			addEvent(new EndDatasetEvent(System.currentTimeMillis(), dataset.getLabel()));
		}

		}

		addEvent(new EndSequenceEvent(System.currentTimeMillis(), obsId));
		addEvent(new EndVisitEvent(System.currentTimeMillis(), obsId));
	}

    public static void test(IDBDatabaseService odb, SPObservationID obsId) {
        final TestEventGenerator teg = new TestEventGenerator(odb, obsId);
        final Thread t = new Thread(teg);
        t.setDaemon(true);
        t.start();
    }
}
