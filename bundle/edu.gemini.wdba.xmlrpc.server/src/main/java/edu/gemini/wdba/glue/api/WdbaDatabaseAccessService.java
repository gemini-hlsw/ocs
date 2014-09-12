package edu.gemini.wdba.glue.api;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.wdba.shared.QueuedObservation;

import java.util.List;

/**
 * Gemini Observatory/AURA
 * $Id: WdbaDatabaseAccessService.java 844 2007-05-18 03:20:30Z gillies $
 */
public interface WdbaDatabaseAccessService {

    /**
     * Routine for returning a program by program id
     *
     * @param programId the program that should be returned
     * @return the {@link edu.gemini.pot.sp.ISPProgram} in the database for the programId
     * @throws edu.gemini.wdba.glue.api.WdbaGlueException
     *          if there is a bad argument or the program fetch fails
     */
    ISPProgram getProgram(String programId) throws WdbaGlueException;

    /**
     * Routine for returning a nightly plan by plan id. The plan is created if not found.
     *
     * @param planId the id for the nightly plan that should be returned
     */
    ISPNightlyRecord getNightlyRecord(SPProgramID planId) throws WdbaGlueException;

    /**
     * Internal routine for returning an observation
     *
     * @param observationId the observation that should be returned
     */
    ISPObservation getObservation(String observationId) throws WdbaGlueException;

    /**
     * Take a list of SPObservationIDs as Strings and return a list of <tt>QueuedObservations</tt>.
     * If an observation is not in the database, it is removed from the list.
     *
     * @param observationIDs a list of observation IDs
     */
    public List<QueuedObservation> getCheckedObservationList(List<String> observationIDs) throws WdbaGlueException;

    /**
     * Glue method for returning an observation with an <code>SPObservationID</code>
     *
     * @param spid an <code>SPObservationID</code>
     * @return
     * @throws WdbaGlueException
     */
    ISPObservation getObservation(SPObservationID spid) throws WdbaGlueException;

    /**
     * Return the IDBDatabase in use by this session
     *
     * @return A reference to the {@link edu.gemini.pot.spdb.IDBDatabaseService} of the connected database
     * @throws edu.gemini.wdba.glue.api.WdbaGlueException
     *          when type is DB_DESIGNATED
     */
    IDBDatabaseService getDatabase() throws WdbaGlueException;
}
