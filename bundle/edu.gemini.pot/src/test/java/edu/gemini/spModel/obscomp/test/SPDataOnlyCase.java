/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file LICENSE for complete details.
 *
 * $Id: SPDataOnlyCase.java 4726 2004-05-14 16:50:12Z brighton $
 */
package edu.gemini.spModel.obscomp.test;

import edu.gemini.spModel.obscomp.SPDataOnly;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


/**
 * Class SPDataOnlyTest tests the SPDataOnly obscomp
 */
public final class SPDataOnlyCase {
    private SPDataOnly _t1;

    @Before
    public void setUp() throws Exception {
        _t1 = new SPDataOnly();
    }

    @Test
    public void testInitial() {
        assertTrue(_t1.size() == 0);
    }

    // Test title
    @Test
    public void testTitle() {
        // Name should be type
        assertFalse(_t1.isTitleChanged());
        assertEquals(_t1.getType().readableStr, _t1.getTitle());

        // set the title
        String name = "Fake Data";
        // Now set the name
        _t1.setTitle(name);
        assertEquals(name, _t1.getTitle());
    }

    /**
     * Test cloneable
     */
    @Test
    public void testCloneable() {
        String title1 = "Initial Test SPDataOnly";
        // Give the data object a title
        SPDataOnly sq1 = _t1;
        assertNotNull(sq1);
        sq1.setTitle(title1);

        // Create change
        String key1 = "key1";
        List<String> d1 = new ArrayList<String>();
        d1.add("1");
        d1.add("2");
        String key2 = "key2";
        List d2 = new ArrayList();
        d1.add("a");
        d1.add("b");
        d1.add("c");
        String key3 = "key3";
        List d3 = new ArrayList();
        sq1.setProperty(key1, d1);
        sq1.setProperty(key2, d2);
        sq1.setProperty(key3, d3);
        assertTrue(d1.equals(sq1.getProperty(key1)));
        assertTrue(d2.equals(sq1.getProperty(key2)));
        assertTrue(d3.equals(sq1.getProperty(key3)));

        SPDataOnly sq2 = (SPDataOnly) sq1.clone();
        assertNotNull(sq2);
        List<String> d4 = new ArrayList<String>();
        d4.add("1000");
        sq2.setProperty(key1, d4);
        assertTrue(d4.equals(sq2.getProperty(key1)));
        assertTrue(d2.equals(sq2.getProperty(key2)));
        assertTrue(d3.equals(sq2.getProperty(key3)));
        assertTrue(d1.equals(sq1.getProperty(key1)));
    }

    @Test
    public void testSerialization() throws Exception {
        final SPDataOnly outObject = new SPDataOnly();

        // Create change
        List<String> d1 = new ArrayList<String>();
        d1.add("1");
        d1.add("2");
        List d2 = new ArrayList();
        d1.add("a");
        d1.add("b");
        d1.add("c");
        List d3 = new ArrayList();
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        outObject.setProperty(key1, d1);
        outObject.setProperty(key2, d2);
        outObject.setProperty(key3, d3);

        final SPDataOnly inObject = ser(outObject);
        assertTrue(d1.equals(inObject.getProperty(key1)));
        assertTrue(d2.equals(inObject.getProperty(key2)));
        assertTrue(d3.equals(inObject.getProperty(key3)));
    }
}
