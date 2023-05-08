//
// $Id: WdbaGlueService.java 848 2007-05-19 03:59:45Z gillies $
//
package edu.gemini.wdba.glue;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.wdba.glue.api.WdbaDatabaseAccessService;
import edu.gemini.wdba.glue.api.WdbaGlueException;
import edu.gemini.wdba.shared.QueuedObservation;


import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A singleton to provide database utilities to other services.
 *
 * @author Kim Gillies
 */
public final class WdbaGlueService implements WdbaDatabaseAccessService {
    private static final Logger LOG = Logger.getLogger(WdbaGlueService.class.getName());

    // The IDBDatabaseService in use by this session
    private final IDBDatabaseService _database;
    private final Set<Principal> _user;

    public WdbaGlueService(IDBDatabaseService database, Set<Principal> user) {
        this._database = database;
        this._user = user;
    }

    /**
     * Return the IDBDatabase in use by this session
     *
     * @return A reference to the {@link IDBDatabaseService} of the connected database
     * @throws edu.gemini.wdba.glue.api.WdbaGlueException when type is DB_DESIGNATED
     */
    public synchronized IDBDatabaseService getDatabase() throws WdbaGlueException {
        if (_database == null) {
            throw new WdbaGlueException("Database not available.");
        }
        return _database;
    }

    /**
     * Return the ISPFactory in use by this session.
     *
     * @return {@link edu.gemini.pot.sp.ISPFactory} reference of the connected database
     * @throws WdbaGlueException for a database remote exception
     */
    public synchronized ISPFactory getFactory() throws WdbaGlueException {
        return getDatabase().getFactory();
    }

    /**
     * Routine for returning a program by program id
     *
     * @param programId the program that should be returned
     * @return the {@link edu.gemini.pot.sp.ISPProgram} in the database for the programId
     * @throws WdbaGlueException if there is a bad argument or the program fetch fails
     */
    public ISPProgram getProgram(String programId) throws WdbaGlueException {
        if (programId == null) {
            String message = "Program id was null";
            LOG.severe(message);
            throw new WdbaGlueException(message);
        }

        ISPProgram spProg;
        try {
            SPProgramID spid = SPProgramID.toProgramID(programId);

            spProg = getDatabase().lookupProgramByID(spid);
            if (spProg == null) {
                throw new WdbaGlueException("Program \"" + programId + "\" not found.");
            }
        } catch (SPBadIDException ex) {
            throw new WdbaGlueException("Poorly formed program id: " + programId);
        }
        return spProg;
    }

    /**
     * Take a list of SPObservationIDs as Strings and return a list of <tt>QueuedObservations</tt>.
     * If an observation is not in the database, it is removed from the list.
     */
    public List<QueuedObservation> getCheckedObservationList(List<String> obsIDs) throws WdbaGlueException {
        List<QueuedObservation> result;
        // First check for no obsIDs - save a little time
        if (obsIDs.size() == 0) return new ArrayList<>();
        try {
            result = QueuedObservationFunctor.getQueuedObservations(getDatabase(), obsIDs, _user);
        } catch (SPBadIDException ex) {
             throw new WdbaGlueException("Poorly formed program id in list: " + obsIDs);
        }
        return result;
    }

    /**
     * Routine for returning a nightly plan by plan id. The plan is created if not found.
     *
     * @param recordId the id for the nightly plan that should be returned
     */
    public ISPNightlyRecord getNightlyRecord(SPProgramID recordId)
            throws WdbaGlueException {
        if (recordId == null) {
            String message = "Plan id was null";
            LOG.severe(message);
            throw new WdbaGlueException(message);
        }

        try {
            ISPNightlyRecord record = getDatabase().lookupNightlyRecordByID(recordId);
            if (record == null) {
                record = getFactory().createNightlyRecord(new SPNodeKey(), recordId);
                ISPDataObject dObj = record.getDataObject();
                dObj.setTitle(recordId.stringValue());
                record.setDataObject(dObj);
                getDatabase().put(record);
            }
            return record;
        } catch (Exception ex) {
            String message = "Database Exception: failed to get nightly plan \"" + recordId + "\" from ODB.";
            LOG.severe(message);
            throw new WdbaGlueException(message, ex);
        }
    }


    /**
     * Internal routine for returning an observation
     *
     * @param observationId the observation that should be returned
     */
    public ISPObservation getObservation(String observationId) throws WdbaGlueException {
        if (observationId == null) throw new WdbaGlueException("Observation id was null.");

        SPObservationID spid;
        try {
            spid = new SPObservationID(observationId);
        } catch (SPBadIDException ex) {
            String message = "Poorly formed observation id: " + observationId;
            LOG.severe(message);
            throw new WdbaGlueException(message);
        }
        return getObservation(spid);
    }

    /**
     * Glue method for returning an observation with an <code>SPObservationID</code>
     *
     * @param spid an <code>SPObservationID</code>
     * @return The <tt>ISPObservation</tt> in the database with the given observation ID
     * @throws WdbaGlueException
     */
    public ISPObservation getObservation(SPObservationID spid) throws WdbaGlueException {
        ISPObservation spObs = getDatabase().lookupObservationByID(spid);
        if (spObs == null) throw new WdbaGlueException("Observation \"" + spid.stringValue() + "\" not found.");
        return spObs;
    }
}

