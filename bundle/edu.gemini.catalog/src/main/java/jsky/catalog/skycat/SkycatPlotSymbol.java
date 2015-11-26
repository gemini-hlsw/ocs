package jsky.catalog.skycat;

import jsky.catalog.RowCoordinates;
import jsky.catalog.TablePlotSymbol;
import jsky.catalog.TableQueryResult;

/**
 * Represents the contents of a plot symbol definition,
 * as defined in a skycat catalog config file.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public class SkycatPlotSymbol extends TablePlotSymbol {

    /**
     * Parses the given fields from the plot symbol definition in the
     * skycat catalog config file and makes the values available via methods.
     * Default values are filled in where needed.
     *
     * @param table contains the table data and information
     * @param cols a Tcl list of column names that may be used in symbol expressions
     * @param symbol a Tcl list of the form {shape color ratio angle label condition}
     * @param expr a Tcl list of the form {sizeExpr units}
     */
    public SkycatPlotSymbol(SkycatTable table, String cols, String symbol, String expr) {
        super(table, cols, symbol, expr);
    }

    /** Return an object storing the column indexes where RA and Dec are found */
    public RowCoordinates getRowCoordinates() {
        TableQueryResult table = getTable();
        if (table instanceof SkycatTable) {
            SkycatTable t = (SkycatTable) table;
            return t.getConfigEntry().getRowCoordinates();
        }
        return super.getRowCoordinates();
    }

    /** Return the index of the center position RA column */
    public int getRaCol() {
        return getRowCoordinates().getRaCol();
    }

    /** Return the index of the center position Dec column */
    public int getDecCol() {
        return getRowCoordinates().getDecCol();
    }
}
