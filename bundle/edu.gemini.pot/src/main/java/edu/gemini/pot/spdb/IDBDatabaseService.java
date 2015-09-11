// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: IDBDatabaseService.java 47005 2012-07-26 22:35:47Z swalker $
//

package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.core.SPProgramID;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;


/**
 * This is the main public interface for the Science Program database.
 * It is implemented by the database service's proxy, and by the database
 * implementation object <code>{@link DBLocalDatabase}</code>.  Clients of
 * either the remote database or a locally created database should be
 * programmed to this interface.
 */
public interface IDBDatabaseService {

    /**
     * Gets the UUID associated with this database instance.
     */
    public UUID getUuid();

    /**
     * Adds a program to the database.  Programs created by the factory
     * are not immediately/automatically added.  This method must be called
     * in order to make the program persistent and available to other
     * applications.  If <code>program</code> refers to a program that is
     * already in the database, nothing is done.  If <code>program</code>
     * has the same key as an existing program, the existing program is
     * replaced with the given one.
     *
     * @return the existing program with the same key that has been replaced
     * or <code>null</code> if none
     */
    ISPProgram put(ISPProgram program) throws DBIDClashException;

    /**
     * Removes the given <code>program</code> from the database, if it is
     * in fact in the database.  If not, nothing is done.  Note that removing
     * a program from the database does not make it unavailable to clients
     * that already have a reference to it.  It may be subsequently modified
     * or even added back to the database in the future.  However, it will no
     * longer be persisted until re-added to the database.
     *
     * @return <code>true</code> if the database is actually modified as a
     *         result of this call
     */
    boolean remove(ISPProgram program);

    /**
     * Removes the program with the given <code>programKey</code> from the
     * database.  If there is no matching program in the database, nothing
     * is done.  Note that removing a program from the database does not
     * make it unavailable to clients that already have a reference to it.
     * It may be subsequently modified or even added back to the database
     * in the future.  However, it will no longer be persisted until
     * re-added to the database.
     *
     * @return <code>true</code> if a matching program was actually removed
     *         and the database was modified
     */
    ISPProgram removeProgram(SPNodeKey programKey);

    /**
     * Adds a "program event" listener that will receive
     * <code>{@link ProgramEvent}</code>s whenever a program is added to,
     * replaced in, or or removed from the database.
     * @param pel  the listener that will receive the events
     */
    void addProgramEventListener(ProgramEventListener<ISPProgram> pel);

    /**
     * Removes the given <code>ProgramEventListener</code> from the list of
     * clients registered to receive program events.  If the given listener
     * is not in fact registered, nothing is done.
     *
     * @param pel the <code>ProgramEventListener</code> client that should
     *            no longer receive the events
     */
    void removeProgramEventListener(ProgramEventListener<ISPProgram> pel);

    /**
     * Adds a nightly record to the database.  NightlyPlans created by the
     * factory are not immediately/automatically added.  This method must be
     * called in order to make the nightly record persistent and available to
     * other applications.  If <code>nightly record</code> refers to a nightly
     * record that is already in the database, nothing is done.  If
     * <code>record</code> has the same key as an existing record, the existing
     * record is replaced with the given one.
     *
     * @return the existing record with the same key that has been replaced or
     * <code>null</code> if none
     */
    ISPNightlyRecord put(ISPNightlyRecord record) throws DBIDClashException;

    /**
     * Removes the given <code>nightly plan</code> from the database, if it is
     * in fact in the database.  If not, nothing is done.  Note that removing
     * a nightly plan from the database does not make it unavailable to clients
     * that already have a reference to it.  It may be subsequently modified
     * or even added back to the database in the future.  However, it will no
     * longer be persisted until re-added to the database.
     *
     * @return <code>true</code> if the database is actually modified as a
     *         result of this call
     */
    boolean remove(ISPNightlyRecord nightlyRecord);

