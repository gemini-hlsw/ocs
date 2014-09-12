//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.skycalc.Angle;
import static edu.gemini.skycalc.Angle.Unit.DEGREES;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Test cases for the default CatalogRow implementation in
 * {@link AbstractCatalogRow} and {@link DefaultCatalogRow}.
 */
public final class CatalogRowTest {

    @Test
    public void testNullValues() {
        ImList<Object> lst = DefaultImList.create("Hi", (Object) null, 3);
        DefaultCatalogRow row = new DefaultCatalogRow(lst);

        assertEquals("Hi", row.get(0).getValue());
        assertEquals(None.INSTANCE, row.get(1));
        assertEquals(3, row.get(2).getValue());
    }


    @Test
    public void testRa() throws Exception {
        Angle expected = new Angle(15, DEGREES);

        // Create a list of different valid representations of an RA of 01:00:00.
        ImList<Object> lst = DefaultImList.create(
            (Object) "01:00:00", // must parse the expected RA format
            15.0,       // a floating point number interpreted as degrees
            new BigDecimal("15.0"),
            15,         // integral number interpreted as degrees
            new BigInteger("15"),
            "15.0",     // string representation of a number (degrees)
            new Angle(15, DEGREES) // an actual angle
        );
        DefaultCatalogRow row = new DefaultCatalogRow(lst);

        for (int col=0; col<lst.size(); ++col) {
            Option<Angle> opt = row.getRa(col);
            Angle res = opt.getValue();
            assertEquals(expected, res);
        }

        // Now make a list of invalid representations of an RA.
        lst = DefaultImList.create(
            "01 00 00",    // using a non-standard separator
            new Object()   // some random object
        );
        row = new DefaultCatalogRow(lst);

        for (int col=0; col<lst.size(); ++col) {
            try {
                row.getRa(col);
                fail(row.getString(col).getValue());
            } catch (CatalogException ex) {
                // expected
            }
        }
    }

    @Test
    public void testDec() throws Exception {
        Angle expected = new Angle(-15, DEGREES);

        // Create a list of different valid representations of a dec of -15:00:00.
        ImList<Object> lst = DefaultImList.create(
            (Object) "-15:00:00", // must parse the expected dec format
            -15.0,       // a floating point number interpreted as degrees
            new BigDecimal("-15.0"),
            -15,         // integral number interpreted as degrees
            new BigInteger("-15"),
            "-15.0",     // string representation of a number (degrees)
            new Angle(-15, DEGREES) // an actual angle
        );
        DefaultCatalogRow row = new DefaultCatalogRow(lst);

        for (int col=0; col<lst.size(); ++col) {
            Option<Angle> opt = row.getDec(col);
            Angle res = opt.getValue();
            assertEquals(expected, res);
        }

        // Now make a list of invalid representations of a dec
        lst = DefaultImList.create(
            "01 00 00",    // using a non-standard separator
            new Object()   // some random object
        );
        row = new DefaultCatalogRow(lst);

        for (int col=0; col<lst.size(); ++col) {
            try {
                row.getDec(col);
                fail(row.getString(col).getValue());
            } catch (CatalogException ex) {
                // expected
            }
        }
    }

    @Test
    public void testDegrees() throws Exception {
        Angle expected = new Angle(-15, DEGREES);

        // Create a list of different valid representations of -15 degrees
        @SuppressWarnings({"UnnecessaryBoxing"}) ImList<Object> lst = DefaultImList.create(
            (Object) new Double(-15.0),
            -15,
            "-15.0",
            new Angle(-15, DEGREES)
        );
        DefaultCatalogRow row = new DefaultCatalogRow(lst);

        for (int col=0; col<lst.size(); ++col) {
            Option<Angle> opt = row.getDegrees(col);
            Angle res = opt.getValue();
            assertEquals(expected, res);
        }

        // Now make a list of invalid representations of an angle
        lst = DefaultImList.create(
            new Object()   // some random object
        );
        row = new DefaultCatalogRow(lst);

        for (int col=0; col<lst.size(); ++col) {
            try {
                row.getDegrees(col);
                fail(row.getString(col).getValue());
            } catch (CatalogException ex) {
                // expected
            }
        }
    }

    @Test
    public void testDouble() throws Exception {
        Double expected = 15.0;

        // Create a list of different representations of 15
        ImList<Object> lst = DefaultImList.create(
                (Object) 15.0,
                new BigDecimal("15.0"),
                15,
                new BigInteger("15"),
                "15.0"
        );
        DefaultCatalogRow row = new DefaultCatalogRow(lst);

        for (int col=0; col<lst.size(); ++col) {
            Option<Double> opt = row.getDouble(col);
            Double res = opt.getValue();
            assertEquals(expected, res);
        }

        // Now make a list of invalid representations of a double value
        lst = DefaultImList.create(
            new Object(), // some random object
            new Angle(15, DEGREES)
        );
        row = new DefaultCatalogRow(lst);

        for (int col=0; col<lst.size(); ++col) {
            try {
                row.getDouble(col);
                fail(row.getString(col).getValue());
            } catch (CatalogException ex) {
                // expected
            }
        }
    }

    @Test
    public void testInteger() throws Exception {
        Integer expected = 15;

        // Create a list of different representations of 15
        ImList<Object> lst = DefaultImList.create(
                (Object) 15.9,   // truncates
                new BigDecimal("15.9"), // truncates
                15,
                new BigInteger("15"),
                "15"
        );
        DefaultCatalogRow row = new DefaultCatalogRow(lst);

        for (int col=0; col<lst.size(); ++col) {
            Option<Integer> opt = row.getInteger(col);
            Integer res = opt.getValue();
            assertEquals(expected, res);
        }

        // Now make a list of invalid representations of a double value
        lst = DefaultImList.create(
            new Object(), // some random object
            "15.9",
            new Angle(15, DEGREES)
        );
        row = new DefaultCatalogRow(lst);

        for (int col=0; col<lst.size(); ++col) {
            try {
                row.getInteger(col);
                fail(row.getString(col).getValue());
            } catch (CatalogException ex) {
                // expected
            }
        }
    }

    @Test
    public void testString() throws Exception {
        String expected = "15.0";

        // Create a list of different representations of 15
        ImList<Object> lst = DefaultImList.create(
                (Object) 15.0,
                new BigDecimal("15.0"),
                15.0,
                "15.0"
        );
        DefaultCatalogRow row = new DefaultCatalogRow(lst);

        for (int col=0; col<lst.size(); ++col) {
            Option<String> opt = row.getString(col);
            String res = opt.getValue();
            assertEquals(expected, res);
        }
    }

}
