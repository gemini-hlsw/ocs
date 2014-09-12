// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DBProgramListFunctor.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPNodeKey;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;


/**
 * An <code>{@link IDBQueryFunctor}</code> implementation that can
 * be used by clients to obtain a listing of all the available program
 * IDs.
 */
public class DBProgramListFunctor extends DBAbstractQueryFunctor implements IDBParallelFunctor {

    private List<SPNodeKey> _keyList;

    /**
     * Gets the list of program IDs.
     */
    public List<SPNodeKey> getKeyList() {
        if (_keyList == null) {
            _keyList = new ArrayList<SPNodeKey>();
        }
        return _keyList;
    }

    /**
     * Called once per program by the <code>{@link IDBQueryRunner}</code>
     * implementation, adds the given program's ID to the list.
     */
    public void execute(IDBDatabaseService database, ISPNode node, Set<Principal> principals) {
        ISPProgram prog = (ISPProgram) node;
        SPNodeKey key = prog.getProgramKey();
        if (key != null) {
            getKeyList().add(key);
        }
    }

    public void mergeResults(Collection<IDBFunctor> functorCollection) {
        List<SPNodeKey> res = new ArrayList<SPNodeKey>();
        for (IDBFunctor f : functorCollection) {
            res.addAll(((DBProgramListFunctor) f).getKeyList());
        }
        _keyList = res;
    }
}

