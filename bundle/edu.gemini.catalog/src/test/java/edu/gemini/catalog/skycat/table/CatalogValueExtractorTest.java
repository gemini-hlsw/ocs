//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.skycalc.Angle;
import static edu.gemini.skycalc.Angle.Unit.DEGREES;
import edu.gemini.shared.skyobject.Magnitude;
import static edu.gemini.shared.skyobject.Magnitude.Band.*;
import edu.gemini.shared.util.immutable.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

import java.util.Iterator;

/**
 *
 */
public final class CatalogValueExtractorTest {
    private static final double DELTA = 0.000001;

    private static TableModelCatalogReader sampleReader() {
        return new TableModelCatalogReader(SampleData.createTableModel());
    }

    public CatalogValueExtractor sampleDataExtractor() {
        TableModelCatalogReader  rdr = sampleReader();
        Option<CatalogHeader> hdrOpt = rdr.getHeader();

        CatalogHeader hdr = hdrOpt.getValue();
        CatalogRow row = rdr.next();
        return new CatalogValueExtractor(hdr, row);
    }

    @Test
    public void testBasics() throws CatalogException {
        CatalogValueExtractor vext = sampleDataExtractor();

        assertEquals("23594336-0016235", vext.getString("2MASS"));

        final double ra = 359.930670;
        assertEquals(ra, vext.getDouble("RAJ2000"), DELTA);
        assertEquals(new Angle(ra, DEGREES), vext.getDegrees("RAJ2000"));
        assertEquals(new Angle(ra, DEGREES), vext.getRa("RAJ2000"));

        final double dec = -0.273210;
        assertEquals(dec, vext.getDouble("DEJ2000"), DELTA);
        assertEquals(new Angle(dec, DEGREES), vext.getDegrees("DEJ2000"));
        assertEquals(new Angle(dec, DEGREES), vext.getDec("DEJ2000"));

        assertEquals((Integer)222, vext.getInteger("Rflg"));
    }

    private void verifyMag(Magnitude expected, Magnitude actual, boolean expectError) {
        assertEquals(expected.getBand(), actual.getBand());
        assertEquals(expected.getBrightness(), actual.getBrightness(), DELTA);
        if (expectError) {
            assertEquals(expected.getError().getValue(), actual.getError().getValue(), DELTA);
        } else {
            //noinspection AssertEqualsBetweenInconvertibleTypes
            assertEquals(None.instance(), actual.getError());
        }
    }

    @Test
    public void testMagnitudeBasics() throws CatalogException {
        CatalogValueExtractor vext = sampleDataExtractor();

        final Magnitude j = new Magnitude(J, 15.664, 0.102);
        final Magnitude h = new Magnitude(H, 15.563, 0.133);
        final Magnitude k = new Magnitude(K, 15.002, 0.147);

        // Get a magnitude w/o error info
        Option<Magnitude> jOpt = vext.getOptionalMagnitude(J, "Jmag");
        verifyMag(j, jOpt.getValue(), false);

        // Get a magnitude with error info.
        jOpt = vext.getOptionalMagnitude(J, "Jmag", "e_Jmag");
        verifyMag(j, jOpt.getValue(), true);

        // Get a magnitude with error info.
        CatalogValueExtractor.MagnitudeDescriptor jdesc;
        jdesc = new CatalogValueExtractor.MagnitudeDescriptor(J, "Jmag", "e_Jmag");
        jOpt = vext.getOptionalMagnitude(jdesc);
        verifyMag(j, jOpt.getValue(), true);

        // Get multiple magnitude information
        CatalogValueExtractor.MagnitudeDescriptor hdesc;
        hdesc = new CatalogValueExtractor.MagnitudeDescriptor(H, "Hmag", "e_Hmag");
        CatalogValueExtractor.MagnitudeDescriptor kdesc;
        kdesc = new CatalogValueExtractor.MagnitudeDescriptor(K, "Kmag", "e_Kmag");

        ImList<CatalogValueExtractor.MagnitudeDescriptor> col;
        col = DefaultImList.create(kdesc, jdesc, hdesc);

        ImList<Magnitude> mags = vext.getMagnitudes(col);
        assertEquals(3, mags.size());
        Iterator<Magnitude> it = mags.iterator();
        verifyMag(k, it.next(), true);
        verifyMag(j, it.next(), true);
        verifyMag(h, it.next(), true);
    }

    @Test
    public void testOptional() throws CatalogException {
        Tuple2<String, Class> id, ra, dec, jmag;
        id  = new Pair<String, Class>("col",   String.class);
        ImList<Tuple2<String,Class>> headerLst = DefaultImList.create(id);
        DefaultCatalogHeader header = new DefaultCatalogHeader(headerLst);

        ImList<Object>         emptyLst = DefaultImList.create((Object)null);
        DefaultCatalogRow      emptyRow = new DefaultCatalogRow(emptyLst);
        CatalogValueExtractor emptyVext = new CatalogValueExtractor(header,  emptyRow);

        ImList<Object>         presLst = DefaultImList.create((Object)"45");
        DefaultCatalogRow      presRow = new DefaultCatalogRow(presLst);
        CatalogValueExtractor presVext = new CatalogValueExtractor(header,  presRow);

        // DEGREES -------------------------------------------

        // Not present optional
        Option<Angle> a = emptyVext.getOptionalDegrees("col");
        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(None.instance(), a);

        // Not present required
        try {
            emptyVext.getDegrees("col");
            fail();
        } catch (CatalogException ex) {
            // expected
        }

        // Present optional
        a = presVext.getOptionalDegrees("col");
        assertEquals(45.0, a.getValue().getMagnitude(), DELTA);


        // DOUBLE --------------------------------------------

        // Not present optional
        Option<Double> d = emptyVext.getOptionalDouble("col");
        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(None.instance(), d);

        // Not present required
        try {
            emptyVext.getDouble("col");
            fail();
        } catch (CatalogException ex) {
            // expected
        }

        // Present optional
        d = presVext.getOptionalDouble("col");
        assertEquals(45.0, d.getValue(), DELTA);


        // INTEGER ---------------------------------------------

        // Not present optional
        Option<Integer> i = emptyVext.getOptionalInteger("col");
        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(None.instance(), i);

        // Not present required
        try {
            emptyVext.getInteger("col");
            fail();
        } catch (CatalogException ex) {
            // expected
        }

        // Present optional
        i = presVext.getOptionalInteger("col");
        assertEquals(45, i.getValue(), DELTA);


        // MAGNITUDE -------------------------------------------

        // Not present optional
        Option<Magnitude> m = emptyVext.getOptionalMagnitude(Magnitude.Band.J, "col");
        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(None.instance(), m);


        // STRING ----------------------------------------------

        // Not present optional
        Option<String> s = emptyVext.getOptionalString("col");
        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(None.instance(), s);

        // Not present required
        try {
            emptyVext.getString("col");
            fail();
        } catch (CatalogException ex) {
            // expected
        }

        // Present optional
        s = presVext.getOptionalString("col");
        assertEquals("45", s.getValue());
    }

}
