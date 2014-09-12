package edu.gemini.obslog.database;

import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.pot.sp.ISPNightlyRecord;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.plan.NightlyRecord;
import edu.gemini.spModel.obs.SPObservation;

import java.util.List;

//
// Gemini Observatory/AURA
// $Id: OlPersistenceManager.java,v 1.8 2005/12/11 15:54:15 gillies Exp $
//

public interface OlPersistenceManager {

    public ISPObservation getObservationByID(SPObservationID id);

    public SPObservation getObservationDataByID(SPObservationID id);

    public SPProgram getProgramByID(SPProgramID id);

    /**
     * Returns a list of observations belonging to the program as <code>{@link SPObservationID}</code> objects.
     *
     * @param id
     * @return <code>List</code> of observation IDs and <String> objects.
     */
    public List<SPObservationID> getProgramObservations(SPProgramID id);

    public ISPNightlyRecord getNightlyRecordNode(SPProgramID id);

    public NightlyRecord getNightlyRecord(SPProgramID id);

    public NightlyRecord createNightlyRecordBy(SPProgramID planID) throws OlLogException;

    public void setNightlyRecord(SPProgramID id, NightlyRecord obsLog);

    public IDBDatabaseService getDatabase();

    public String info();
}

