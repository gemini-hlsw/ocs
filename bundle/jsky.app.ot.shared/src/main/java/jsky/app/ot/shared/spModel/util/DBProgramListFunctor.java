// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DBProgramListFunctor.java 46768 2012-07-16 18:58:53Z rnorris $
//


package jsky.app.ot.shared.spModel.util;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPRootNode;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.shared.util.immutable.Trio;
import edu.gemini.shared.util.immutable.Tuple3;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBFunctor;
import edu.gemini.pot.spdb.IDBParallelFunctor;
import edu.gemini.shared.util.immutable.PredicateOp;
import edu.gemini.shared.util.immutable.Pair;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.util.DBProgramInfo;
import edu.gemini.spModel.util.IDBProgramLister;
import edu.gemini.util.security.permission.ProgramPermission;
import edu.gemini.util.security.policy.ImplicitPolicyForJava;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

// RCN: this was moved into a bundle that has access to the security stuff (spModel doesn't at the moment).


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

    private static final class NotPredicate implements PredicateOp<ISPRootNode>, Serializable {
        private final PredicateOp<ISPRootNode> p;
        NotPredicate(PredicateOp<ISPRootNode> p) { this.p = p; }
        @Override public Boolean apply(ISPRootNode rootNode) { return !p.apply(rootNode); }
    }

    private static final class EmptyId implements PredicateOp<ISPRootNode>, Serializable {
        @Override public Boolean apply(ISPRootNode rootNode) {
            final SPProgramID id = rootNode.getProgramID();
            return (id == null) || "".equals(id.stringValue().trim());
        }
    }
    public static final PredicateOp<ISPRootNode> EMPTY_PROGRAM_ID = new EmptyId();
    public static final PredicateOp<ISPRootNode> NON_EMPTY_PROGRAM_ID = new NotPredicate(EMPTY_PROGRAM_ID);

    private static final class NonEmptyIdAndReadable implements PredicateOp<Tuple3<IDBDatabaseService, ISPRootNode, Set<Principal>>>, Serializable {
        @Override public Boolean apply(Tuple3<IDBDatabaseService, ISPRootNode, Set<Principal>> t) {
            final IDBDatabaseService db = t._1();
            final ISPRootNode root = t._2();
            return NON_EMPTY_PROGRAM_ID.apply(root) &&
               ImplicitPolicyForJava.hasPermission(db, t._3(), new ProgramPermission.Read(root.getProgramID()));
        }
    }
    public static final PredicateOp<Tuple3<IDBDatabaseService, ISPRootNode, Set<Principal>>> NON_EMPTY_PROGRAM_ID_AND_READABLE =
            new NonEmptyIdAndReadable();

    private static final class EmptyIdOrReadable implements PredicateOp<Tuple3<IDBDatabaseService, ISPRootNode, Set<Principal>>>, Serializable {
        @Override public Boolean apply(Tuple3<IDBDatabaseService, ISPRootNode, Set<Principal>> t) {
            final IDBDatabaseService db = t._1();
            final ISPRootNode root = t._2();
            return EMPTY_PROGRAM_ID.apply(root) ||
               ImplicitPolicyForJava.hasPermission(db, t._3(), new ProgramPermission.Read(root.getProgramID()));
        }
    }
    public static final PredicateOp<Tuple3<IDBDatabaseService, ISPRootNode, Set<Principal>>> EMPTY_PROGRAM_ID_OR_READABLE =
            new EmptyIdOrReadable();

    private final PredicateOp<Tuple3<IDBDatabaseService, ISPRootNode, Set<Principal>>> predicate;
    private List<DBProgramInfo> list = new ArrayList<DBProgramInfo>();

    public static DBProgramListFunctor forAllPrograms() {
        return forSimplePredicate(ALL);
    }

    public static DBProgramListFunctor forSimplePredicate(final PredicateOp<ISPRootNode> r) {
        return forFullPredicate(new PredicateOp<Pair<IDBDatabaseService, ISPRootNode>>() {
            public Boolean apply(Pair<IDBDatabaseService, ISPRootNode> pair) {
                return r.apply(pair._2());
            }
        });
    }

    public static DBProgramListFunctor forFullPredicate(final PredicateOp<Pair<IDBDatabaseService, ISPRootNode>> r) {
        return new DBProgramListFunctor(new PredicateOp<Tuple3<IDBDatabaseService, ISPRootNode, Set<Principal>>>() {
            @Override
            public Boolean apply(Tuple3<IDBDatabaseService, ISPRootNode, Set<Principal>> t) {
                return r.apply(new Pair<IDBDatabaseService, ISPRootNode>(t._1(), t._2()));
            }
        });
    }

    public DBProgramListFunctor(PredicateOp<Tuple3<IDBDatabaseService, ISPRootNode, Set<Principal>>> r) {
        this.predicate = r;
    }


    public List<DBProgramInfo> getList() { return list; }

    /**
     * Called once per program by the <code>{@link edu.gemini.pot.spdb.IDBQueryRunner}</code>
     * implementation, adds the given program's ID to the list.
     */
    public void execute(IDBDatabaseService database, ISPNode node, Set<Principal> principals) {
        final ISPRootNode prog = (ISPRootNode) node;
        if (predicate.apply(new Trio<IDBDatabaseService, ISPRootNode, Set<Principal>>(database, prog, principals))) {
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

