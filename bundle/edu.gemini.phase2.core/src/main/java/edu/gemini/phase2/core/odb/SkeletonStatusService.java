package edu.gemini.phase2.core.odb;

import edu.gemini.phase2.core.model.SkeletonStatus;
import static edu.gemini.phase2.core.model.SkeletonStatus.*;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.obs.ObsPhase2Status;
import edu.gemini.spModel.obs.SPObservation;


import java.util.List;

public final class SkeletonStatusService {
    private SkeletonStatusService() {}

    // For now, just see whether it has observations past PHASE2.
    public static SkeletonStatus getStatus(ISPProgram p)  {
        return forallPhase2(p.getAllObservations()) ? INITIALIZED : MODIFIED;
    }

    private static boolean forallPhase2(List<ISPObservation> obsList)  {
        if (obsList == null) return true;
        for (ISPObservation obs : obsList) {
            SPObservation dataobj = (SPObservation) obs.getDataObject();
            ObsPhase2Status status = dataobj.getPhase2Status();
            if ((status != null) && (status != ObsPhase2Status.PI_TO_COMPLETE)) return false;
        }
        return true;
    }

    public static SkeletonStatus getStatus(IDBDatabaseService odb, SPProgramID id)  {
        ISPProgram p = odb.lookupProgramByID(id);
        return (p == null) ? NOT_PRESENT : getStatus(p);
    }
}
