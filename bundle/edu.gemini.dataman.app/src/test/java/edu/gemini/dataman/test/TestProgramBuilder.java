//
// $
//

package edu.gemini.dataman.test;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.dataset.*;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obsrecord.ObsExecRecord;

/**
 * Creates simple science programs to be used in dataman testing.
 */
public final class TestProgramBuilder {

    private IDBDatabaseService odb;
    private ISPProgram prog;

    public TestProgramBuilder(IDBDatabaseService odb, SPProgramID progId) throws Exception {
        this.odb = odb;

        // Create the program and add it to the database.
        prog = odb.getFactory().createProgram(new SPNodeKey(), progId);
        odb.put(prog);
    }

    public ISPProgram getProgram() {
        return prog;
    }

    /**
     * Adds an observation with an obs log component.
     */
    public ISPObservation addObservation() throws Exception {
        final ISPObservation obs = odb.getFactory().createObservation(prog, null);
        obs.setObsExecLog(odb.getFactory().createObsExecLog(prog, null));
        prog.addObservation(obs);
        return obs;
    }

    public DatasetExecRecord getDataset(DatasetLabel label) throws Exception {
        SPObservationID obsId = label.getObservationId();
        ISPObservation obs = odb.lookupObservationByID(obsId);

        ISPObsExecLog obsLogComp = obs.getObsExecLog();
        ObsExecLog dataObj = (ObsExecLog) obsLogComp.getDataObject();
        ObsExecRecord obsExecRecord = dataObj.getRecord();
        return obsExecRecord.getDatasetExecRecord(label);
    }

    public void putDataset(DatasetExecRecord record) throws Exception {
        SPObservationID obsId = record.label().getObservationId();
        ISPObservation obs = odb.lookupObservationByID(obsId);

        ISPObsExecLog obsLogComp = obs.getObsExecLog();
        ObsExecLog dataObj = (ObsExecLog) obsLogComp.getDataObject();
        ObsExecRecord obsExecRecord = dataObj.getRecord();
        obsExecRecord.putDatasetExecRecord(record, null);

        // Store the changes
        obsLogComp.setDataObject(dataObj);
    }
}
