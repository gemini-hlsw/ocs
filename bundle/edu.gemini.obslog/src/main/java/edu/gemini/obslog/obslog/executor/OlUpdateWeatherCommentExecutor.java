package edu.gemini.obslog.obslog.executor;

import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.pot.sp.ISPNightlyRecord;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.plan.NightlyRecord;

public class OlUpdateWeatherCommentExecutor {

    private final OlPersistenceManager _persistenceManager;
    private final SPProgramID _planID;
    private final String _entryID;
    private final String _comment;

    public OlUpdateWeatherCommentExecutor(OlPersistenceManager persistenceManager, SPProgramID planID, String entryID, String comment) {
        _persistenceManager = persistenceManager;
        _planID = planID;
        _entryID = entryID;
        _comment = comment;
    }

    /**
     * Sets the appropriate comment.   This looks for an entry in entryID entry in the weather segment of the plan.
     * @throws edu.gemini.obslog.obslog.OlLogException if an error occurs while constructing the observing log
     */
    public void execute() throws OlLogException {

        if (_entryID == null) {
            throw new OlLogException("Comment received is null");
        }

        int entryID = Integer.parseInt(_entryID);

        ISPNightlyRecord nrec = _persistenceManager.getNightlyRecordNode(_planID);
        if (nrec == null) return;

        NightlyRecord obsLog = (NightlyRecord)nrec.getDataObject();
        // Need the observation data object too.  Can't get this far without a valid observation
        obsLog.setWeatherComment(entryID, _comment);
        nrec.setDataObject(obsLog);
    }

}
