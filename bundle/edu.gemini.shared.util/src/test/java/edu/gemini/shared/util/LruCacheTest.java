//
// $
//

package edu.gemini.shared.util;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests cases for {@link LruCache}.
 */
public class LruCacheTest {

    @Test
    public void testIllegalArument() {
        try {
            new LruCache<String, String>(0);
            fail("0 not allowed");
        } catch (IllegalArgumentException ex) {
            // okay
        }

        try {
            new LruCache<String, String>(-1);
            fail("negative not allowed");
        } catch (IllegalArgumentException ex) {
            // okay
        }
    }

    @Test
    public void testOne() {
        LruCache<String, String> c = new LruCache<String, String>(1);
        c.put("Steve", "Jobs");
        assertEquals("Jobs", c.get("Steve"));

        c.put("Eric", "Schmidt");
        assertEquals("Schmidt", c.get("Eric"));

        // Kicks out the oldest entry.
        assertNull(c.get("Steve"));
    }

    @Test
    public void testLru() {
        LruCache<String, String> c = new LruCache<String, String>(2);
        c.put("Steve", "Jobs");
        assertEquals("Jobs", c.get("Steve"));

        c.put("Eric", "Schmidt");
        assertEquals("Schmidt", c.get("Eric"));

        // Access Steve.
        assertEquals("Jobs", c.get("Steve"));

        // Now we will kick out Eric, though it was inserted after Steve
        c.put("Larry", "Ellison");
        assertNull(c.get("Eric"));
        assertEquals("Jobs", c.get("Steve"));
        assertEquals("Ellison", c.get("Larry"));
    }
}
