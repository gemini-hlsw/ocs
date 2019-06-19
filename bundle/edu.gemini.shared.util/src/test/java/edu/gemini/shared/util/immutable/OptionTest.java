package edu.gemini.shared.util.immutable;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class OptionTest extends TestCase {
    public void testNone() throws Exception {
        Option<Integer> o = None.instance();

        try {
            o.getValue();
            fail();
        } catch (NoSuchElementException ex) {
            // okay
        }

        assertTrue(o.isEmpty());

        assertEquals(new Integer(7), o.getOrElse(7));

        ImList<Integer> emptyList = ImCollections.emptyList();
        assertEquals(emptyList, o.toImList());

        assertSame(o, o.filter(new PredicateOp<Integer>() {
            public Boolean apply(Integer integer) {
                return integer == 0;
            }
        }));

        o.foreach(new ApplyOp<Integer>() {
            public void apply(Integer integer) {
                fail();
            }
        });

        assertSame(o, o.map(new MapOp<Integer, String>() {
            public String apply(Integer integer) {
                return integer.toString();
            }
        }));

        assertSame(o, o.flatMap(new MapOp<Integer, Option<String>>() {
            public Option<String> apply(Integer integer) {
                return new Some<String>(integer.toString());
            }
        }));

        // fold
        assertSame(o.fold(() -> 0, i -> i + 1), 0);

    }

    public void testSome() throws Exception {
        Option<Integer> o = new Some<>(6);

        assertEquals(new Integer(6), o.getValue());
        assertFalse(o.isEmpty());

        assertEquals(new Integer(6), o.getOrElse(7));

        // toImList
        ImList<Integer> singletonList = DefaultImList.create(6);
        assertEquals(singletonList, o.toImList());

        // filter
        assertEquals(new Some<Integer>(6), o.filter(new PredicateOp<Integer>() {
            public Boolean apply(Integer integer) {
                return true;
            }
        }));
        Option<Integer> none = None.instance();
        assertEquals(none, o.filter(new PredicateOp<Integer>() {
            public Boolean apply(Integer integer) {
                return false;
            }
        }));

        // foreach
        final List<String> lst = new ArrayList<String>();
        o.foreach(new ApplyOp<Integer>() {
            public void apply(Integer integer) {
                lst.add(integer.toString());
            }
        });
        assertEquals(1, lst.size());
        assertEquals("6", lst.get(0));

        // map
        Option<String> res = o.map(new MapOp<Integer, String>() {
            public String apply(Integer integer) {
                return integer.toString();
            }
        });
        assertEquals(new Some<String>("6"), res);

        // flatMap
        res = o.flatMap(new MapOp<Integer, Option<String>>() {
            public Option<String> apply(Integer integer) {
                return new Some<String>(integer.toString());
            }
        });
        assertEquals(new Some<String>("6"), res);

        // fold
        assertEquals(o.fold(() -> "", i -> String.valueOf(i)), "6");
    }

    public void testApply(){
        assertTrue(ImOption.apply(null) instanceof None);
        assertTrue(ImOption.apply("") instanceof Some);
    }
}
