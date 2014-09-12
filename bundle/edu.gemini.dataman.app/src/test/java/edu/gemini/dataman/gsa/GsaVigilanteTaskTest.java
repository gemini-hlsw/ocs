//
// $
//

package edu.gemini.dataman.gsa;

import edu.gemini.dataman.context.TestDatamanConfig;
import edu.gemini.dataman.context.TestDatamanContext;
import edu.gemini.dataman.gsa.query.*;
import edu.gemini.dataman.test.TestProgramBuilder;
import edu.gemini.dataman.util.DatamanFileUtil;
import edu.gemini.dataman.util.DatasetCommandProcessor;
import edu.gemini.datasetrecord.DatasetRecordEvent;
import edu.gemini.datasetrecord.DatasetRecordListener;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.dataset.*;
import edu.gemini.util.security.principal.StaffPrincipal;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.*;


//
// NOTICE: these test cases result in the GsaVigilanteTask doing file
// transfers as specified in the TestDatamanConfig.  It makes the directories
// it needs, but uses sftp and ssh in the process of copying.  So, in order
// for these tests to work, it needs the username and password of the person
// running the test.
//
// These can be specified using the system properties:
//
//   edu.gemini.dataman.context.user
//   edu.gemini.dataman.context.password
//


/**
 * Test cases for the {@link GsaVigilanteTask}.
 */

@Ignore
public class GsaVigilanteTaskTest {

    // Let's test as superuser
    private Set<Principal> user = Collections.<Principal>singleton(StaffPrincipal.Gemini());

    // Listens for changes to dataset records that we anticipate will be made
    // by the GsaVigilanteTask
    private static final class UpdateListener implements DatasetRecordListener {
        Map<DatasetLabel, List<GsaState>> changeMap =
                                    new HashMap<DatasetLabel, List<GsaState>>();

        public synchronized void datasetModified(DatasetRecordEvent evt) {
            DatasetRecord newRec = evt.getNewVersion();
            List<GsaState> stateList = changeMap.get(newRec.getLabel());
            if (stateList == null) {
                stateList = new ArrayList<GsaState>();
                changeMap.put(newRec.getLabel(), stateList);
            }
            stateList.add(newRec.exec.gsaState);

            notifyAll();
        }

        private GsaState getCurrentState(DatasetLabel label) {
            List<GsaState> stateList = changeMap.get(label);
            if (stateList == null) return null;
            return stateList.get(stateList.size()-1);
        }

        private boolean isSubList(List<GsaState> actual, List<GsaState> expected) {
            if (actual == null) return true;
            if (actual.size() > expected.size()) return false;
            return expected.subList(0, actual.size()).equals(actual);
        }

        // Waits for the dataset with the given label to cycle through the given
        // states.  If it doesn't happen before timeout milliseconds, or if it
        // cycles through an unxpected sequence, then false is returned.
        synchronized boolean waitForUpdates(DatasetLabel label, GsaState[] states, long timeout) {
            long now  = System.currentTimeMillis();
            long then = now + timeout;

            List<GsaState> expectList = Arrays.asList(states);
            List<GsaState> actualList = changeMap.get(label);

            while ((now < then) && isSubList(actualList, expectList) && !expectList.equals(actualList)) {
                try {
                    wait(then - now);
                    now = System.currentTimeMillis();
                    actualList = changeMap.get(label);
                } catch (InterruptedException ex) {
                    return false;
                }
            }

            return expectList.equals(actualList);
        }
    }

    private TestDatamanContext context;
    private UpdateListener listener = new UpdateListener();

    private DatasetExecRecord record1_1_1;


    private TestProgramBuilder prog1;

    @Before
    public void setUp() throws Exception {
        context = new TestDatamanContext();

        // Watch for changes to datasets in the database.  The GsaVigilanteTask
        // updates the GsaState as it does its work.  We'll be checking to see
        // that the expected transitions have happened.
        context.getDatasetRecordService().addListener(listener);

        // Handles updating dataset records in the database.
        DatasetCommandProcessor.INSTANCE.start();

        // Make a dummy program and observation.
        IDBDatabaseService testOdb = context.getTestOdb();

        SPProgramID progId1 = SPProgramID.toProgramID("GS-1969B-Q-1");
        SPObservationID obsId1_1 = new SPObservationID(progId1, 1);
        DatasetLabel lab = new DatasetLabel(obsId1_1, 1);

        record1_1_1 = new DatasetExecRecord(new Dataset(lab, "S19690815S0001.fits", System.currentTimeMillis()));

        prog1   = new TestProgramBuilder(testOdb, progId1);
        prog1.addObservation();
    }

