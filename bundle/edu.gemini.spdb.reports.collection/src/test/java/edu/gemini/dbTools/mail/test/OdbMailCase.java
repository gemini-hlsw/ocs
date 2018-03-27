//
// $Id: OdbMailCase.java 46733 2012-07-12 20:43:36Z rnorris $
//
package edu.gemini.dbTools.mail.test;

import edu.gemini.dbTools.mail.OdbMailAgent;
import edu.gemini.dbTools.mail.OdbMailConfig;
import edu.gemini.dbTools.odbState.OdbStateAgent;
import edu.gemini.dbTools.odbState.OdbStateConfig;
import edu.gemini.pot.sp.Instrument;
import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.obs.ObsPhase2Status;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;
import edu.gemini.util.security.principal.StaffPrincipal;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.mail.Message;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class OdbMailCase {
    private static final Logger LOG = Logger.getLogger(OdbMailCase.class.getName());

    private Path _tempDirectory;

    private TestMailSender _testMailSender;
    private TestProgram _testProgram;
    private ISPProgram _prog;
    private ISPFactory _fact;
    private ISPObservation _obs0;
    private SPObservation _obsObj0;
    private ISPObservation _obs1;
    private SPObservation _obsObj1;
    private ISPObservation _obs2;
    private SPObservation _obsObj2;
    private ISPObservation _obs3;
    private SPObservation _obsObj3;

    private IDBDatabaseService odb;
    private OdbMailConfig mailConfig = null;
    private OdbStateConfig stateConfig = null;
    private OdbStateAgent stateAgent = null;
    private OdbMailAgent mailAgent = null;

    private final Set<Principal> user = Collections.<Principal>singleton(StaffPrincipal.Gemini());

    @Before
    public void setUp() throws Exception {
        _tempDirectory = Files.createTempDirectory("OdbMailTest");
        final Map<String, String> props = new HashMap<>();
        props.put("SITE_SMTP_SERVER", "notused");
        mailConfig  = new OdbMailConfig(_tempDirectory.toFile(), props);

        odb         = DBLocalDatabase.createTransient();
        stateConfig = new OdbStateConfig(_tempDirectory.toFile());
        mailAgent   = new OdbMailAgent(mailConfig, stateConfig);
        stateAgent  = new OdbStateAgent(LOG, stateConfig, odb);

        // Erase the state files, if they exist.
        File stateFile = mailConfig.stateFile;
        if (stateFile.exists()) assertTrue(stateFile.delete());
        stateFile = stateConfig.stateFile;
        if (stateFile.exists()) assertTrue(stateFile.delete());

        // Set up the email agent with a test sender.
        _testMailSender = new TestMailSender();
        OdbMailAgent.setMailSender(_testMailSender);

        _testProgram = new TestProgram(mailConfig);
        _prog = _testProgram.create(odb);
        _fact = odb.getFactory();
        odb.put(_prog);

        // Run it once to create the initial state.
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        // Make sure that the mail sender is empty.
        assertEquals(0, _testMailSender.getMessageCount());

        final List obsList = _prog.getAllObservations();

        // Make sure we're starting as expected.
        _obs0 = (ISPObservation) obsList.get(0);
        _obsObj0 = (SPObservation) _obs0.getDataObject();
        assertEquals(ObservationStatus.PHASE2,
                ObservationStatus.computeFor(_obs0));

        _obs1 = (ISPObservation) obsList.get(1);
        _obsObj1 = (SPObservation) _obs1.getDataObject();
        assertEquals(ObservationStatus.FOR_REVIEW,
                ObservationStatus.computeFor(_obs1));

        _obs2 = (ISPObservation) obsList.get(2);
        _obsObj2 = (SPObservation) _obs2.getDataObject();
        assertEquals(ObservationStatus.FOR_ACTIVATION,
                ObservationStatus.computeFor(_obs2));

        _obs3 = (ISPObservation) obsList.get(3);
        _obsObj3 = (SPObservation) _obs3.getDataObject();
        assertEquals(ObservationStatus.READY,
                ObservationStatus.computeFor(_obs3));
    }

    @After
    public void tearDown() {
        odb.getDBAdmin().shutdown();
    }

    //
    // Moves the first observation from PhaseII to Ready one step at a time.
    //
    @Test public void testOneAdvancing() throws Exception {
        // The same list of observation ids will apply each time.
        final SPObservationID obsId = _obs0.getObservationID();
        final List<SPObservationID> idList = new ArrayList<>();
        idList.add(obsId);

        final ObservationStatus[] statusA = {
            ObservationStatus.FOR_REVIEW,
            ObservationStatus.FOR_ACTIVATION,
            ObservationStatus.READY,
        };

        final Message[] messageA = {
            _testProgram.createUp_ForReview(idList),
            _testProgram.createUp_ForActivation(idList),
            _testProgram.createUp_Ready(idList),
        };

        for (int i = 0; i < statusA.length; ++i) {
            final ObservationStatus status = statusA[i];
            final Message msg = messageA[i];

            // Move to the next status up.  Leave all else the same.
            _obsObj0.setPhase2Status(status.phase2());
            _obs0.setDataObject(_obsObj0);

            // Run the email agent.
            stateAgent.updateState(LOG, user);
            mailAgent.executeOnce(LOG);

            // Check the message that was generated.
            final String key = "oneAdvancing: " + status.name();
            assertTrue(key, _testMailSender.matchMessage(msg));
            assertEquals(key, 1, _testMailSender.getMessageCount());
            _testMailSender.clearMessages();
        }
    }

    //
    // Moves the first observation from PhaseII to Ready one step at a time.
    //
    @Test public void testOneRetreating() throws Exception {
        // The same list of observation ids will apply each time.
        final SPObservationID obsId = _obs2.getObservationID();
        final List<SPObservationID> idList = new ArrayList<>();
        idList.add(obsId);

        final ObservationStatus[] statusA = {
            ObservationStatus.FOR_REVIEW,
            ObservationStatus.PHASE2,
        };

        final Message[] messageA = {
            _testProgram.createDown_ForReview(idList),
            _testProgram.createDown_Phase2(idList),
        };

        for (int i = 0; i < statusA.length; ++i) {
            final ObservationStatus status = statusA[i];
            final Message msg = messageA[i];

            // Move to the next status up.  Leave all else the same.
            _obsObj2.setPhase2Status(status.phase2());
            _obs2.setDataObject(_obsObj2);

            // Run the email agent.
            stateAgent.updateState(LOG, user);
            mailAgent.executeOnce(LOG);

            // Check the message that was generated.
            final String key = "oneRetreating: " + status.name();
            assertTrue(key, _testMailSender.matchMessage(msg));
            assertEquals(key, 1, _testMailSender.getMessageCount());
            _testMailSender.clearMessages();
        }
    }

    //
    // Tests moving to ON_HOLD for a non-Too program which should do nothing.
    //
    @Test public void testNotTooOnHold() throws Exception {
        // Move the first observation to ON_HOLD.
        _obsObj0.setPhase2Status(ObsPhase2Status.ON_HOLD);
        _obs0.setDataObject(_obsObj0);

        // Run the email agent.
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        // Check that no message was generated.
        assertEquals("onHold, non Too", 0, _testMailSender.getMessageCount());
    }

    //
    // Tests moving to ON_HOLD for a ToO program.
    //
    @Test public void testTooOnHold() throws Exception {
        // Modify the program to be a ToO program.
        Too.set(_prog, TooType.standard);

        // Move the first observation up to ON_HOLD.
        _obsObj0.setPhase2Status(ObsPhase2Status.ON_HOLD);
        _obs0.setDataObject(_obsObj0);

        // Move the last observation down to ON_HOLD.
        _obsObj3.setPhase2Status(ObsPhase2Status.ON_HOLD);
        _obs3.setDataObject(_obsObj3);

        // Run the email agent.
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        // Check that a message was generated.
        assertEquals("onHold, Too", 1, _testMailSender.getMessageCount());

        final List<SPObservationID> idList = Arrays.asList(
            _obs0.getObservationID(),
            _obs3.getObservationID()
        );
        final Message msg = _testProgram.createAny_On_Hold(idList);
        assertTrue("onHold, Too", _testMailSender.matchMessage(msg));
    }


    //
    // Tests having more than one observation in an email.
    //
    @Test public void testMultipleObsOneMail() throws Exception {
        Message msg;
        final List<SPObservationID> idList = new ArrayList<>();

        // Move obs0 to FOR_REVIEW
        idList.add(_obs0.getObservationID());
        _obsObj0.setPhase2Status(ObservationStatus.FOR_REVIEW.phase2());
        _obs0.setDataObject(_obsObj0);
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        msg = _testProgram.createUp_ForReview(idList);
        assertTrue(_testMailSender.matchMessage(msg));
        assertEquals(1, _testMailSender.getMessageCount());
        _testMailSender.clearMessages();

        // Move obs0 and obs1 to FOR_ACTIVATION
        idList.add(_obs1.getObservationID());
        _obsObj0.setPhase2Status(ObservationStatus.FOR_ACTIVATION.phase2());
        _obs0.setDataObject(_obsObj0);
        _obsObj1.setPhase2Status(ObservationStatus.FOR_ACTIVATION.phase2());
        _obs1.setDataObject(_obsObj1);
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        msg = _testProgram.createUp_ForActivation(idList);
        assertTrue(_testMailSender.matchMessage(msg));
        assertEquals(1, _testMailSender.getMessageCount());
        _testMailSender.clearMessages();

        // Move obs0, obs1, and obs2 to READY
        idList.add(_obs2.getObservationID());
        _obsObj0.setPhase2Status(ObservationStatus.READY.phase2());
        _obs0.setDataObject(_obsObj0);
        _obsObj1.setPhase2Status(ObservationStatus.READY.phase2());
        _obs1.setDataObject(_obsObj1);
        _obsObj2.setPhase2Status(ObservationStatus.READY.phase2());
        _obs2.setDataObject(_obsObj2);
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        msg = _testProgram.createUp_Ready(idList);
        assertTrue(_testMailSender.matchMessage(msg));
        assertEquals(1, _testMailSender.getMessageCount());
        _testMailSender.clearMessages();
    }

    //
    // Tests changing observation status for observations in a group.  There
    // is nothing new here really, but a bug was reported that only the first
    // observation in a group results in email being sent.  This test case
    // proves that theory wrong.
    //
    @Test public void testObsInGroup() throws Exception {
        Message msg;
        final List<SPObservationID> idList = new ArrayList<>();

        _obsObj0.setGroup("xyz");
        _obsObj1.setGroup("xyz");
        _obsObj2.setGroup("xyz");

        // Move obs0 and obs1 to FOR_ACTIVATION
        idList.add(_obs0.getObservationID());
        idList.add(_obs1.getObservationID());
        _obsObj0.setPhase2Status(ObservationStatus.FOR_ACTIVATION.phase2());
        _obs0.setDataObject(_obsObj0);
        _obsObj1.setPhase2Status(ObservationStatus.FOR_ACTIVATION.phase2());
        _obs1.setDataObject(_obsObj1);
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        msg = _testProgram.createUp_ForActivation(idList);
        assertTrue(_testMailSender.matchMessage(msg));
        assertEquals(1, _testMailSender.getMessageCount());
        _testMailSender.clearMessages();

        // Move obs0, obs1, and obs2 to READY
        idList.add(_obs2.getObservationID());
        _obsObj0.setPhase2Status(ObservationStatus.READY.phase2());
        _obs0.setDataObject(_obsObj0);
        _obsObj1.setPhase2Status(ObservationStatus.READY.phase2());
        _obs1.setDataObject(_obsObj1);
        _obsObj2.setPhase2Status(ObservationStatus.READY.phase2());
        _obs2.setDataObject(_obsObj2);
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        msg = _testProgram.createUp_Ready(idList);
        assertTrue(_testMailSender.matchMessage(msg));
        assertEquals(1, _testMailSender.getMessageCount());
        _testMailSender.clearMessages();
    }

    //
    // Tests more than one email generated by a program.
    //
    @Test public void testMultipleMailsOneProgram() throws Exception {
        final List<SPObservationID> idList0 = new ArrayList<>();
        final List<SPObservationID> idList1 = new ArrayList<>();

        // Move obs0 to FOR_REVIEW.
        idList0.add(_obs0.getObservationID());
        _obsObj0.setPhase2Status(ObservationStatus.FOR_REVIEW.phase2());
        _obs0.setDataObject(_obsObj0);

        // Move obs1 to FOR_ACTIVATION.
        idList1.add(_obs1.getObservationID());
        _obsObj1.setPhase2Status(ObservationStatus.FOR_ACTIVATION.phase2());
        _obs1.setDataObject(_obsObj1);

        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        final Message msg0 = _testProgram.createUp_ForReview(idList0);
        final Message msg1 = _testProgram.createUp_ForActivation(idList1);
        assertTrue(_testMailSender.matchMessage(msg0));
        assertTrue(_testMailSender.matchMessage(msg1));
        assertEquals(2, _testMailSender.getMessageCount());
    }

    //
    // Tests "skipping" an observation status.
    //
    @Test public void testSkippingStatus() throws Exception {
        final List<SPObservationID> idList = new ArrayList<>();
        idList.add(_obs0.getObservationID());

        // Move obs0 from PhaseII to FOR_ACTIVATION (skipping FOR_REVIEW)
        _obsObj0.setPhase2Status(ObservationStatus.FOR_ACTIVATION.phase2());
        _obs0.setDataObject(_obsObj0);
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        Message msg = _testProgram.createUp_ForActivation(idList);
        assertTrue(_testMailSender.matchMessage(msg));
        assertEquals(1, _testMailSender.getMessageCount());
        _testMailSender.clearMessages();

        // Now move obs0 back to Phase II (skipping FOR_REVIEW)
        _obsObj0.setPhase2Status(ObservationStatus.PHASE2.phase2());
        _obs0.setDataObject(_obsObj0);
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        msg = _testProgram.createDown_Phase2(idList);
        assertTrue(_testMailSender.matchMessage(msg));
        assertEquals(1, _testMailSender.getMessageCount());
        _testMailSender.clearMessages();
    }

    //
    // Tests a new observation.
    //
    @Test public void testNewObservation() throws Exception {
        // Create a new Observation in the ready state.
        final List<ISPObservation> obsList = _prog.getAllObservations();
        final ISPObservation newObs = _fact.createObservation(_prog, Instrument.none, null);
        obsList.add(newObs);
        final SPObservation newObsDataObject = (SPObservation) newObs.getDataObject();
        newObsDataObject.setPhase2Status(ObservationStatus.READY.phase2());
        newObs.setDataObject(newObsDataObject);
        _prog.setObservations(obsList);

        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        final List<SPObservationID> idList = new ArrayList<>();
        idList.add(newObs.getObservationID());
        final Message msg = _testProgram.createUp_Ready(idList);
        assertTrue(_testMailSender.matchMessage(msg));
        assertEquals(1, _testMailSender.getMessageCount());
        _testMailSender.clearMessages();
    }

    //
    // Tests moving to some arbitrary non-email associated state.
    //
    @Test public void testOtherStatus() throws Exception {
        // Not yet fully implemented.  Want to make sure that nothing bad happens.
        _obsObj0.setPhase2Status(ObsPhase2Status.INACTIVE);
        _obs0.setDataObject(_obsObj0);
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        assertEquals(0, _testMailSender.getMessageCount());
    }

    //
    // Test events involving a program with bad email addresses.
    //
    @Test public void testBadAddresses() throws Exception {
        final BadAddressProgram bap = new BadAddressProgram(mailConfig);

        final ISPProgram prog = bap.create(odb);
        final List obsList = prog.getAllObservations();
        final ISPObservation obs0 = (ISPObservation) obsList.get(0);
        final SPObservation obsObj0 = (SPObservation) obs0.getDataObject();

        odb.put(prog);

        // Run it once to create the initial state.
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        // Make sure that the mail sender is empty.
        assertEquals(0, _testMailSender.getMessageCount());

        final List<SPObservationID> idList = new ArrayList<>();
        idList.add(obs0.getObservationID());

        // Move the status of obs0 to FOR_REVIEW.
        obsObj0.setPhase2Status(ObservationStatus.FOR_REVIEW.phase2());
        obs0.setDataObject(obsObj0);
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        // One email should be sent even though the NGO address is bad,
        // since the gemini contact address is used in this case.
        assertEquals(1, _testMailSender.getMessageCount());
        Message msg = bap.createUp_ForReview(idList);
        assertTrue(_testMailSender.matchMessage(msg));
        _testMailSender.clearMessages();

        // Move the status of obs0 back to PHASE2.
        obsObj0.setPhase2Status(ObservationStatus.PHASE2.phase2());
        obs0.setDataObject(obsObj0);
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        // Only one email should be sent because only just one is valid, one
        // of the gemini contact emails.
        assertEquals(1, _testMailSender.getMessageCount());

        // Move the status of obs0 back to FOR_ACTIVATION
        obsObj0.setPhase2Status(ObservationStatus.FOR_ACTIVATION.phase2());
        obs0.setDataObject(obsObj0);
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        // One (more) email should have been sent because there is one valid
        // gemini address.
        assertEquals(2, _testMailSender.getMessageCount());

        msg = bap.createUp_ForActivation(idList);
        assertTrue(_testMailSender.matchMessage(msg));
        _testMailSender.clearMessages();


        // Finally move back to FOR_REVIEW.  In this case, since the NGO address
        // is bad and there is no PI, no email should be sent for those but
        // we still have the one valid gemini address.
        obsObj0.setPhase2Status(ObservationStatus.FOR_REVIEW.phase2());
        obs0.setDataObject(obsObj0);
        stateAgent.updateState(LOG, user);
        mailAgent.executeOnce(LOG);

        // One email should be send because there is one valid gemini address.
        assertEquals(1, _testMailSender.getMessageCount());
    }
}
