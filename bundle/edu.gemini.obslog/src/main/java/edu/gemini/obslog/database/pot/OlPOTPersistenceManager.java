package edu.gemini.obslog.database.pot;

import edu.gemini.obslog.database.OlPersistenceException;
import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBIDClashException;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.plan.NightlyRecord;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.prog.GemPlanId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//
// Gemini Observatory/AURA
// $Id: OlPOTPersistenceManager.java,v 1.11 2005/12/11 15:54:15 gillies Exp $
//

public abstract class OlPOTPersistenceManager implements OlPersistenceManager, Serializable {

    public abstract IDBDatabaseService getDatabase() throws OlPersistenceException;

    /**
     * Routine for returning a program by program id
     *
     * @param progID the program that should be returned
     */
    public SPProgram getProgramByID(SPProgramID progID) throws NullPointerException, OlPersistenceException {
        if (progID == null) throw new NullPointerException("null progID");

        IDBDatabaseService db = getDatabase();
        if (db == null) throw new OlPersistenceException("failed to grasp database for program");

        SPProgram spProg;
        ISPProgram ispProg = db.lookupProgramByID(progID);
        spProg = (SPProgram) ispProg.getDataObject();
        return spProg;
    }

    /**
     * Return a list of <code>{@link SPObservationID}</code> objects.  One for each observation in a program.
     *
     * @param progID the program that should be returned
     * @return <code>List</code> of <code>{@link SPObservationID}</code> objects.
     */
    public List<SPObservationID> getProgramObservations(SPProgramID progID) throws NullPointerException, OlPersistenceException {
        if (progID == null) throw new NullPointerException("null progID");

        IDBDatabaseService db = getDatabase();
        if (db == null) throw new OlPersistenceException("failed to grasp database for program");

        List<SPObservationID> observationIDs = new ArrayList<SPObservationID>();
        ISPProgram ispProg = db.lookupProgramByID(progID);
        List<ISPObservation> obsList = ispProg.getAllObservations();

        for (ISPObservation spObs : obsList) {
            SPObservationID obsID = spObs.getObservationID();
            observationIDs.add(obsID);
        }
        return observationIDs;
    }

    /**
     * Return a nightly record in the database given its <tt>SPProgramID</tt>.
     *
     * @param recordId an instance of <tt>SPProgramID</tt>
     * @return the <tt>ISPNightlyRecord</tt> in the database.
     * @throws NullPointerException   if the recordId is null
     * @throws OlPersistenceException if a database problem occurred.
     */
    public ISPNightlyRecord getNightlyRecordNode(SPProgramID recordId) throws NullPointerException, OlPersistenceException {
        if (recordId == null) throw new NullPointerException("null recordId");

        IDBDatabaseService db = getDatabase();
        if (db == null) throw new OlPersistenceException("failed to grasp database for plan");

        ISPNightlyRecord spRecord;
        spRecord = db.lookupNightlyRecordByID(recordId);
        return spRecord;
    }


    public NightlyRecord getNightlyRecord(SPProgramID recordId) throws NullPointerException, OlPersistenceException {
        ISPNightlyRecord record = getNightlyRecordNode(recordId);
        if (record == null) {
            throw new OlPersistenceException("No record with ID: " + recordId.toString());
        }

        NightlyRecord obsLogObj;
        obsLogObj = (NightlyRecord) record.getDataObject();
        return obsLogObj;
    }

    public NightlyRecord createNightlyRecordBy(SPProgramID recordId) throws OlPersistenceException, OlLogException {
        if (recordId == null) throw new NullPointerException("null recordId");

        // Make sure this is a valid plan id, and that it isn't too far in the
        // future.
        Option<GemPlanId> gemPlanId = GemPlanId.parse(recordId);
        if (None.instance().equals(gemPlanId)) {
            throw new OlLogException("Invalid plan id: " + recordId);
        }
        GemPlanId today = GemPlanId.create(gemPlanId.getValue().getSite());
        if (gemPlanId.getValue().compareTo(today.tomorrow()) > 0) {
            throw new OlLogException("Future plan id: " + recordId);
        }


        IDBDatabaseService db = getDatabase();
        if (db == null) throw new OlPersistenceException("failed to grasp database for plan creation");
        ISPFactory fact = db.getFactory();
        ISPNightlyRecord ispNightlyRecord = fact.createNightlyRecord(new SPNodeKey(), recordId);
        // Set the plan title to the recordId
        ISPDataObject dObj = ispNightlyRecord.getDataObject();
        dObj.setTitle(recordId.stringValue());
        ispNightlyRecord.setDataObject(dObj);
        try {
            db.put(ispNightlyRecord);
        } catch (DBIDClashException ex) {
            throw new OlPersistenceException(ex);
        }
        return getNightlyRecord(recordId);
    }

    /**
     * Store an observing log back to its plan document
     *
     * @param recordId <code>SPProgramID</code> that is the plan to use
     * @param record the <code>NightlyRecord</code> data object to store
     */
    public void setNightlyRecord(SPProgramID recordId, NightlyRecord record) {
        ISPNightlyRecord recNode = getNightlyRecordNode(recordId);
        if (recNode == null) {
            throw new OlPersistenceException("No recNode with ID: " + recordId.toString());
        }
        recNode.setDataObject(record);
    }

    /**
     * Internal routine for returning an observation
     *
     * @param obsID the observation that should be returned
     */
    public ISPObservation getObservationByID(SPObservationID obsID)
            throws IllegalArgumentException, OlPersistenceException {

        if (obsID == null) throw new IllegalArgumentException();

        IDBDatabaseService db = getDatabase();
        if (db == null) return null;

        ISPObservation spObs;
        spObs = db.lookupObservationByID(obsID);
        return spObs;
    }

    public SPObservation getObservationDataByID(SPObservationID obsID) throws NullPointerException, OlPersistenceException {
        ISPObservation spObs = getObservationByID(obsID);
        SPObservation obs;
        obs = (SPObservation) spObs.getDataObject();
        return obs;
    }

    /**
     * Abstract method to allow subclasses to print some ID info.
     *
     * @return info string
     */
    public abstract String info();
}

