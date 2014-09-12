package edu.gemini.spdb.shell.misc;


import edu.gemini.pot.sp.ISPNode;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;

import java.security.Principal;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public final class LsProgs extends DBAbstractQueryFunctor {

	private final SortedSet<SPProgramID> ids = new TreeSet<SPProgramID>();

    public void execute(IDBDatabaseService arg0, ISPNode arg1, Set<Principal> principals) {
        SPProgramID id = arg1.getProgramID();
        if (id != null)
            ids.add(id);
	}

	public SortedSet<SPProgramID> ids() {
		return ids;
    }
}
