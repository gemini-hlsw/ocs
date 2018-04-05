/*
 * Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ObsCatalogTest.java 47005 2012-07-26 22:35:47Z swalker $
 */
package jsky.app.ot.gemini.obscat.test;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import jsky.app.ot.gemini.obscat.ObsCatalog;
import jsky.app.ot.gemini.obscat.ObsCatalogQueryArgs;
import jsky.app.ot.shared.gemini.obscat.ObsCatalogInfo;
import jsky.catalog.QueryResult;
import jsky.catalog.skycat.SkycatConfigFile;
import jsky.catalog.skycat.SkycatTable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;


/**
 * This test class is called after the SessionManager has initialized the spdb.
 * It tests the ObsCatalog class with whatever database is active.
 */
public class ObsCatalogTest {
    // number of programs to test with
    private static final int NUM_PROGS = 2;

    // number of observations per program
    private static final int NUM_OBS = 2;

    IDBDatabaseService _db;
    SPProgramID[] _progIds = new SPProgramID[NUM_PROGS];
    ISPProgram[] _progs = new ISPProgram[NUM_PROGS];
    ISPObservation[] _obs = new ISPObservation[NUM_PROGS * NUM_OBS];
    ObsCatalog _cat;

    // Initialize and create test programs to work with
    @Before
    public void setUp() throws Exception {

        _db = DBLocalDatabase.createTransient();
        ISPFactory f = _db.getFactory();
        SkycatConfigFile.setConfigFile(ObsCatalogTest.class.getResource("skycat.cfg"));
        _cat = ObsCatalog.INSTANCE;

        for(int i = 0; i < _progs.length; i++) {
            _progIds[i] = SPProgramID.toProgramID("TEST" + i);

            // remove any left overs from a previous run
            _progs[i] = _db.lookupProgramByID(_progIds[i]);
            if (_progs[i] != null) {
                _db.remove(_progs[i]);
            }

            SPNodeKey progKey = new SPNodeKey();
            _progs[i]= f.createProgram(progKey, _progIds[i]);
            _db.put(_progs[i]);

            for(int j = 0; j < NUM_OBS; j++) {
                int obsIndex = i*NUM_OBS+j;
                _obs[obsIndex] = f.createObservation(_progs[i], Instrument.none, new SPNodeKey());
                _progs[i].addObservation(_obs[obsIndex]);

            }

            // add a GMOS instrument to the first observation in each program
            ISPObsComponent obsComp = f.createObsComponent(_progs[i], InstGmosNorth.SP_TYPE, new SPNodeKey());
            _obs[i*2].addObsComponent(obsComp);
        }
    }

    // remove the test program
    @After
    public void tearDown() throws Exception {
        for (ISPProgram _prog : _progs) {
            _db.remove(_prog);
        }
        _db.getDBAdmin().shutdown();
    }

    @Test
    @Ignore("Requires that the OT have its KeyChain established.")
    public void testQuery() throws Exception {
        for (SPProgramID _progId : _progIds) {
            _doProgIdQuery(_progId);
            _doInstQuery();
        }
    }

    private void _doProgIdQuery(SPProgramID progId) throws Exception {
        ObsCatalogQueryArgs queryArgs = new ObsCatalogQueryArgs(_cat);
        queryArgs.setParamValue(ObsCatalogInfo.PROG_REF, progId.stringValue());
        QueryResult result = _cat.query(queryArgs);
        SkycatTable table = ((SkycatTable) result);
        assertEquals(table.getRowCount(), NUM_OBS);

        for(int i = 0; i < NUM_OBS; i++) {
            Object value = table.getValueAt(i, ObsCatalogInfo.PROG_REF);
            assertNotNull(value);
            // note: user types in progId as a String, but the table holds an SPProgramID,
            // for sorting
            assertEquals(value, progId);
        }
    }

    private void _doInstQuery() throws Exception {
        ObsCatalogQueryArgs queryArgs = new ObsCatalogQueryArgs(_cat);
        queryArgs.setParamValue(ObsCatalogInfo.PROG_REF, "TEST*");
        queryArgs.setParamValue(ObsCatalogInfo.INSTRUMENT, InstGmosNorth.SP_TYPE.readableStr);
        QueryResult result = _cat.query(queryArgs);
        SkycatTable table = ((SkycatTable) result);
        assertEquals(NUM_PROGS, table.getRowCount());

        for(int i = 0; i < NUM_PROGS; i++) {
            Object value = table.getValueAt(i, ObsCatalogInfo.INSTRUMENT);
            assertNotNull(value);
            // note: user types in progId as a String, but the table holds an SPProgramID,
            // for sorting
            assertEquals(value, InstGmosNorth.SP_TYPE.readableStr);
        }
    }
}