    /**
     * Removes the nightly plan with the given <code>nightlyRecordKey</code> from
     * the database.  If there is no matching nightlyPlan in the database,
     * nothing is done.  Note that removing a nightly plan from the database
     * does not make it unavailable to clients that already have a reference to
     * it. It may be subsequently modified or even added back to the database
     * in the future.  However, it will no longer be persisted until
     * re-added to the database.
     *
     * @return <code>true</code> if a matching nightly plan was actually removed
     *         and the database was modified
     */
    boolean removeNightlyRecord(SPNodeKey nightlyRecordKey);

    /**
     * Adds a "nighly plan event" listener that will receive
     * <code>{@link ProgramEvent}</code>s whenever a nightly plan is added to,
     * replaced in, or removed from the database.
     *
     * @param rel the listener that will receive the events
     */
    void addNightlyRecordEventListener(ProgramEventListener<ISPNightlyRecord> rel);

    /**
     * Removes the given <code>ProgramEventListener</code> from the list of
     * clients registered to receive nightly plan events.  If the given listener
     * is not in fact registered, nothing is done.
     *
     * @param rel the <code>ProgramEventListener</code> client that should
     *            no longer receive the events
     */
    void removeNightlyRecordEventListener(ProgramEventListener<ISPNightlyRecord> rel);


    /**
     * Checkpoints all outstanding modifications to all programs.
     */
    void checkpoint();

    /**
     * Checkpoints the given program, storing any outstanding modifications
     * to it.
     */
    void checkpoint(ISPProgram prog);

    /**
     * Gets the database administration object.
     */
    IDBAdmin getDBAdmin();

    /**
     * Gets a reference to the factory that can be used to create new
     * items in this database.
     */
    ISPFactory getFactory();

    /**
     * Gets a reference to the query runner used for executing functors on
     * behalf of the given user.
     */
    IDBQueryRunner getQueryRunner(Set<Principal> user);

    /**
     * Registers a trigger action under the given condition.  When the condition
     * is met, the action is executed.  A Lease for the trigger registration is
     * returned, which the client must keep up-to-date in order to maintain the
     * registration as valid.
     *
     * @param condition     condition under which the trigger action should be
     *                      executed
     * @param action        the action to perform when the condition is met
     */
    void registerTrigger(IDBTriggerCondition condition, IDBTriggerAction action);

    void unregisterTrigger(IDBTriggerCondition condition, IDBTriggerAction action);

    /**
     * Finds the program node key associated with the given program id, if any.
     * @return the program key that identifies the program with the given
     * program id if any; <code>null</code> otherwise
     */
    SPNodeKey lookupProgramKeyByID(SPProgramID programID);

    /**
     * Fetches the observation by its id.
     *
     * @return the observation with the given id, if any;
     *         <code>null</code> otherwise
     */
    ISPObservation lookupObservationByID(SPObservationID obsID);

    /**
     * Fetches the program with the given <code>programKey</code> from the
     * database.
     *
     * @return the program with the given <code>programKey</code>, if any;
     *         <code>null</code> otherwise
     */
    ISPProgram lookupProgram(SPNodeKey programKey);

    /**
     * Fetches the program by its reference id.
     *
     * @return the program with the given reference id, if any;
     *         <code>null</code> otherwise
     */
    ISPProgram lookupProgramByID(SPProgramID programID);

    /**
     * Fetches the nightly plan with the given <code>programKey</code> from the
     * database.
     *
     * @return the nightly plan with the given <code>programKey</code>, if any;
     *         <code>null</code> otherwise
     */
    ISPNightlyRecord lookupNightlyPlan(SPNodeKey programKey);

    /**
     * Fetches the nightly plan by its reference id.
     *
     * @return the nightly plan with the given reference id, if any;
     *         <code>null</code> otherwise
     */
    ISPNightlyRecord lookupNightlyRecordByID(SPProgramID nightlyPlanID);

    long fileSize(SPNodeKey key);
}

