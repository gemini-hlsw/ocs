package edu.gemini.spModel.config.test;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.test.EmptyNodeInitializer;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.config.ObservationCB;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.*;
import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

public abstract class CBTestBase {

    protected IDBDatabaseService testOdb;
    protected ISPFactory testFactory;
    protected ISPProgram testProg;
    protected SPNodeKey testProgKey;

    // Convenience routines to create parameters in line
    protected void _putParameterValue(ISysConfig sc, String pname, Object pvalue) {
        IParameter ip = DefaultParameter.getInstance(pname, pvalue);
        sc.putParameter(ip);
    }

    // Convenience routines to create parameters in line
    protected void _putConfigParameterValue(IConfig config, String sysName, String pname, String pvalue) {
        ISysConfig sc = config.getSysConfig(sysName);
        if (sc == null) {
            sc = new DefaultSysConfig(sysName);
            config.appendSysConfig(sc);
        }
        _putParameterValue(sc, pname, pvalue);
    }

    public void setUp() throws Exception {
        testOdb     = DBLocalDatabase.createTransient();
        testFactory = testOdb.getFactory();
        testProg    = testFactory.createProgram(new SPNodeKey(), null);
        testOdb.put(testProg);
        testProgKey = testProg.getProgramKey();
    }

    public void tearDown() {
        testOdb.getDBAdmin().shutdown();
    }

    protected ISPObservation createObservation() throws Exception {
        // Create the observation.
        ISPObservation obs = testFactory.createObservation(testProg, Instrument.none, null);

        // Set the observation parameter builder.
        ObservationCB ocb = new ObservationCB(obs);
        obs.putClientData(IConfigBuilder.USER_OBJ_KEY, ocb);

        return obs;
    }

    private final ISPNodeInitializer<ISPObsComponent, ISPDataObject> UNKNOWN_OC =
        new EmptyNodeInitializer<>();

    protected ISPObsComponent createObsComponent(SPComponentType compType) throws Exception {
        // Create the observation component.
        if (compType == SPComponentType.UNKNOWN) {
            return testFactory.createObsComponent(testProg, compType, UNKNOWN_OC, null);
        } else {
            return testFactory.createObsComponent(testProg, compType, null);
        }
    }

    private final ISPNodeInitializer<ISPSeqComponent, ISPSeqObject> UNKNOWN_SC =
        new EmptyNodeInitializer<>();

    protected ISPSeqComponent createSeqComponent(SPComponentType compType) throws Exception {
        // Create the sequence component.
        if (compType == SPComponentType.UNKNOWN) {
            return testFactory.createSeqComponent(testProg, compType, UNKNOWN_SC, null);
        } else {
            return testFactory.createSeqComponent(testProg, compType, null);
        }
    }

    protected void runApply(IConfigBuilder cb, IConfig expected)  {
        List<IConfig> eList = new LinkedList<>();
        eList.add(expected);

        runApply(cb, eList);
    }

    /**
     * Run the parameter builder by successively calling applyNext() while
     * there are remaining configurations to be produced.  Each configuration
     * produced is matched against the next configuration in the
     * <code>expectedList</code>.
     *
     * @param cb the parameter builder to execute
     * @param expectedList list of expected IConfig instances to match
     */
    protected void runApply(IConfigBuilder cb, List<IConfig> expectedList)  {
        // Test reset.  Make sure that "hasNext()" doesn't modify the state with
        // each call.
        cb.reset(null);
        assertTrue("should have a configuration now", cb.hasNext());
        assertTrue("should still have configuration now", cb.hasNext());

        // Try to match the expected configurations by successively applying
        // the next configuration on the builder.
        String hasNextMsg = "hasNext";
        String matchMsg = "matching result";

        IConfig full = new DefaultConfig();

        int count = 0;
        int sz = expectedList.size();

        for (Object anExpectedList : expectedList) {
            if (sz > 1) {
                hasNextMsg = "hasNext: " + count;
                matchMsg = "matching result: " + count;
            }

            assertTrue(hasNextMsg, cb.hasNext());

            // Create a result to hold the current config, and extend the full results by this config.
            IConfig result = new DefaultConfig();
            cb.applyNext(result, full);
            full.mergeSysConfigs(result.getSysConfigs());

            // The expected results are this config.
            IConfig expectedConf = (IConfig) anExpectedList;

            /* Comments on the result.removeSysConfig calls:
             * SW: Removing because it creates too much output and make it hard to see the results of running the unit tests.
             *     (Note: SW had commented out the metadata removeSysConfig call.)
             * RCN: I think we want to discard this because it breaks equality checking. Either that or
             *      add it as an expected value.
             * SR: Seems that all the results contain three configs above and beyond the results, namely
             *     ocs, telescope, and metadata (which are all created immediately when the observation is created).
             *     Thus, to achieve equality, we need to remove these from the result as they do not appear in the
             *     expected value.
             */
            result.removeSysConfig("metadata");
            // SR: Added these two as per the comment above.
            result.removeSysConfig("ocs");
            result.removeSysConfig("telescope");

//            System.out.println("\nRESULT: ");
//            ((DefaultConfig) result).dumpState();
//
//            System.out.println("\nEXPECTED: ");
//            ((DefaultConfig) expectedConf).dumpState();


            // Compare the expected and result.
            assertEquals(matchMsg, expectedConf, result);

            ++count;
        }

        // Should be finished now.
        assertTrue("finished", !cb.hasNext());
    }

}

