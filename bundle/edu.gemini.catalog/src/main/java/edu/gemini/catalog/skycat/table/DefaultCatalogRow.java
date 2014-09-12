//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;

/**
 * Provides an implementation of a {@link CatalogRow} backed by an immutable
 * list (see {@link ImList}).
 */
public class DefaultCatalogRow extends AbstractCatalogRow {
    private ImList<Object> row;

    /**
     * Constructs with the immutable list from which the row data will be
     * extracted.
     */
    public DefaultCatalogRow(ImList<Object> row) {
        this.row = row;
    }

    @Override
    public Option<Object> get(int columnIndex) {
        Object res = row.get(columnIndex);
        if (res == null) return None.instance();
        return new Some<Object>(res);
    }

    @Override
    public String toString() { return row.mkString("Catalog Row: [", ", ", "]"); }
}
