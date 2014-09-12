//
// $
//

package edu.gemini.catalog.skycat.binding.skyobj;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.catalog.skycat.table.*;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.Pair;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Tuple2;

import static edu.gemini.shared.skyobject.Magnitude.Band.*;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Most *SkyObjectFactory implementations can simply delegate to
 * {@link FactorySupport} after defining a few columns.  This class adds
 * test cases for this class and covers the simple *SkyObjectFactory
 * instances.
 */
public final class FactorySupportTest {
    private enum TestSkyObjectFactory implements SkyObjectFactory {
        instance;

        public static final String ID_COL  = "ID";
        public static final String RA_COL  = "RA";
        public static final String DEC_COL = "DEC";

        public static final CatalogValueExtractor.MagnitudeDescriptor J_MAG =
                new CatalogValueExtractor.MagnitudeDescriptor(J, "Jmag");
        public static final CatalogValueExtractor.MagnitudeDescriptor K_MAG =
                new CatalogValueExtractor.MagnitudeDescriptor(K, "Kmag");

        private static final FactorySupport sup =
                new FactorySupport.Builder(ID_COL, RA_COL, DEC_COL).add(J_MAG, K_MAG).build();

        public static final Set<Magnitude.Band> BANDS = Collections.unmodifiableSet(
                new HashSet<Magnitude.Band>(Arrays.asList(J, K)));

        @Override
        public Set<Magnitude.Band> bands() { return BANDS; }

        @Override
        public String getMagColumn(Magnitude.Band band) {
            return sup.getMagColumn(band);
        }

        @Override
        public SkyObject create(CatalogHeader header, CatalogRow row) throws CatalogException {
            return sup.create(header, row);
        }
    }

    private SkyObject createSkyObject(ImList<Object> row) throws CatalogException {
        return TestSkyObjectFactory.instance.create(header, new DefaultCatalogRow(row));
    }

    private final CatalogHeader header;

    public FactorySupportTest() {
        Tuple2<String, Class> id, ra, dec, j, k;
        id  = new Pair<String, Class>("ID", String.class);
        ra  = new Pair<String, Class>("RA", String.class);
        dec = new Pair<String, Class>("DEC", String.class);
        j   = new Pair<String, Class>("Jmag", String.class);
        k   = new Pair<String, Class>("Kmag", String.class);
        header = new DefaultCatalogHeader(DefaultImList.create(id,ra,dec,j,k));
    }

    @Test
    public void testNoId() throws CatalogException {
        ImList<Object> row = DefaultImList.create((Object)null, "10:00:00", "20:00:00", "", "");
        try {
            createSkyObject(row);
            fail("Id is required, though it can be blank ...");
        } catch (CatalogException ex) {
            // expected
        }
    }

    @Test
    public void testNoRa() throws CatalogException {
        ImList<Object> row = DefaultImList.create((Object)"1234", "", "20:00:00", "", "");
        try {
            createSkyObject(row);
            fail("RA must be specified");
        } catch (CatalogException ex) {
            // expected
        }
    }

    @Test
    public void testNoDec() throws CatalogException {
        ImList<Object> row = DefaultImList.create((Object)"1234", "10:00:00", "", "", "");
        try {
            createSkyObject(row);
            fail("Dec must be specified");
        } catch (CatalogException ex) {
            // expected
        }
    }

    private void verifyNonMag(SkyObject obj) {
        assertEquals("1234", obj.getName());
        assertEquals(150.0, obj.getHmsDegCoordinates().getRa().getMagnitude(),  0.000001);
        assertEquals( 20.0, obj.getHmsDegCoordinates().getDec().getMagnitude(), 0.000001);
    }

    @Test
    public void testNoMag() throws CatalogException {
        ImList<Object> row = DefaultImList.create((Object)"1234", "10:00:00", "20:00:00", "", "");
        SkyObject obj = createSkyObject(row);
        verifyNonMag(obj);

        // mags are optional
        assertTrue(obj.getMagnitude(J).isEmpty());
        assertTrue(obj.getMagnitude(K).isEmpty());
    }

    @Test
    public void testMag() throws CatalogException {
        ImList<Object> row = DefaultImList.create((Object)"1234", "10:00:00", "20:00:00", "8", "");
        SkyObject obj = createSkyObject(row);
        verifyNonMag(obj);

        Magnitude mag = obj.getMagnitude(J).getValue();
        assertEquals(8, mag.getBrightness(), 0.000001);
        assertTrue(obj.getMagnitude(K).isEmpty());

        Set set = obj.getMagnitudeBands();
        assertTrue(set.contains(J));
        assertFalse(set.contains(K));
        assertFalse(set.contains(H));
    }

    @Test
    public void testMag2() throws CatalogException {
        ImList<Object> row = DefaultImList.create((Object)"1234", "10:00:00", "20:00:00", "8", "10");
        SkyObject obj = createSkyObject(row);
        verifyNonMag(obj);

        Magnitude jmag = obj.getMagnitude(J).getValue();
        assertEquals(8, jmag.getBrightness(), 0.000001);
        Magnitude kmag = obj.getMagnitude(K).getValue();
        assertEquals(10, kmag.getBrightness(), 0.000001);

        Set set = obj.getMagnitudeBands();
        assertTrue(set.contains(J));
        assertTrue(set.contains(K));
        assertFalse(set.contains(H));
    }
}
