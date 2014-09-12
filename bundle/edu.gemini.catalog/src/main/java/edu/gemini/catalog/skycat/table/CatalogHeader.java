//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.shared.util.immutable.Option;

/**
 * Header or metadata information about a catalog's results.  Viewed as
 * tabular information, the CatalogHeader provides information on the
 * contents of the columns.
 */
public interface CatalogHeader {

    /**
     * Gets the number of pieces of information in the catalog results.
     */
    int getColumnCount();

    /**
     * Gets the index associated with the given name, if any.  Each catalog
     * item must have a unique name which can be mapped to its position in
     * each row.  This method provides access to that mapping.
     *
     * @param name name of the catalog information of interest
     *
     * @return the index where the named item can be founded wrapped in a
     * {@link edu.gemini.shared.util.immutable.Some} if any;
     * {@link edu.gemini.shared.util.immutable.None} otherwise
     */
    Option<Integer> columnIndex(String name);

    /**
     * Gets the name of the catalog information at the given index.
     *
     * @param columnIndex column index of interest
     *
     * @return name of the column at columnIndex
     */
    String getName(int columnIndex);

    /**
     * Gets the class of the catalog data at the given index.
     *
     * @param columnIndex column index of interest
     *
     * @return type of the column at columnIndex
     */
    Class getClass(int columnIndex);
}
