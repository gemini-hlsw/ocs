//
// $Id: TigraTableFunctor.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.dbTools.tigratable;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBQueryRunner;
import edu.gemini.sp.vcs.log.VcsLog;
import edu.gemini.spModel.core.*;

import java.security.Principal;
import java.util.*;

/**
 * A functor that is used to create {@link TigraTableRow}s representing each
 * queue program in the database.
 */
class TigraTableFunctor extends DBAbstractQueryFunctor {

    // Program types that should be included in the result.
    private static final Set<ProgramType> TYPE_SET;
    static {
        final Set<ProgramType> s = new HashSet<ProgramType>();
        s.add(ProgramType.Classical$.MODULE$);
        s.add(ProgramType.Queue$.MODULE$);
        s.add(ProgramType.LargeProgram$.MODULE$);
        s.add(ProgramType.FastTurnaround$.MODULE$);
        TYPE_SET = Collections.unmodifiableSet(s);
    }

    // semester string (e.g., GN_2004A) -> TigraTable
    private final SyncTimestamp syncTimestamp;
    private final Map<Semester, TigraTable> _tigraTableMap = new TreeMap<>();

    TigraTableFunctor(SyncTimestamp st) {
        this.syncTimestamp = st;
    }

    /**
     * Gets the TigraTable associated with the given semester key.
     *
     * @param semester a key of the form <pre>\\d\\d[AB]</pre>
     * @return associated TigraTable, creating it if necessary
     */
    TigraTable getTigraTable(final Semester semester) {
        TigraTable tt = _tigraTableMap.get(semester);
        if (tt == null) {
            tt = new TigraTable(semester.toShortString());
            _tigraTableMap.put(semester, tt);
        }
        return tt;
    }

    /**
     * Returns all the TigraTables created by the functor.
     *
     * @return List of {@link TigraTable}, sorted by semester key
     */
    List<TigraTable> getTigraTables() {
        return new ArrayList<TigraTable>(_tigraTableMap.values());
    }


    public void execute(final IDBDatabaseService db, final ISPNode node, Set<Principal> principals) {
        final ISPProgram prog = (ISPProgram) node;
        final SPProgramID progId = prog.getProgramID();
        if (progId == null) return;

        final ProgramId pid = ProgramId$.MODULE$.parse(progId.stringValue());

        if (!pid.semester().isDefined()) return;
        final Semester semesterKey = pid.semester().get();

        if (!pid.ptype().isDefined()) return;
        final ProgramType ptype = pid.ptype().get();
        if (TYPE_SET.contains(ptype)) {
            final TigraTable tt = getTigraTable(semesterKey);
            tt.addRow(TigraTableRow.create(prog, syncTimestamp));
        }
    }

    public static List<TigraTable> getTigraTables(final IDBDatabaseService db, final VcsLog vcs, final Set<Principal> user) {
        final SyncTimestamp     st    = new SyncTimestamp(vcs);
        final TigraTableFunctor funct = new TigraTableFunctor(st);
        final IDBQueryRunner    qr    = db.getQueryRunner(user);
        return qr.queryPrograms(funct).getTigraTables();
    }
}