    @After
    public void tearDown() throws Exception {
        DatasetCommandProcessor.INSTANCE.stop();
        context.cleanup();
    }

    private void singleDatasetTest(GsaState startState, GsaState[] expectedTransitions) throws Exception {
        record1_1_1 = record1_1_1.withGsaState(startState);
        prog1.putDataset(record1_1_1);

        // Run the vigilante task to update any datasets that it can.
        (new GsaVigilanteTask(context, user)).run();

        // Wait for any actions it may have initiated to finish.
        DatasetLabel lab = record1_1_1.getLabel();
        assertTrue(listener.waitForUpdates(lab, expectedTransitions, 10000));
        DatasetCommandProcessor.INSTANCE.waitUntilFinished(lab, 10000);

        // Make sure that more updates haven't come in.
        assertTrue(listener.waitForUpdates(lab, expectedTransitions, 0));
    }

    // Writes the MultiXferQuery results to fool the GsaVigilante into thinking
    // the file has the given status.
    private void writeXferQueryResults(XferQueryBase.Status.Code code, File f) throws IOException {
        XferQueryBase.Status status;
        status = new XferQueryBase.Status(f.getName(), f.lastModified(), code, "");
        MultiXferQueryTest.writeXferStatus((TestDatamanConfig) context.getConfig(), status);
    }

    // Writes the MultiXferQuery results to fool the GsaVigilante into thinking
    // the file has the given status.
    private void writeXferQueryResults(XferQueryBase.Status.Code code, String filename) throws IOException {
        XferQueryBase.Status status;
        status = new XferQueryBase.Status(filename, 0, XferQueryBase.Status.Code.notFound, "");
        MultiXferQueryTest.writeXferStatus((TestDatamanConfig) context.getConfig(), status);
    }

    /**
     * Tests a trip through the GsaVigilanteTask in which a dataset is not
     * updated.
     */
    @Test public void testEmptyQuery() throws Exception {
        prog1.putDataset(record1_1_1);
        (new GsaVigilanteTask(context, user)).run();
        DatasetLabel lab = record1_1_1.getLabel();
        assertTrue(listener.waitForUpdates(lab, new GsaState[] { GsaState.NONE }, 1000));
    }

    /**
     * Tests a failure copying. <em>EXPECT A FEW EXCEPTIONS IN THE LOG.</em>
     * It will try to copy a file that doesn't exist to the gsa and to the
     * base.  That will throw exceptions, which is expected.
     */
    @Test public void testPending_None() throws Exception {
        GsaState[] expected = new GsaState[] {
                GsaState.PENDING,       // Starts life in PENDING state.
                GsaState.NONE,          // VigilanteTask moves it to NONE
        };

        // Write a query result which indicates the file isn't in the e-transfer
        // system.
        String filename = record1_1_1.getDataset().getDhsFilename();
        writeXferQueryResults(XferQueryBase.Status.Code.notFound, filename);

        // Will try to copy the file, but since we never created one in the
        // working dir, the dataset record will end up in the COPY_FAILED state.
        singleDatasetTest(GsaState.PENDING, expected);
    }

    /**
     * Tests a normal new dataset copy.  The GSA doesn't know anything about
     * the dataset beforehand.
     */
    @Test public void testPending_Queued_New() throws Exception {
        // Write the file in the working store.
        String filename = record1_1_1.getDataset().getDhsFilename();
        File f = new File(context.getConfig().getWorkDir(), filename);
        assertTrue(f.createNewFile());

        // Run the test.
        GsaState[] expected = new GsaState[] {
                GsaState.PENDING,       // Starts life in PENDING state.
                GsaState.COPYING,       // VigilanteTask moves it to COPYING
                GsaState.VERIFYING,     // Running the verify script
                GsaState.QUEUED,        // Waiting GSA
        };

        // Write a query result which indicates the file isn't in the e-transfer
        // system.  That will allow it to be copied.
        writeXferQueryResults(XferQueryBase.Status.Code.notFound, filename);

        singleDatasetTest(GsaState.PENDING, expected);

        // Make sure the file is really queued.
        f = new File(context.getConfig().getGsaXferConfig().getDestDir(), filename);
        assertTrue(f.exists());
    }

