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
        final Tuple2<String, Class<?>> id = new Pair<String, Class<?>>("id", String.class);
        final Tuple2<String, Class<?>> ra = new Pair<String, Class<?>>("ra", Angle.class);
        final Tuple2<String, Class<?>> dec = new Pair<String, Class<?>>("dec", Angle.class);

        final ImList<Tuple2<String,Class<?>>> headerList = DefaultImList.create(id, ra, dec);

        final DefaultCatalogHeader header = new DefaultCatalogHeader(headerList);

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
