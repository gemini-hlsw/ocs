package edu.gemini.spdb.reports;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a tabular slice of some database (normally an SPDB) which 
 * may not be intrinsically tabular. Basically the ITable represents a 
 * translation from some domain element (such as a Science Program) to
 * zero or more tabular rows.
 * @author rnorris
 *
 */
public interface ITable extends Serializable {

	/**
	 * OSGi service property that should contain the table's unique name.
	 */
	String SERVICE_PROP_ID = "table.id";

	/**
	 * An ITable operates on a specific domain. More may be added.
	 */
	enum Domain { 
		
		/** Transform an ISPProgram to 0 or more rows. */
		PROGRAM, 

		/** Transform an ISPObservation to 0 or more rows. */
		OBSERVATION, 
		
		/** Transform null to 0 or more rows. */
		NULL 
	}
	

	/**
	 * Should return all supported columns. The default display order will be
	 * determined by the iteration order of the returned set, so implementations
	 * may wish to use a LinkedHashSet or something like that.
	 * @return a Set of IColumn
	 */
	Set<IColumn> getColumns();
	
	/**
	 * Should return the Domain on which the table operates.
	 * @return a Domain
	 */
	Domain getDomain();

	/**
	 * Mapping function that turns a domain object (determined by the 
	 * return value of getDomain()) into zero or more output rows, where
	 * each row is simply a mapping from IColumn to Object. Note that for
	 * SPDB domains, the objects will be sent across the wire so they 
	 * need to be serializable. Basically this gets wrapped up in a
	 * functor.
	 * @param domainObject
	 * @return a List of rows, each of which is a Map from IColumn to Object
	 */
	List<Map<IColumn, Object>> getRows(Object domainObject);

	/**
	 * Returns the table's display name, for display purposes. This should
	 * probably be a service property instead.
	 */
	String getDisplayName();

	/**
	 * Returns the table's display description, for display purposes. This should
	 * probably be a service property instead.
	 */
	String getShortDescription();

}
