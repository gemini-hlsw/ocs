//
// $Id: ProgramStateFunctor.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.dbTools.odbState;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.sp.SPNodeNotLocalException;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBSingleNodeFunctor;
import edu.gemini.shared.util.GeminiRuntimeException;
import edu.gemini.spModel.core.SPProgramID;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A functor that is used to create a current {@link ProgramState}
 * object.
 */
class ProgramStateFunctor extends DBAbstractFunctor implements IDBSingleNodeFunctor {

    private final SPNodeKey _key;
    private ProgramState _state;

    private ProgramStateFunctor(final SPNodeKey key) {
        _key = key;
    }

    ProgramState getProgramState() {
        return _state;
    }

    private ISPProgram _getProgram(final IDBDatabaseService db, final ISPNode node) {
        if (node != null) return (ISPProgram) node;
        if (_key == null) return null;
        return db.lookupProgram(_key);
    }

    public void execute(final IDBDatabaseService db, final ISPNode node, Set<Principal> principals) {
        final ISPProgram prog = _getProgram(db, node);
        if (prog == null) return;
        final SPProgramID progId = prog.getProgramID();
        if (progId == null) return;
        _state = new ProgramState(prog);
    }

    public static ProgramState getProgramState(final Logger log,
                                               final IDBDatabaseService db,
                                               final SPNodeKey programKey,
                                               final Set<Principal> user) {

        ProgramStateFunctor funct;
        funct = new ProgramStateFunctor(programKey);

        try {
            funct = db.getQueryRunner(user).execute(funct, null);
        } catch (SPNodeNotLocalException e) {
            // can't happen
            throw GeminiRuntimeException.newException(e);
        }

        final Exception ex = funct.getException();
        if (ex != null) {
            log.log(Level.SEVERE, "Failure getting program state", ex);
        }

        return funct.getProgramState();
    }

    public SPNodeKey getNodeKey() {
        return _key;
    }
}
