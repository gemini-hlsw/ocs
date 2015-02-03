package edu.gemini.epics.acm.test;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.gemini.epics.acm.CaApplySender;
import edu.gemini.epics.acm.CaAttribute;
import edu.gemini.epics.acm.CaCommandMonitor;
import edu.gemini.epics.acm.CaCommandSender;
import edu.gemini.epics.acm.CaException;
import edu.gemini.epics.acm.CaParameter;
import edu.gemini.epics.acm.CaService;
import edu.gemini.epics.acm.CaStatusAcceptor;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

//TODO: Create a test IOC to run these tests against it.
//For now, just use the TCS simulator.

public class CaCommandSenderTest {

    // Must be the address of a known IOC.
    private static final String CA_ADDR_LIST = "127.0.0.1";
    private static final String TOP = "test";
    private static final String APPLY_NAME = "testApply";
    private static final String CS_NAME = "testCommand";
    private static final String APPLY = TOP + ":apply";
    private static final String CAR = TOP + ":applyC";
    private static final String NORMAL_CAD = TOP + ":test";
    private static final String PARAM1_NAME = "param1";
    private static final String PARAM1_CHANNEL = NORMAL_CAD + ".param1";
    private static final String PARAM2_NAME = "param2";
    private static final String PARAM2_CHANNEL = NORMAL_CAD + ".param2";
    private static final String ERROR_CAD = TOP + ":reboot";
    private static final String TIMEOUT_CAD = TOP + ":init";

    private static final String VALUE = "MagicWord";
    private static final long SLEEP_TIME = 5000;

    private CaService caService;
    private TestSimulator simulator;

    @Before
    public void setUp() throws Exception {
        simulator = new TestSimulator(TOP);
        simulator.start();
        CaService.setAddressList(CA_ADDR_LIST);
        caService = CaService.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        if (caService != null) {
            caService.unbind();
            caService = null;
        }

        simulator.stop();
    }

