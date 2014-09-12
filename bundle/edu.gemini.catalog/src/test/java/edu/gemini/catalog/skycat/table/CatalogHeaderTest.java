//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.skycalc.Angle;
import edu.gemini.shared.util.immutable.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 */
public final class CatalogHeaderTest {

    @Test
    public void testBasics() {
        Tuple2<String, Class> id, ra, dec;
        id = new Pair<String, Class>("id", String.class);
        ra = new Pair<String, Class>("ra", Angle.class);
        dec = new Pair<String, Class>("dec", Angle.class);

        ImList<Tuple2<String,Class>> headerList = DefaultImList.create(id, ra, dec);

        DefaultCatalogHeader header = new DefaultCatalogHeader(headerList);

        assertEquals(3, header.getColumnCount());

        assertEquals(0, header.columnIndex("id").getValue().intValue());
        assertEquals("id", header.getName(0));
        assertEquals(String.class, header.getClass(0));

        assertEquals(1, header.columnIndex("ra").getValue().intValue());
        assertEquals("ra", header.getName(1));
        assertEquals(Angle.class, header.getClass(1));

        assertEquals(2, header.columnIndex("dec").getValue().intValue());
        assertEquals("dec", header.getName(2));
        assertEquals(Angle.class, header.getClass(2));

        // Look for the index of a name that doesn't exist.
        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(None.instance(), header.columnIndex("x"));

        // Try to get the name for a column that doesn't exit
        try {
            header.getName(3);
            fail();
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }
    }
}
