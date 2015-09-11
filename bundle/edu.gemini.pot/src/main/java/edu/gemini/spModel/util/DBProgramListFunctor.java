// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DBProgramListFunctor.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.util;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.*;
import edu.gemini.shared.util.immutable.PredicateOp;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;


import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Set;


/**
 * An <code>edu.gemini.pot.spdb.IDBQueryFunctor</code>
 * implementation that can be used by clients to obtain a listing
 * of all the available program IDs.
 *
 * @author Kim Gillies
 */
public final class DBProgramListFunctor extends DBAbstractQueryFunctor
        implements IDBProgramLister, IDBParallelFunctor {

    private static final class AcceptAll implements PredicateOp<ISPRootNode>, Serializable {
        @Override public Boolean apply(ISPRootNode rootNode) { return true; }
    }
    public static final PredicateOp<ISPRootNode> ALL = new AcceptAll();

    private static final class NonEmptyId implements PredicateOp<ISPRootNode>, Serializable {
        @Override public Boolean apply(ISPRootNode rootNode) {
            final SPProgramID id = rootNode.getProgramID();
            return (id != null) && !"".equals(id.stringValue().trim());
        }
    }
    public static final PredicateOp<ISPRootNode> NON_EMPTY_PROGRAM_ID = new NonEmptyId();


    private final PredicateOp<ISPRootNode> predicate;
    private List<DBProgramInfo> list = new ArrayList<DBProgramInfo>();

    public DBProgramListFunctor() {
        this(ALL);
    }

    public DBProgramListFunctor(PredicateOp<ISPRootNode> r) { this.predicate = r; }

    public List<DBProgramInfo> getList() { return list; }

    /**
     * Called once per program by the <code>{@link IDBQueryRunner}</code>
     * implementation, adds the given program's ID to the list.
     */
    public void execute(IDBDatabaseService database, ISPNode node, Set<Principal> principals) {
        final ISPRootNode prog = (ISPRootNode) node;
        if (predicate.apply(prog)) {
            final SPNodeKey key        = prog.getProgramKey();
            final ISPDataObject spProg = prog.getDataObject();
            final String name          = spProg.getTitle();
            final SPProgramID progID   = prog.getProgramID();
            final long size            = database.fileSize(key);
            final long timestamp       = prog.lastModified();

            if ((key != null) && (name != null)) {
                getList().add(new DBProgramInfo(key, name, progID, size, timestamp));
            }
        }
    }

    public void mergeResults(Collection<IDBFunctor> col) {
        List<DBProgramInfo> res = new ArrayList<DBProgramInfo>();
        for (IDBFunctor f : col) {
            res.addAll(((DBProgramListFunctor) f).getList());
        }
        list = res;
    }
}

