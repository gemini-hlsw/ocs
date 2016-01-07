package jsky.catalog;

import java.util.List;
import java.util.Vector;

import javax.swing.table.TableModel;

import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.util.immutable.Option;
import jsky.coords.Coordinates;
import jsky.coords.WorldCoordinates;

/**
 * This interface defines the methods required to access tabular query results.
 * It extends QueryResult, since it represents the result of a catalog query.
 * It extends TableModel to make it easy to display in a JTable.
 * It also extends Catalog, so that it is possible to search again in the result
 * of a previous query.
 */
public interface TableQueryResult extends Catalog, TableModel {

    /**
     * Returns the Vector of Vectors that contains the table's data values.
     * The vectors contained in the outer vector are each a single row of values.
     */
    Vector<Vector<Object>> getDataVector();

    /** Return a description of the ith table column field */
    FieldDesc getColumnDesc(int i);

    /** Return the table column index for the given column name */
    int getColumnIndex(String name);

    /** Return a vector of column headings for this table. */
    List<String> getColumnIdentifiers();

    /**
     * Return a Coordinates object based on the appropriate columns in the given row,
     * or null if there are no coordinates available for the row.
     */
    Coordinates getCoordinates(int rowIndex);

    /**
     * Return an object describing the columns that can be used to search
     * this catalog.
     */
    RowCoordinates getRowCoordinates();

    /**
     * Return the center coordinates for this table from the query arguments,
     * if known, otherwise return the coordinates of the first row, or null
     * if there are no world coordinates available.
     */
    WorldCoordinates getWCSCenter();

    /**
     * Return the object representing the arguments to the query that resulted in this table,
     * if known, otherwise null.
     */
    QueryArgs getQueryArgs();

    /**
     * Set the object representing the arguments to the query that resulted in this table.
     */
     void setQueryArgs(QueryArgs queryArgs);

    /** Return true if the result was truncated and more data would have been available */
    boolean isMore();

    /**
     * Return the catalog used to create this table,
     * or a dummy, generated catalog object, if not known.
     */
    Catalog getCatalog();

    /**
     * Return a possible SkyObject for the row i
     */
    Option<SkyObject> getSkyObject(int i);

}
