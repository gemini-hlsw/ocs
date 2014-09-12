//
// $
//

package edu.gemini.catalog.skycat.binding.skyobj;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.catalog.skycat.table.CatalogHeader;
import edu.gemini.catalog.skycat.table.DefaultCatalogHeader;
import edu.gemini.catalog.skycat.table.DefaultCatalogRow;
import edu.gemini.shared.skyobject.Magnitude;
import static edu.gemini.shared.skyobject.Magnitude.Band.B;
import static edu.gemini.shared.skyobject.Magnitude.Band.I;
import static edu.gemini.shared.skyobject.Magnitude.Band.R;
import static edu.gemini.shared.skyobject.Magnitude.Band.V;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.Pair;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Tuple2;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 */
public final class Gsc2EsoSkyObjectFactoryTest {

    private final CatalogHeader header;

    public Gsc2EsoSkyObjectFactoryTest() {
        Tuple2<String, Class> id, ra, dec, b, i, r, v;
        id  = new Pair<String, Class>("GSC2ID", String.class);
        ra  = new Pair<String, Class>("Ra", String.class);
        dec = new Pair<String, Class>("Dec", String.class);
        b   = new Pair<String, Class>("Jmag", String.class);
        i   = new Pair<String, Class>("Nmag", String.class);
        r   = new Pair<String, Class>("Fmag", String.class);
        v   = new Pair<String, Class>("Vmag", String.class);
        header = new DefaultCatalogHeader(DefaultImList.create(id,ra,dec,b,i,r,v));
    }

    private ImList<Object> createRow(double b, double i, double r, double v) {
        return DefaultImList.create(
                (Object) "ID123",
                "10:00:00",
                "20:00:00",
                Double.toString(b),
                Double.toString(i),
                Double.toString(r),
                Double.toString(v));
    }

    private SkyObject createSkyObject(ImList<Object> row) throws CatalogException {
        return Gsc2EsoSkyObjectFactory.instance.create(header, new DefaultCatalogRow(row));
    }

    private void verifyNonMag(SkyObject obj) {
        assertEquals("ID123", obj.getName());
        assertEquals(150.0, obj.getHmsDegCoordinates().getRa().getMagnitude(),  0.000001);
        assertEquals( 20.0, obj.getHmsDegCoordinates().getDec().getMagnitude(), 0.000001);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testNoMag() throws CatalogException {
        // Should filter out magnitudes brighter than -99
        ImList<Object> row = createRow(-99.9, -99.9, -99.9, -99.9);
        SkyObject obj = createSkyObject(row);
        verifyNonMag(obj);
        assertEquals(0, obj.getMagnitudes().size());
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testOneMag() throws CatalogException {
        // Should filter out magnitudes brighter than -99
        ImList<Object> row = createRow(0.0, -99.9, -99.9, -99.9);
        SkyObject obj = createSkyObject(row);
        verifyNonMag(obj);
        assertEquals(1, obj.getMagnitudes().size());
        assertFalse(obj.getMagnitude(B).isEmpty());
        assertTrue(obj.getMagnitude(V).isEmpty());

        Magnitude b = obj.getMagnitude(B).getValue();
        assertEquals(0.0, b.getBrightness(), 0.0000001);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testAllMags() throws CatalogException {
        ImList<Object> row = createRow(0.0, 1.0, 2.0, 3.0);
        SkyObject obj = createSkyObject(row);
        verifyNonMag(obj);
        assertEquals(4, obj.getMagnitudes().size());
        assertFalse(obj.getMagnitude(B).isEmpty());
        assertFalse(obj.getMagnitude(I).isEmpty());
        assertFalse(obj.getMagnitude(R).isEmpty());
        assertFalse(obj.getMagnitude(V).isEmpty());

        Magnitude b = obj.getMagnitude(B).getValue();
        assertEquals(0.0, b.getBrightness(), 0.0000001);
        Magnitude i = obj.getMagnitude(I).getValue();
        assertEquals(1.0, i.getBrightness(), 0.0000001);
        Magnitude r = obj.getMagnitude(R).getValue();
        assertEquals(2.0, r.getBrightness(), 0.0000001);
        Magnitude v = obj.getMagnitude(V).getValue();
        assertEquals(3.0, v.getBrightness(), 0.0000001);
    }
}
