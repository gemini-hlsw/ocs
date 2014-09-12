//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.shared.util.immutable.*;

import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A {@link CatalogReader} that reads a Swing TableModel.  The
 * {@link CatalogHeader header} is taken directly from the table header and
 * the {@link CatalogRow rows} from the table rows.
 *
 * <p>This class was intended to be used to help ease the transition from jsky
 * code, which provides a catalog results object that implements TableModel.
 */
public class TableModelCatalogReader implements CatalogReader {
    private final TableModel table;
    private int rowIndex = 0;

    /**
     * Constructs with the backing TableModel whose data will be read.
     *
     * @param table containing catalog data
     */
    public TableModelCatalogReader(TableModel table) {
        this.table = table;
    }

    /**
     * Resets the iterator to the first row of the table.
     */
    @Override
    public void open() {
        rowIndex = 0;
    }

    /**
     * Constructs and returns a {@link CatalogHeader} based upon the table
     * columns in the TableModel.  If the TableModel has no columns,
     * {@link None} is returned.
     *
     * @return a {@link CatalogHeader} based upon the table header, or
     * {@link None} if the TableModel has no columns
     */
    @Override
    public Option<CatalogHeader> getHeader() {
        if (table.getColumnCount() == 0) return None.instance();

        List<Tuple2<String, Class>> lst;
        lst = new ArrayList<Tuple2<String, Class>>(table.getColumnCount());
        for (int col=0; col<table.getColumnCount(); ++col) {
            String name = table.getColumnName(col);
            Class  c    = table.getColumnClass(col);
            lst.add(new Pair<String, Class>(name, c));
        }

        return new Some<CatalogHeader>(new DefaultCatalogHeader(DefaultImList.create(lst)));
    }

    /**
     * Returns <code>true</code> if there are unvisited rows left in the
     * TableModel
     *
     * @return <code>true</code> if there are more rows left in the table;
     * <code>false</code> otherwise
     */
    @Override
    public boolean hasNext() {
        return rowIndex < table.getRowCount();
    }

    /**
     * Constructs and returns the next {@link CatalogRow}, taking the data
     * directly from the current table row.
     *
     * @return {@link CatalogRow} representing the next table row
     */
    @Override
    public CatalogRow next() {
        Object[] res = new Object[table.getColumnCount()];
        for (int c=0; c<res.length; ++c) {
            res[c] = table.getValueAt(rowIndex, c);
        }
        ++rowIndex;
        return new DefaultCatalogRow(DefaultImList.create(Arrays.asList(res)));
    }

    /**
     * Does nothing in this implementation of {@link CatalogReader}, since there
     * is no external connection.
     */
    @Override
    public void close() {
        // noop
    }
}
