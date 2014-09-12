//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 * Test cases for {@link TableModelCatalogReader}.
 */
public final class TableModelCatalogReaderTest {
    @Test
    public void testNoHeader() {
        TableModel tm = new DefaultTableModel();
        TableModelCatalogReader rdr = new TableModelCatalogReader(tm);
        assertEquals(None.INSTANCE, rdr.getHeader());
    }

    private static TableModelCatalogReader sampleReader() {
        return new TableModelCatalogReader(SampleData.createTableModel());
    }

    @Test
    public void testHeader() {
        TableModelCatalogReader rdr = sampleReader();
        Option<CatalogHeader> hdrOpt = rdr.getHeader();
        assertFalse(hdrOpt.isEmpty());

        CatalogHeader hdr = hdrOpt.getValue();
        assertEquals(SampleData.TABLE_HEADER.length, hdr.getColumnCount());

        assertEquals(new Some<Integer>(0), hdr.columnIndex("2MASS"));
        assertEquals(new Some<Integer>(3), hdr.columnIndex("Jmag"));
        assertEquals(None.INSTANCE, hdr.columnIndex("doesn't exist"));

        assertEquals("2MASS", hdr.getName(0));
        assertEquals("Jmag", hdr.getName(3));

        // DefaultTableModel treats all columns as Object
        assertEquals(Object.class, hdr.getClass(0));
        assertEquals(Object.class, hdr.getClass(3));
    }

    @Test
    public void testNoRows() {
        TableModel tm = new DefaultTableModel(new Object[][] {}, new Object[] {
                "ID", "RA", "Dec"
        });
        TableModelCatalogReader rdr = new TableModelCatalogReader(tm);
        assertFalse(rdr.hasNext());
    }

    @Test
    public void testHasNext() {
        TableModelCatalogReader rdr = sampleReader();

        int count = 0;
        while (rdr.hasNext()) {
            rdr.next();
            ++count;
        }

        assertEquals(count, SampleData.TABLE_DATA.length);
    }

    @Test
    public void testNext() throws CatalogException {
        TableModelCatalogReader rdr = sampleReader();

        int r = 0;
        while (rdr.hasNext()) {
            CatalogRow row = rdr.next();

            for (int c=0; c<SampleData.TABLE_DATA[r].length; ++c) {
                assertEquals(SampleData.TABLE_DATA[r][c], row.get(c).getValue());
            }
            ++r;
        }
    }

}
