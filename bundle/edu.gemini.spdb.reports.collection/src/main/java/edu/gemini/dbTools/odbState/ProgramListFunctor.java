// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ProgramListFunctor.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.dbTools.odbState;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.spdb.*;
import edu.gemini.spModel.core.SPProgramID;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * This is a program list functor, like
 * {@link DBProgramListFunctor}, but which only lists
 * programs with valid program ids.
 */
class ProgramListFunctor extends DBAbstractQueryFunctor implements IDBParallelFunctor {

    /**
     * A simple pair of SPNodeKey and SPProgramID that refers to a particular
     * program in the database.
     */
    public static final class ProgramRef implements Serializable {
        private static final long serialVersionUID = 1L;

        private final SPNodeKey _key;
        private final SPProgramID _id;

        public ProgramRef(final SPNodeKey key, final SPProgramID id) {
            _key = key;
            _id = id;
        }

        public SPNodeKey getKey() {
            return _key;
        }

        public SPProgramID getId() {
            return _id;
        }
    }

    private List<ProgramRef> _refList;

    /**
     * Gets the list of program IDs.
     */
    List<ProgramRef> getRefList() {
        if (_refList == null) {
            _refList = new ArrayList<ProgramRef>();
        }
        return _refList;
    }

    /**
     * Called once per program by the <code>{@link IDBQueryRunner}</code>
     * implementation, adds the given program's ID to the list.
     */
    public void execute(final IDBDatabaseService database, final ISPNode node, Set<Principal> principals) {
        final ISPProgram prog = (ISPProgram) node;
        final SPNodeKey key;
        final SPProgramID id;
//        try {
            key = prog.getProgramKey();
            id = prog.getProgramID();
//        } catch (RemoteException ex) {
//            throw new RuntimeException("remote exception in functor");
//        }

        if (id != null) {
            getRefList().add(new ProgramRef(key, id));
        }
    }

    public void mergeResults(final Collection<IDBFunctor> functorCollection) {
        final List<ProgramRef> res = new ArrayList<ProgramRef>();

        for (final IDBFunctor fun : functorCollection) {
            final ProgramListFunctor plf = (ProgramListFunctor) fun;
            res.addAll(plf.getRefList());
        }

        _refList = res;
    }

    /**
     * Returns a list of the program IDs contained in the specified database.
     * @return List of {@link ProgramRef}
     */
    public static List<ProgramRef> getProgramRefs(final IDBDatabaseService db, Set<Principal> user) {
        return db.getQueryRunner(user).queryPrograms(new ProgramListFunctor()).getRefList();
    }
}