    @Test
    public void testCreateApplySender() throws CAException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        assertNotNull("Unable to create CaApplySender", apply);
    }

    @Test
    public void testGetApplySender() throws CAException {
        CaApplySender apply1 = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaApplySender apply2 = caService.getApplySender(APPLY_NAME);

        assertEquals("Retrieved the wrong CaStatusAcceptor.", apply1, apply2);
    }

    @Test
    public void testCreateCommandSender() throws CAException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs = caService.createCommandSender(CS_NAME, apply, null);
        assertNotNull("Unable to create CaCommandSender", cs);
    }

    @Test
    public void testGetCommandSender() throws CAException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs1 = caService.createCommandSender(CS_NAME, apply, null);
        CaCommandSender cs2 = caService.getCommandSender(CS_NAME);

        assertEquals("Retrieved the wrong CaStatusAcceptor.", cs1, cs2);
    }

    @Test
    public void testCreateParameter() throws CaException, CAException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs = caService.createCommandSender(CS_NAME, apply, null);
        CaParameter<String> param = cs.addString(PARAM1_NAME, PARAM1_CHANNEL);

        assertNotNull("Unable to create CaParameter.", param);
    }

    @Test(expected = CaException.class)
    public void testRejectParameterCreationWithDifferentType()
            throws CaException, CAException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs = caService.createCommandSender(CS_NAME, apply, null);

        cs.addString(PARAM1_NAME, PARAM1_CHANNEL);
        cs.addInteger(PARAM1_NAME, PARAM1_CHANNEL);
    }

    @Test(expected = CaException.class)
    public void testRejectParameterCreationWithDifferentChannel()
            throws CaException, CAException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs = caService.createCommandSender(CS_NAME, apply, null);

        cs.addString(PARAM1_NAME, PARAM1_CHANNEL);
        cs.addInteger(PARAM1_NAME, PARAM2_CHANNEL);
    }

    @Test
    public void testGetParameter() throws CaException, CAException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs = caService.createCommandSender(CS_NAME, apply, null);
        CaParameter<String> param1 = cs.addString(PARAM1_NAME, PARAM1_CHANNEL);
        CaParameter<String> param2 = cs.getString(PARAM1_NAME);

        assertEquals("Retrieved wrong command sender parameter.", param1,
                param2);
    }

    @Test
    public void testGetInfo() throws CaException, CAException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs = caService.createCommandSender(CS_NAME, apply, null);
        cs.addString(PARAM1_NAME, PARAM1_CHANNEL);
        cs.addString(PARAM2_NAME, PARAM2_CHANNEL);

        Set<String> paramSet = cs.getInfo();

        assertNotNull("Unable to retrieve attribute list.", paramSet);

        Set<String> testSet = new HashSet<String>();
        testSet.add(PARAM1_NAME);
        testSet.add(PARAM2_NAME);

        assertEquals("Retrieved bad attribute list.", paramSet, testSet);
    }

    @Test
    public void testSetParameter() throws CAException, TimeoutException,
            CaException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs = caService.createCommandSender(CS_NAME, apply, null);
        CaParameter<String> param1 = cs.addString(PARAM1_NAME, PARAM1_CHANNEL);

        CaStatusAcceptor sa = caService.createStatusAcceptor(CS_NAME);
        CaAttribute<String> attr = sa.addString(PARAM1_NAME, PARAM1_CHANNEL);

        param1.set(VALUE);
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String val = attr.value();

        assertEquals("Unable to write parameter value.", VALUE, val);
    }

    @Test
    public void testTriggerUnmarkedCommand() throws CAException,
            TimeoutException, CaException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs = caService.createCommandSender(CS_NAME, apply, null);

        CaCommandMonitor cm = cs.post();

        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue("Unmarked command did not completed.", cm.isDone());
    }

    @Test
    public void testTriggerCommandWithParameter() throws CAException,
            TimeoutException, CaException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs = caService.createCommandSender(CS_NAME, apply, null);
        CaParameter<String> param1 = cs.addString(PARAM1_NAME, PARAM1_CHANNEL);

        param1.set(VALUE);
        CaCommandMonitor cm = cs.post();

        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue("Unable to trigger command execution.", cm.isDone());
    }

    @Test
    public void testTriggerCommandWithMark() throws CAException,
            TimeoutException, CaException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs = caService.createCommandSender(CS_NAME, apply,
                NORMAL_CAD);

        cs.mark();
        CaCommandMonitor cm = cs.post();

        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue("Unable to trigger command execution.", cm.isDone());
    }

    @Test
    public void testCommandError() throws CAException, TimeoutException,
            CaException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs = caService.createCommandSender(CS_NAME, apply,
                ERROR_CAD);

        cs.mark();
        CaCommandMonitor cm = cs.post();

        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue("Command did not report execution error.", cm.isDone()
                && cm.state().equals(CaCommandMonitor.State.ERROR));
    }

    @Test
    public void testRetrieveCommandError() throws CAException,
            TimeoutException, CaException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs = caService.createCommandSender(CS_NAME, apply,
                ERROR_CAD);

        cs.mark();
        CaCommandMonitor cm = cs.post();

        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue("Command did not report execution error.", cm.isDone()
                && cm.state().equals(CaCommandMonitor.State.ERROR)
                && cm.error().getMessage().equals(TestSimulator.ERROR_MSG));
    }

    @Test
    public void testCommandTimeout() throws CAException, TimeoutException,
            CaException {
        CaApplySender apply = caService.createApplySender(APPLY_NAME, APPLY,
                CAR);
        CaCommandSender cs = caService.createCommandSender(CS_NAME, apply,
                TIMEOUT_CAD);

        apply.setTimeout(SLEEP_TIME / 2, TimeUnit.MILLISECONDS);

        cs.mark();
        CaCommandMonitor cm = cs.post();

        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue("Command did not report execution timeout.",
                cm.isDone() && cm.state().equals(CaCommandMonitor.State.ERROR)
                        && cm.error() instanceof TimeoutException);
    }

}