    /**
     * Tests the case where the pending dataset can't be copied because we
     * cannot determine the e-transfer status.
     */
    @Test public void testPending_NoAdvance_Unknown() throws Exception {
        // Run the test.
        GsaState[] expected = new GsaState[] {
                GsaState.PENDING,       // Starts life in PENDING state and doesn't move
        };
        singleDatasetTest(GsaState.PENDING, expected);
    }

    /**
     * Tests the case where the pending dataset can't be copied because we
     * cannot determine the e-transfer status.
     */
    @Test public void testPending_StillQueued() throws Exception {
        // Write the file in the working store.
        String filename = record1_1_1.getDataset().getDhsFilename();
        File f = new File(context.getConfig().getWorkDir(), filename);
        assertTrue(f.createNewFile());

        // Write the query result we're expecting in the right place so that
        // the test query URL will be fooled into thinking it got that result
        // from the GSA.
        writeXferQueryResults(XferQueryBase.Status.Code.pickup, f);

        // Run the test.
        GsaState[] expected = new GsaState[] {
                GsaState.PENDING,       // Starts life in PENDING state and doesn't move
        };
        singleDatasetTest(GsaState.PENDING, expected);
    }

    /**
     * Tests the case where the pending dataset can't be copied because an
     * earlier version is being transferred by the GSA.
     */
    @Test public void testPending_Pending_InXferProcess() throws Exception {
        // Write the file in the working store.
        String filename = record1_1_1.getDataset().getDhsFilename();
        File f = new File(context.getConfig().getWorkDir(), filename);
        assertTrue(f.createNewFile());

        // Write the query result we're expecting in the right place so that
        // the test query URL will be fooled into thinking it got that result
        // from the GSA.
        writeXferQueryResults(XferQueryBase.Status.Code.transferring, f);

        // Run the test.
        GsaState[] expected = new GsaState[] {
                GsaState.PENDING,       // Starts life in PENDING state and doesn't move
        };
        singleDatasetTest(GsaState.PENDING, expected);
    }

    /**
     * Tests the case where a dataset was already accepted, but now found to
     * have a PENDING state.  It should be transferred again so it will wind up
     * in a QUEUED state.
     */
    @Test public void testPending_Queued_WasAccepted() throws Exception {
        // Write the file in the working store.
        String filename = record1_1_1.getDataset().getDhsFilename();
        File f = new File(context.getConfig().getWorkDir(), filename);
        assertTrue(f.createNewFile());

        // Write the e-transfer query result that indicates the file is not
        // found in the e-transfer system.
        writeXferQueryResults(XferQueryBase.Status.Code.notFound, f);

        // Write the CRC value in the right place to make the test query URL
        // think that the file has already been accepted.
        CrcQueryTest.writeLine((TestDatamanConfig) context.getConfig(),
                                                                 filename, 0);

        // Run the test.
        GsaState[] expected = new GsaState[] {
                GsaState.PENDING,       // Starts life in PENDING state.
                GsaState.COPYING,       // VigilanteTask moves it to COPYING
                GsaState.VERIFYING,     // Running the verify script
                GsaState.QUEUED,        // Waiting GSA
        };
        singleDatasetTest(GsaState.PENDING, expected);

        // Make sure the file is really queued.
        f = new File(context.getConfig().getGsaXferConfig().getDestDir(), filename);
        assertTrue(f.exists());
    }

