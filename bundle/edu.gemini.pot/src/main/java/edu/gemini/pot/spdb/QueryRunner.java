package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.*;

import java.security.Principal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The query runner implementation.  See
 * <code>{@link IDBQueryRunner}</code> for more details.
 */
final class QueryRunner implements IDBQueryRunner {
    private static final Logger LOG = Logger.getLogger(QueryRunner.class.getName());

    private DBLocalDatabase _database;
    private DatabaseManager _dataMan;
    private Set<Principal> _principals;

    /**
     * Constructs with the database manager.
     */
    QueryRunner(DBLocalDatabase database, DatabaseManager dataMan, Set<Principal> principals) {
        _database = database;
        _dataMan = dataMan;
        _principals = principals;
    }

    /**
     * Runs a query on the available observations.
     */
    public <T extends IDBQueryFunctor> T queryObservations(T queryFunctor) {
        List<ISPObservation> lst = new LinkedList<>();
        for (ISPProgram prog : _dataMan.getProgramManager().getPrograms()) {
            lst.addAll(prog.getAllObservations());
        }
        return _doQuery(lst, queryFunctor);
    }

    /**
     * Runs a query on the available programs.
     */
    public <T extends IDBQueryFunctor> T queryPrograms(T queryFunctor) {
        List<ISPProgram> lst = _dataMan.getProgramManager().getPrograms();
        return _doQuery(lst, queryFunctor);
    }

    /**
     * Runs a query on the available nightly plans.
     */
    public <T extends IDBQueryFunctor> T queryNightlyPlans(T queryFunctor) {
        List<ISPNightlyRecord> lst = _dataMan.getNightlyPlanManager().getPrograms();
        return _doQuery(lst, queryFunctor);
    }

    public <T extends IDBFunctor> T execute(T functor, ISPNode node) throws SPNodeNotLocalException {
        return _database.execute(functor, node, _principals);
    }

    /**
     * Runs the query on the given node list using the given functor.
     */
    <T extends IDBQueryFunctor> T _doQuery(final List<? extends ISPNode> nodeList, final T queryFunctor) {
        WithPriority.exec(queryFunctor.getPriority(), () -> {
            Iterator<? extends ISPNode> it = nodeList.iterator();
            FunctorLogger.Handback hb = _dataMan.functorLogger.logQueryStart(queryFunctor);
            try {
                queryFunctor.init();
                while (!queryFunctor.isDone() && it.hasNext()) {
                    ISPNode node = it.next();
                    queryFunctor.execute(_database, node, _principals);
                }
                queryFunctor.finished();
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Problem running functor: " + queryFunctor, ex);
                queryFunctor.setException(ex);
            }
            _dataMan.functorLogger.logQueryEnd(queryFunctor, hb);
        });
        return queryFunctor;
    }

}
