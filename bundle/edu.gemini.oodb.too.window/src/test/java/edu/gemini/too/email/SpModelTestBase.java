//
// $
//

package edu.gemini.too.email;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.DBProgramListFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;

/**
 * A base class for SP Model test cases.
 */
public abstract class SpModelTestBase {
    private IDBDatabaseService odb;

    private ISPProgram prog;
    private ISPObservation obs;

    public void setUp(SPProgramID progId) throws Exception {
        odb = DBLocalDatabase.createTransient();
        prog = odb.getFactory().createProgram(new SPNodeKey(), progId);
        odb.put(prog);

        obs = odb.getFactory().createObservation(prog, Instrument.none, null);
        prog.addObservation(obs);
    }

    public void setUp() throws Exception {
        setUp(null);
    }

    public void tearDown() throws Exception {
        odb.getDBAdmin().shutdown();
    }

    /*
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
    */

    protected ISPProgram getProgram() {
        return prog;
    }

    protected ISPObservation getObs() {
        return obs;
    }

//    protected ISPObsComponent getObsComponent(SPComponentType type) throws Exception {
//        return getObsComponent(obs, type);
//    }
//
//    protected static ISPObsComponent getObsComponent(ISPObservation obs, SPComponentType type) throws Exception {
//        for (ISPObsComponent obsComp : obs.getObsComponents()) {
//            SPComponentType curType = obsComp.getType();
//            if (type.equals(curType)) return obsComp;
//        }
//        return null;
//    }
//
//    protected ISPObsComponent getTarget() {
//        return target;
//    }
//
//    protected TargetEnvironment getTargetEnvironment() throws Exception {
//        TargetObsComp dataObj = (TargetObsComp) target.getDataObject();
//        if (dataObj == null) return null;
//        return dataObj.getTargetEnvironment();
//    }
//
//    protected ISPSeqComponent addSeqComponent(ISPSeqComponent parent, SPComponentType type) throws Exception {
//        ISPSeqComponent comp = odb.getFactory().createSeqComponent(prog, type, null);
//        parent.addSeqComponent(comp);
//        return comp;
//    }
//
//    protected ISPSeqComponent getSeqComponent(SPComponentType type) throws Exception {
//        return getSeqComponent(obs, type);
//    }
//
//    protected static ISPSeqComponent getSeqComponent(ISPSeqComponent root, SPComponentType type) throws Exception {
//        if (type.equals(root.getType())) return root;
//
//        for (ISPSeqComponent child : root.getSeqComponents()) {
//            ISPSeqComponent res = getSeqComponent(child, type);
//            if (res != null) return res;
//        }
//
//        return null;
//    }
//
//    protected static ISPSeqComponent getSeqComponent(ISPObservation obs, SPComponentType type) throws Exception {
//        return getSeqComponent(obs.getSeqComponent(), type);
//    }
}