    /**
     * Tests the case where a queued file hasn't moved since the last time.
     */
    @Test public void testQueued_Queued() throws Exception {
        // Write the file in the working store.
        String filename = record1_1_1.getDataset().getDhsFilename();
        File f = new File(context.getConfig().getWorkDir(), filename);
        assertTrue(f.createNewFile());

        // Write the query result we're expecting in the right place so that
        // the test query URL will be fooled into thinking it got that result
        // from the GSA.
        writeXferQueryResults(XferQueryBase.Status.Code.pickup, f);

        // Run the test.
        GsaState[] expected = new GsaState[] {
                GsaState.QUEUED,       // Starts life in QUEUED state and doesn't move
        };
        singleDatasetTest(GsaState.QUEUED, expected);
    }

    /**
     * Tests the case where a queued file has been removed from the staging
     * directory, but the GSA doesn't know anything about it.  This would
     * essentially be a bug in their software... but we should handle it.
     */
    @Test public void testQueued_Pending() throws Exception {
        // Write the file in the working store.
        String filename = record1_1_1.getDataset().getDhsFilename();
        File f = new File(context.getConfig().getWorkDir(), filename);
        assertTrue(f.createNewFile());

        writeXferQueryResults(XferQueryBase.Status.Code.unknown, f);

        // Run the test.
        GsaState[] expected = new GsaState[] {
                GsaState.QUEUED,         // Starts life in QUEUED state
                GsaState.TRANSFER_ERROR, // Can't determine anything about it
                                         // afterwords, so try again
        };
        singleDatasetTest(GsaState.QUEUED, expected);
    }

    /**
     * Tests the case where a previously queued dataset is now being
     * transferred.
     */
    @Test public void testQueued_Transferring() throws Exception {
        // Write the file in the working store.
        String filename = record1_1_1.getDataset().getDhsFilename();
        File f = new File(context.getConfig().getWorkDir(), filename);
        assertTrue(f.createNewFile());

        // Write the query result we're expecting in the right place so that
        // the test query URL will be fooled into thinking it got that result
        // from the GSA.
        writeXferQueryResults(XferQueryBase.Status.Code.transferring, f);

        // Run the test.
        GsaState[] expected = new GsaState[] {
                GsaState.QUEUED,       // Starts life in QUEUED state
                GsaState.TRANSFERRING, // Ends up being transferred
        };
        singleDatasetTest(GsaState.QUEUED, expected);
    }

    /**
     * Tests the case where a previously queued dataset has been rejected.
     */
    @Test public void testQueued_Rejected() throws Exception {
        // Write the file in the working store.
        String filename = record1_1_1.getDataset().getDhsFilename();
        File f = new File(context.getConfig().getWorkDir(), filename);
        assertTrue(f.createNewFile());

        // Write the query result we're expecting in the right place so that
        // the test query URL will be fooled into thinking it got that result
        // from the GSA.
        writeXferQueryResults(XferQueryBase.Status.Code.rejected, f);

        // Run the test.
        GsaState[] expected = new GsaState[] {
                GsaState.QUEUED,    // Starts life in QUEUED state
                GsaState.REJECTED,  // Ends up being rejected
        };
        singleDatasetTest(GsaState.QUEUED, expected);
    }

    /**
     * Tests the case where a previously queued dataset has been accepted.
     */
    @Test public void testQueued_Accepted() throws Exception {
        // Write the file in the working store.
        String filename = record1_1_1.getDataset().getDhsFilename();
        File f = new File(context.getConfig().getWorkDir(), filename);
        assertTrue(f.createNewFile());

        // No longer in e-transfer system.
        writeXferQueryResults(XferQueryBase.Status.Code.notFound, f);

        // Write the CRC query result we're expecting in the right place.
        TestDatamanConfig config = (TestDatamanConfig) context.getConfig();
        long crc = DatamanFileUtil.crc(f);
        CrcQueryTest.writeLine(config, filename, crc);

        // Run the test.
        GsaState[] expected = new GsaState[] {
                GsaState.QUEUED,    // Starts life in QUEUED state
                GsaState.ACCEPTED,  // Ends up being accepted
        };
        singleDatasetTest(GsaState.QUEUED, expected);
    }

