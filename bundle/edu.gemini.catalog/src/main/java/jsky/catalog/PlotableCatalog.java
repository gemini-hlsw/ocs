package jsky.catalog;

/**
 * Defines the interface for catalogs whose tabular query results
 * can be plotted on an image.
 *
 * @version $Revision: 4726 $
 * @author Allan Brighton
 */
public interface PlotableCatalog extends Catalog {

    /** Return the number of plot symbol definitions associated with this catalog. */
    int getNumSymbols();

    /** Return the ith plot symbol description */
    TablePlotSymbol getSymbolDesc(int i);

    /** Return an array of symbol descriptions, or null if none are defined. */
    TablePlotSymbol[] getSymbols();

    /** Set the plot symbol descriptions to use to plot tables returned from this catalog */
    void setSymbols(TablePlotSymbol[] symbols);

    /** Set to true if the user edited the plot symbol definitions (default: false) */
    void setSymbolsEdited(boolean edited);

    /** Return true if the user edited the plot symbol definitions otherwise false */
    boolean isSymbolsEdited();

}


