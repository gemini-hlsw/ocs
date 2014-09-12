/*
 * ESO Archive
 *
 * $Id: QueryResult.java 4414 2004-02-03 16:21:36Z brighton $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/17  Created
 */

package jsky.catalog;


/**
 * Represents the result of a catalog query. We don't
 * make any assumptions here about the type of the query result. It
 * could be tabular data, an image, or HTML file, etc. It is up to
 * the implementing classes to define the rest.
 */
public abstract interface QueryResult {

}