    /**
     * Tests the case where a previously queued dataset appears to be
     * accepted but the CRC doesn't match.
     */
    @Test public void testQueued_Pending_BadCrc() throws Exception {
        // Write the file in the working store.
        String filename = record1_1_1.getDataset().getDhsFilename();
        File f = new File(context.getConfig().getWorkDir(), filename);
        assertTrue(f.createNewFile());

        // No longer in e-transfer system.
        writeXferQueryResults(XferQueryBase.Status.Code.notFound, f);

        // Write a bad CRC query result -- a CRC that doesn't match.
        TestDatamanConfig config = (TestDatamanConfig) context.getConfig();
        CrcQueryTest.writeLine(config, filename, 1);

        // Run the test.
        GsaState[] expected = new GsaState[] {
                GsaState.QUEUED,          // Starts life in QUEUED state
                GsaState.PENDING,         // CRC doesn't match--reset to PENDING
        };
        singleDatasetTest(GsaState.QUEUED, expected);
    }

    /**
     * Tests the handling of a file that should not be transferred to the GSA.
     * Though it is PENDING transfer, it should get reset to NONE.
     */
    @Test public void testPending_Engineering() throws Exception {
        // Make a dummy program and observation.
        IDBDatabaseService testOdb = context.getTestOdb();

        SPProgramID progId1 = SPProgramID.toProgramID("GS-ENG19690815");
        SPObservationID obsId1_1 = new SPObservationID(progId1, 1);
        DatasetLabel lab = new DatasetLabel(obsId1_1, 1);

        record1_1_1 = new DatasetExecRecord(new Dataset(lab, "S19690815S0001.fits", System.currentTimeMillis()));

        prog1 = new TestProgramBuilder(testOdb, progId1);
        prog1.addObservation();

        // Create the file in the working store.
        String filename = record1_1_1.getDataset().getDhsFilename();
        File f = new File(context.getConfig().getWorkDir(), filename);
        assertTrue(f.createNewFile());

        // Not in e-transfer system.
        writeXferQueryResults(XferQueryBase.Status.Code.notFound, f);

        GsaState[] expected = new GsaState[] {
                GsaState.PENDING,      // Starts life in PENDING
                GsaState.NONE,  // Reset to NONE because it can't be sent to GSA
        };
        singleDatasetTest(GsaState.PENDING, expected);
    }

    /**
     * Tests resetting COPY_FAILED to PENDING, then copying it, ending up
     * QUEUED.
     */
    @Test public void testCopyFailed_Queued() throws Exception {
        // Write the file in the working store.
        String filename = record1_1_1.getDataset().getDhsFilename();
        File f = new File(context.getConfig().getWorkDir(), filename);
        assertTrue(f.createNewFile());

        // Not longer in e-transfer system.
        writeXferQueryResults(XferQueryBase.Status.Code.notFound, f);

        GsaState[] expected = new GsaState[] {
                GsaState.COPY_FAILED,   // Starts life in COPY_FAILED state.
                GsaState.PENDING,       // Vigilante moves it to PENDING
                GsaState.COPYING,       // VigilanteTask starts COPYING
                GsaState.VERIFYING,     // Running the verify script
                GsaState.QUEUED,        // Waiting GSA
        };
        singleDatasetTest(GsaState.COPY_FAILED, expected);

        // Make sure the file is really queued.
        f = new File(context.getConfig().getGsaXferConfig().getDestDir(), filename);
        assertTrue(f.exists());
    }

    /**
     * Tests transitioning out of a TRANSFER_ERROR when more information is
     * known.
     */
    @Test public void testTransferError_Transferring() throws Exception {
        // Write the file in the working store.
        String filename = record1_1_1.getDataset().getDhsFilename();
        File f = new File(context.getConfig().getWorkDir(), filename);
        assertTrue(f.createNewFile());

        // Write result that makes the vigilante task think we now have
        // information back from the GSA.
        writeXferQueryResults(XferQueryBase.Status.Code.transferring, f);


        // Run the test.
        GsaState[] expected = new GsaState[] {
                GsaState.TRANSFER_ERROR,  // Starts life in TRANSFER_ERROR
                GsaState.TRANSFERRING,    // Vigilante moves it to TRANSFERRING
        };
        singleDatasetTest(GsaState.TRANSFER_ERROR, expected);
    }
}
