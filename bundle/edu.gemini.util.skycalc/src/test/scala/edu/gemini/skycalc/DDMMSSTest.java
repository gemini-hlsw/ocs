//
// $
//

package edu.gemini.skycalc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.text.ParseException;

/**
 *
 */
public final class DDMMSSTest {

    private static final double DELTA = 0.000001;

    @Test
    public void testParse() throws Exception {
        String[] valid = {
                "1:00:00",
                "01:00:00",
                "+01:00:00",
                "1:00:00.0",
                "1:00:00.0000000000000000001",
        };

        for (String str : valid) {
            Angle res = DDMMSS.parse(str);
            assertEquals(1, res.getMagnitude(), DELTA);
        }

        assertEquals(  1.5, DDMMSS.parse( "01:30:00").getMagnitude(), DELTA);
        assertEquals( -1.5, DDMMSS.parse("-01:30:00").getMagnitude(), DELTA);
        assertEquals(  1.0000277778, DDMMSS.parse( "01:00:00.1").getMagnitude(), DELTA);
        assertEquals( -1.0000277778, DDMMSS.parse("-01:00:00.1").getMagnitude(), DELTA);
        assertEquals( 90.0, DDMMSS.parse( "90:00:00").getMagnitude(), DELTA);
        assertEquals(-90.0, DDMMSS.parse("-90:00:00").getMagnitude(), DELTA);

    }

    @Test
    public void testNoParse() throws Exception {
        String[] invalid = {
                "01 00 00",
                "*1:00:00",
                "0::00",
                "0",
                "0.00.00",
                "01:00:",
                "01:00:00:00",
                "01 00 00 00",
                "01 -0 -0",
                "91:00:00",
                "90:00:00.1",
                "-91:00:00",
                "-90:00:00.1",
        };

        for (String str : invalid) {
            try {
                DDMMSS.parse(str);
                fail(str);
            } catch (ParseException ex) {
                // expected
            }
        }
    }

    @Test
    public void testValidSeparators() throws Exception {

        String[][] valid = {
                { "01 00 00", " "   },  // common to use a space
                { "01.00.00.0", "." },  // should work even with a .
                { "01[00[00", "["   },  // should work even with regex characters
                { "01#$%00#$%00", "#$%" }, // multiple chars okay
                { "01-6-00-6-00", "-6-" }, // okay to contain a digit
        };

        for (String[] validPair : valid) {
            assertEquals(1.0, DDMMSS.parse(validPair[0], validPair[1]).getMagnitude(), DELTA);
        }
    }

    @Test
    public void testInvalidSeparators() throws Exception {
        String[][] valid = {
                { "01600600", "6"      },  // cannot be a digit
                { "016x006x00.0", "6x" },  // cannot start with a digit
                { "01x600x600", "x6"   },  // cannot end with a digit
                { "010000", null       },  // cannot be null
                { "010000", ""         },  // cannot be empty
        };

        for (String[] validPair : valid) {
            try {
                DDMMSS.parse(validPair[0], validPair[1]);
                fail("'" + validPair[0] + "', '" + validPair[1] + "'");
            } catch (IllegalArgumentException expected) {
                // okay
            }
        }
    }
}