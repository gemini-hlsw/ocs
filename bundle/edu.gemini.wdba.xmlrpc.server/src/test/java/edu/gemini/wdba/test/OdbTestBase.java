// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.test;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import junit.framework.TestCase;

public abstract class OdbTestBase extends TestCase {

    private IDBDatabaseService odb;

    private SPNodeKey progKey;
    private ISPProgram prog;
    private ISPObservation obs;

    private ISPObsComponent target;

    public void setUp(SPProgramID progId) throws Exception {
        super.setUp();
        odb = DBLocalDatabase.createTransient();
        prog = odb.getFactory().createProgram(new SPNodeKey(), progId);
        odb.put(prog);
        progKey = prog.getProgramKey();

        obs = odb.getFactory().createObservation(prog, Instrument.none, null);

        prog.addObservation(obs);

        target = getObsComponent(obs, TargetObsComp.SP_TYPE);
    }

    public void setUp() throws Exception {
        setUp(null);
    }

    public void tearDown() throws Exception {
        odb.getDBAdmin().shutdown();
    }

    protected IDBDatabaseService getOdb() { return odb; }
    protected ISPFactory getFactory() { return odb.getFactory(); }

    protected ISPObsComponent addObsComponent(SPComponentType type) throws Exception {
        ISPObsComponent comp = odb.getFactory().createObsComponent(prog, type, null);
        obs.addObsComponent(comp);
        return comp;
    }

    protected void removeObsComponent(SPComponentType type) throws Exception {
        ISPObsComponent comp = getObsComponent(type);
        if (comp == null) return;
        obs.removeObsComponent(comp);
    }

    protected SPNodeKey getProgKey() {
        return progKey;
    }

    protected ISPProgram getProgram() {
        return prog;
    }

    protected ISPObservation getObs() {
        return obs;
    }

    protected ISPObsComponent getObsComponent(SPComponentType type) throws Exception {
        return getObsComponent(obs, type);
    }

    protected static ISPObsComponent getObsComponent(ISPObservation obs, SPComponentType type) throws Exception {
        for (ISPObsComponent obsComp : obs.getObsComponents()) {
            SPComponentType curType = obsComp.getType();
            if (type.equals(curType)) return obsComp;
        }
        return null;
    }

    protected ISPObsComponent getTarget() {
        return target;
    }

    protected TargetEnvironment getTargetEnvironment() throws Exception {
        TargetObsComp dataObj = (TargetObsComp) target.getDataObject();
        if (dataObj == null) return null;
        return dataObj.getTargetEnvironment();
    }

    protected ISPSeqComponent addSeqComponent(ISPSeqComponent parent, SPComponentType type) throws Exception {
        ISPSeqComponent comp = odb.getFactory().createSeqComponent(prog, type, null);
        parent.addSeqComponent(comp);
        return comp;
    }

    protected ISPSeqComponent getSeqComponent(SPComponentType type) throws Exception {
        return getSeqComponent(obs, type);
    }

    protected static ISPSeqComponent getSeqComponent(ISPSeqComponent root, SPComponentType type) throws Exception {
        if (type.equals(root.getType())) return root;

        for (ISPSeqComponent child : root.getSeqComponents()) {
            ISPSeqComponent res = getSeqComponent(child, type);
            if (res != null) return res;
        }

        return null;
    }

    protected static ISPSeqComponent getSeqComponent(ISPObservation obs, SPComponentType type) throws Exception {
        return getSeqComponent(obs.getSeqComponent(), type);
    }

}
