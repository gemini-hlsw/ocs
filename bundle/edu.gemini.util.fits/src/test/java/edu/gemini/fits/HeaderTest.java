//
// $Id: HeaderTest.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

import junit.framework.TestCase;

import java.util.*;

/**
 *
 */
public class HeaderTest extends TestCase {

    private static void _verify(List<HeaderItem> exp, Header act) {

        Map<String, Integer> keyCounts = new HashMap<String, Integer>();

        assertEquals(exp.size(), act.size());

        int i=0;
        Iterator<HeaderItem> it = act.iterator();
        for (HeaderItem hi : exp) {
            assertSame(hi, act.get(i++));
            assertTrue(it.hasNext());
            assertSame(hi, it.next());

            String keyword = hi.getKeyword();
            Integer count = keyCounts.get(keyword);
            if (count == null) {
                count = 0;
                keyCounts.put(keyword, count);
            }

            List<HeaderItem> lst = act.getAll(keyword);
            assertSame(hi, lst.get(count));

            if (count == 0) {
                assertEquals(hi, act.get(keyword));
            }

            keyCounts.put(keyword, count + 1);
        }
    }

    private static HeaderItem create(int i) {
        return DefaultHeaderItem.create("ITEM"+i, "VAL"+i, "COM"+i);
    }

    public void testAdd() throws Exception {
        Header h = new DefaultHeader();

        // Add to an empty header.
        List<HeaderItem> exp = new ArrayList<HeaderItem>();
        HeaderItem hi = create(0);
        assertTrue(h.add(hi));
        exp.add(hi);
        _verify(exp, h);

        // Add a few at the end of a header.
        for (int i=1; i<5; ++i) {
            hi = create(i);
            assertTrue(h.add(hi));
            exp.add(hi);
            _verify(exp, h);
        }

        // Add a duplicate key.
        hi = create(0);
        h.add(hi);
        assertTrue(exp.add(hi));
        _verify(exp, h);
    }

    public void testAddIndex() throws Exception {
        Header h = new DefaultHeader();

        // Add to an empty header.
        List<HeaderItem> exp = new ArrayList<HeaderItem>();
        HeaderItem hi0 = create(0);

        try {
            h.add(1, hi0);
            fail("no exception");
        } catch (IndexOutOfBoundsException ex) {
        }
        h.add(0, hi0);
        exp.add(hi0);
        _verify(exp, h);

        // Add a few at the end of a header.
        for (int i=1; i<5; ++i) {
            HeaderItem hi = create(i);
            assertTrue(h.add(hi));
            exp.add(hi);
            _verify(exp, h);
        }

        // Add at the beginning of the header.
        HeaderItem hi5 = create(5);
        h.add(0, hi5);
        exp.add(0, hi5);
        _verify(exp, h);

        assertEquals(1, h.indexOf(hi0));
        assertEquals(0, h.indexOf(hi5));

        // Add a duplicate key, ahead of the existing key.
        HeaderItem hi0_2 = create(0);
        h.add(0, hi0_2);

        List<HeaderItem> lst = h.getAll("ITEM0");
        assertEquals(2, lst.size());
        assertSame(hi0_2, lst.get(0));
        assertSame(hi0, lst.get(1));
        assertNotSame(hi0, lst.get(0));
    }

    public void testAddAll() throws Exception {
        Header h = new DefaultHeader();
        List<HeaderItem> exp = new ArrayList<HeaderItem>();

        List<HeaderItem> lst = new ArrayList<HeaderItem>();
        for (int i=0; i<5; ++i) {
            HeaderItem hi = create(i);
            lst.add(hi);
        }

        h.addAll(lst);
        exp.addAll(lst);
        _verify(exp, h);

        lst.clear();

        // Add to the end
        for (int i=5; i<10; ++i) {
            HeaderItem hi = create(i);
            lst.add(hi);
        }
        h.addAll(h.size(), lst);
        exp.addAll(exp.size(), lst);
        _verify(exp, h);

        lst.clear();

        // Add to the beginning
        for (int i=10; i<15; ++i) {
            HeaderItem hi = create(i);
            lst.add(hi);
        }
        h.addAll(0, lst);
        exp.addAll(0, lst);
        _verify(exp, h);

        lst.clear();

        // Add to the middle
        for (int i=15; i<20; ++i) {
            HeaderItem hi = create(i);
            lst.add(hi);
        }
        h.addAll(5, lst);
        exp.addAll(5, lst);
        _verify(exp, h);

        lst.clear();

        // Add a duplicate key ahead of an existing key.
        HeaderItem hi5 = create(5);
        lst.add(hi5);
        HeaderItem hi6 = create(6);
        lst.add(hi6);

        h.addAll(5, lst);
        exp.addAll(5, lst);

        assertEquals(5, h.indexOf(hi5));
        assertEquals(6, h.indexOf(hi6));

        lst = h.getAll("ITEM5");
        assertEquals(2, lst.size());
        assertSame(hi5, lst.get(0));
        assertNotSame(hi5, lst.get(1));
    }

    public void testRemoveByIndex() throws Exception {
        Header h = new DefaultHeader();
        List<HeaderItem> exp = new ArrayList<HeaderItem>();

        for (int i=0; i<10; ++i) {
            HeaderItem hi = create(i);
            h.add(hi);
            exp.add(hi);
        }

        h.remove(0);
        exp.remove(0);
        _verify(exp, h);

        HeaderItem hi0 = create(0);
        HeaderItem hi1 = create(1);

        assertEquals(-1, h.indexOf(hi0));
        assertEquals(0, h.indexOf(hi1));

        assertFalse(h.contains(hi0));
        assertTrue(h.contains(hi1));
    }


    public void testCreateIndexedHeader() {

        Header h = new DefaultHeader();

        assertEquals(0, h.getIndex());

        h = new DefaultHeader(1);

        assertEquals(1, h.getIndex());


    }


}
