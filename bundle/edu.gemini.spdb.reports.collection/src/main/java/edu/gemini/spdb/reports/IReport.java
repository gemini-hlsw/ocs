package edu.gemini.spdb.reports;

import edu.gemini.pot.spdb.IDBDatabaseService;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Represents a report with custom formatting or other unusual features
 * that can't easily be reproduced using the ad-hoc querying feature.
 * @author rnorris
 */
public interface IReport {

	/**
	 * Service property for this report's unique ID.
	 */
	String SERVICE_PROP_ID = "report.id";

	/**
	 * Service property for the table this report runs against.
	 */
	String SERVICE_PROP_TABLE_ID = "report.table.id";

	/**
	 * Called by the framework to allow the report to configure the query
	 * before it is run. By default the query will simply be the default
	 * query for this report's specified table.
	 * @param q a query
	 */
	void configureQuery(IQuery q);

	/**
	 * Called by the framework to execute the report. Implementations should
	 * write out results in the passed parentDir using names that are unique
	 * enough not to clobber other report output being written to the same
	 * place. A report can create any number of output files, but all should
	 * be returned so the framework knows what happened.
	 * @param query the query definition that has been executed already
	 * @param results a map of results for each registered database
	 * @param parentDir the output directory where report files should go
	 * @return a List of File objects
	 * @throws IOException
	 */
	List<File> execute(IQuery query, Map<IDBDatabaseService, List<IRow>> results, File parentDir) throws IOException;

	/**
	 * Should return true if the report is intended for public consumption.
	 * This is a hint to the framework that the output files should be copied
	 * to the public web server.
	 * @return true if the report should be published externally
	 */
	boolean isPublic();

}


