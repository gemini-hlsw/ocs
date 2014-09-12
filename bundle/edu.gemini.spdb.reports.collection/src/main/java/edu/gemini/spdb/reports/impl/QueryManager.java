package edu.gemini.spdb.reports.impl;


import java.util.List;
import java.util.Collections;
import java.security.Principal;
import java.util.Set;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBQueryRunner;
import edu.gemini.spdb.reports.IQuery;
import edu.gemini.spdb.reports.IQueryManager;
import edu.gemini.spdb.reports.IRow;
import edu.gemini.spdb.reports.ITable;
import edu.gemini.spdb.reports.ITable.Domain;


/**
 * Internal implementation of IQueryManager.
 * @author rnorris
 */
@SuppressWarnings("serial")
public class QueryManager implements IQueryManager {

    private final Set<Principal> user;

	public QueryManager(Set<Principal> user) {
        this.user = user;
	}

	public IQuery createQuery(ITable table) {
		return new Query(table);
	}

	/**
	 * Runs the specified query by wrapping it in a query functor and
	 * either calling it directly with null for NULL domains, or
	 * passing it to a query runner via queryObservations() or
	 * queryPrograms().
	 */
	public List<IRow> runQuery(IQuery query, IDBDatabaseService dbs) {
        QueryFunctor func = new QueryFunctor(query);
        Domain domain = query.getTable().getDomain();
        if (domain == Domain.NULL) {
            func.init();
            func.execute(null, null, Collections.<Principal>emptySet());
            func.finished();
        } else {
            IDBQueryRunner runner = dbs.getQueryRunner(user);
            switch (domain) {
            case OBSERVATION:
                func = runner.queryObservations(func);
                break;
            case PROGRAM:
                func = runner.queryPrograms(func);
                break;
            default:
                throw new Error("Impossible");
            }
        }
        return func.getResults();
	}

}
