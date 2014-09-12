package edu.gemini.spdb.reports;

import java.util.List;

import edu.gemini.pot.spdb.IDBDatabaseService;

/**
 * Interface for a service that can create and execute queries.
 * @author rnorris
 */
public interface IQueryManager {

	/**
	 * Creates a new IQuery on the specified table.
	 * @param table
	 * @return a new query
	 */
	IQuery createQuery(ITable table);	
	
	/**
	 * Runs the specified query on the specified database, returning a 
	 * list of rows. The passed query must have been created by this
	 * query manager via createQuery().
	 * @param query a query created by this query manager
	 * @param dbs a database
	 */
	List<IRow> runQuery(IQuery query, IDBDatabaseService dbs);
	
}
	

