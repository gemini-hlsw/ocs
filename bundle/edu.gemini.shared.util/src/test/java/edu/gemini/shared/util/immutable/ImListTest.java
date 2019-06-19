//
// $
//

package edu.gemini.shared.util.immutable;

import junit.framework.TestCase;

import java.util.*;

/**
 * Test code for immutable lists.
 */
public class ImListTest extends TestCase {
    protected <T> ImList<T> create(T... elements) {
        return DefaultImList.create(elements);
    }

    private ImList<Integer> emptySingleton = ImCollections.emptyList();
    private ImList<Integer> empty = create(0).tail();


    protected void setUp() {
    }

    public void testCons() {
        ImList<Integer> lst = create(1, 2, 3);
        assertEquals(create(0, 1, 2, 3), lst.cons(0));

        assertEquals(create(0), emptySingleton.cons(0));
        assertEquals(create(0), empty.cons(0));
    }

    public void testAppendTail() {
        ImList<Integer> lst = create(0, 1);
        assertEquals(create(0, 1, 2, 3), lst.append(create(2, 3)));

        assertEquals(create(0, 1), emptySingleton.append(create(0, 1)));
        assertEquals(create(0, 1), empty.append(create(0, 1)));

        ImList<Number> lstNumber = create((Number) (-1));
        ImList<Number> res = lstNumber.append(lst);
        assertEquals(create((Number)(-1), 0, 1), res);

        ImList<Number> emptyNumberList = ImCollections.emptyList();
        assertEquals(create((Number)0,1,2), emptyNumberList.append(create(0,1,2)));
    }

    public void testAppend() {
        ImList<Integer> lst = create(0, 1);
        assertEquals(create(0, 1, 2), lst.append(2));

        assertEquals(create(0), emptySingleton.append(0));
        assertEquals(create(0), empty.append(0));
    }

    public void testRemove() {
        ImList<Integer> lst = create(0, 1, 2, 3);
        assertEquals(create(0, 2, 3), lst.remove(1));
        assertEquals(create(0, 2), lst.remove(1).remove(3));

        ImList<Integer> tmp = lst.remove(0).remove(1).remove(2).remove(3);
        assertEquals(emptySingleton, tmp);
        assertEquals(empty, tmp);

        assertEquals(lst, lst.remove(42));
        assertEquals(emptySingleton, emptySingleton.remove(0));
        assertEquals(empty, empty.remove(0));
    }

    public void testRemovePredicate() {
        ImList<Integer> lst = create(0, 1, 2, 3);

        PredicateOp<Number> op = new PredicateOp<Number>() {
            @Override public Boolean apply(Number i) {
                return i.intValue()%2==0;
            }
        };
        assertEquals(create(1,3), create(0,1,2,3,4).remove(op));
        assertEquals(create(1,3), create(1,2,3).remove(op));
        assertEquals(create(1,3), create(1,3).remove(op));
        assertEquals(empty, create(0,2,4).remove(op));
        assertEquals(empty, empty.remove(op));
        assertEquals(emptySingleton, emptySingleton.remove(op));
    }

    public void testHead() {
        ImList<Integer> lst = create(0);
        assertEquals(new Integer(0), lst.head());

        assertNull(emptySingleton.head());
        assertNull(empty.head());
    }

    public void testTail() {
        ImList<Integer> lst = create(0, 1, 2);
        assertEquals(create(1, 2), lst.tail());
        assertEquals(create(2), lst.tail().tail());
        assertEquals(empty, lst.tail().tail().tail());

        assertEquals(emptySingleton, emptySingleton.tail());
        assertEquals(empty, empty.tail());
    }

    public void testLast() {
        ImList<Integer> lst = create(0, 1, 2);
        assertEquals(new Integer(2), lst.last());

        lst = create(0);
        assertEquals(new Integer(0), lst.last());

        assertNull(emptySingleton.last());
        assertNull(empty.last());
    }

    public void testInitial() {
        ImList<Integer> lst = create(0, 1, 2);
        assertEquals(create(0, 1), lst.initial());
        assertEquals(create(0), lst.initial().initial());
        assertEquals(empty, lst.initial().initial().initial());

        assertEquals(emptySingleton, emptySingleton.initial());
        assertEquals(empty, empty.initial());
    }

    public void testContains() {
        ImList<Integer> lst = create(0, 1, 2);
        for (int i=0; i<3; ++i) assertTrue(lst.contains(i));
        assertFalse(lst.contains(3));

        assertFalse(emptySingleton.contains(0));
        assertFalse(empty.contains(0));
    }

    public void testContainsAll() {
        ImList<Integer> lst = create(0, 1, 2);
        assertTrue(lst.containsAll(create(0, 1, 2)));
        assertTrue(lst.containsAll(create(0, 1)));
        assertTrue(lst.containsAll(create(0)));
        assertTrue(lst.containsAll(empty));
        assertFalse(lst.containsAll(create("x")));

        assertFalse(lst.containsAll(create(0, 1, 2, 3)));
        assertFalse(lst.containsAll(create(3)));

        assertTrue(emptySingleton.containsAll(empty));
        assertTrue(empty.containsAll(emptySingleton));
        assertFalse(emptySingleton.containsAll(create(1)));
        assertFalse(empty.containsAll(create(1)));
    }

    public void testGet() {
        ImList<Integer> lst = create(0, 1, 2);
        for (int i=0; i<3; ++i) assertEquals(new Integer(i), lst.get(i));

        try {
            lst.get(-1);
            fail("index not out of bounds?");
        } catch (IndexOutOfBoundsException ex) {
            // okay
        }

        try {
            lst.get(3);
            fail("index not out of bounds?");
        } catch (IndexOutOfBoundsException ex) {
            // okay
        }

        try {
            emptySingleton.get(0);
            fail("index not out of bounds?");
        } catch (IndexOutOfBoundsException ex) {
            // okay
        }

        try {
            empty.get(0);
            fail("index not out of bounds?");
        } catch (IndexOutOfBoundsException ex) {
            // okay
        }
    }

    public void testIndexOf() {
        ImList<Integer> lst = create(0, 1, 2);
        for (int i=0; i<3; ++i) assertEquals(i, lst.indexOf(i));

        assertEquals(-1, lst.indexOf(3));
        assertEquals(-1, emptySingleton.indexOf(0));
        assertEquals(-1, empty.indexOf(0));
    }

    public void testIsEmpty() {
        ImList<Integer> lst = create(0, 1, 2);
        assertFalse(lst.isEmpty());
        assertFalse(lst.tail().isEmpty());
        assertFalse(lst.tail().tail().isEmpty());
        assertTrue(lst.tail().tail().tail().isEmpty());

        assertTrue(emptySingleton.isEmpty());
        assertTrue(empty.isEmpty());
    }

    public void testSize() {
        ImList<Integer> lst = create(0, 1, 2);
        assertEquals(3, lst.size());
        assertEquals(2, lst.tail().size());
        assertEquals(1, lst.tail().tail().size());
        assertEquals(0, lst.tail().tail().tail().size());

        assertEquals(0, emptySingleton.size());
        assertEquals(0, empty.size());
    }

    public void testToList() {
        ImList<Integer> lst = create(0, 1, 2);
        List<Integer> tmp = new ArrayList<Integer>();
        for (int i=0; i<3; ++i) tmp.add(i);

        for (int i=0; i<3; ++i) {
            assertEquals(tmp, lst.toList());
            lst = lst.tail();
            tmp.remove(0);
        }

        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(Collections.emptyList(), emptySingleton.toList());
        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(Collections.emptyList(), empty.toList());
    }

    public void testMap() {
        MapOp<Number, String> op = new MapOp<Number, String>() {
            public String apply(Number n) { return n.toString(); }
        };

        ImList<Integer> lst = create(0, 1, 2);
        assertEquals(create("0", "1", "2"), lst.map(op));
        assertEquals(create("1", "2"), lst.tail().map(op));
        assertEquals(create("2"), lst.tail().tail().map(op));
        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(empty, lst.tail().tail().tail().map(op));

        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(emptySingleton, emptySingleton.map(op));

        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(empty, empty.map(op));
    }

    public void testFlatMap() {
        MapOp<String, ImList<Character>> op = new MapOp<String, ImList<Character>>() {
            public ImList<Character> apply(String str) {
                ImList<Character> res = ImCollections.emptyList();
                for (char c : str.toCharArray()) res = res.append(c);
                return res;
            }
        };

        ImList<String> lst = create("Scala", "Java");
        assertEquals(create(create('S', 'c', 'a', 'l', 'a'), create('J', 'a', 'v', 'a')), lst.map(op));
        assertEquals(create('S', 'c', 'a', 'l', 'a', 'J', 'a', 'v', 'a'), lst.flatMap(op));

        ImList<Character> emptyC = ImCollections.emptyList();
        ImList<String> emptyS = ImCollections.emptyList();
        assertEquals(emptyC, emptyS.flatMap(op));
    }

    public void testForeach() {
        class SumOp implements ApplyOp<Integer> {
            int sum = 0;
            public void apply(Integer integer) {
                sum += integer;
            }
        }

        SumOp op = new SumOp();

        ImList<Integer> lst = create(0, 1, 2, 3);
        lst.foreach(op);
        assertEquals(6, op.sum);

        op = new SumOp();
        lst = create(1);
        lst.foreach(op);
        assertEquals(1, op.sum);

        op = new SumOp();
        emptySingleton.foreach(op);
        assertEquals(0, op.sum);

        op = new SumOp();
        empty.foreach(op);
        assertEquals(0, op.sum);
    }

    private PredicateOp<Integer> even = new PredicateOp<Integer>() {
        public Boolean apply(Integer integer) {
            return (integer % 2 == 0);
        }
    };

    public void testIndexWhere() {
        assertEquals(-1, create(1, 3, 5, 7).indexWhere(even));
        assertEquals(0,  create(0, 1, 2, 3).indexWhere(even));
        assertEquals(3,  create(1, 3, 5, 6).indexWhere(even));
    }

    public void testFind() {
        assertEquals(None.INSTANCE, create(1, 3, 5, 7).find(even));
        assertEquals(new Some<Integer>(0), create(0, 1, 2).find(even));
        assertEquals(new Some<Integer>(2), create(1, 2, 3).find(even));
        assertEquals(new Some<Integer>(4), create(1, 3, 4).find(even));

        assertEquals(None.INSTANCE, emptySingleton.find(even));
        assertEquals(None.INSTANCE, empty.find(even));
    }

    public void testFilter() {
        assertEquals(create(2, 4, 6), create(1,2,3,4,5,6).filter(even));
        assertEquals(create(2, 4, 6), create(2, 4, 6).filter(even));
        assertEquals(empty, create(1, 3, 5).filter(even));

        assertEquals(empty, empty.filter(even));
        assertEquals(emptySingleton, emptySingleton.filter(even));
    }

    private Tuple2<ImList<Integer>, ImList<Integer>> tup(ImList<Integer> lst1, ImList<Integer> lst2) {
        return new Pair<ImList<Integer>, ImList<Integer>>(lst1, lst2);
    }

    public void testPartition() {
        ImList<Integer> emptyIntList = ImCollections.emptyList();
        assertEquals(tup(create(0, 2, 4), create(1, 3, 5)), create(0,1,2,3,4,5).partition(even));
        assertEquals(tup(create(0, 2, 4), emptyIntList), create(0,2,4).partition(even));
        assertEquals(tup(emptyIntList, create(1,3,5)), create(1,3,5).partition(even));

        assertEquals(tup(empty, empty), empty.partition(even));
        assertEquals(tup(empty, empty), emptySingleton.partition(even));
    }

    public void testForall() {
        assertTrue(create(0, 2, 4, 6).forall(even));
        assertFalse(create(1, 3, 5, 7).forall(even));
        assertFalse(create(0, 1).forall(even));

        assertTrue(empty.forall(even));
        assertTrue(emptySingleton.forall(even));
    }

    public void testExists() {
        assertTrue(create(0, 2, 4).exists(even));
        assertFalse(create(1, 3, 5).exists(even));
        assertTrue(create(1, 2, 3).exists(even));

        assertFalse(empty.exists(even));
        assertFalse(emptySingleton.exists(even));
    }

    public void testMkString() {
        assertEquals("{0, 1, 2}", create(0, 1, 2).mkString("{", ", ", "}"));
        assertEquals("{0}", create(0).mkString("{", ", ", "}"));

        assertEquals("{}", emptySingleton.mkString("{", ", ", "}"));
        assertEquals("{}", empty.mkString("{", ", ", "}"));
    }

    private static <T, U> Tuple2<T, U> ctup(T t, U u) {
        return new Pair<T, U>(t, u);
    }

    @SuppressWarnings({"AssertEqualsBetweenInconvertibleTypes"})
    public void testZip() {
        ImList<Integer> intList = create(0, 1, 2);
        ImList<Character> charList = create('a', 'b', 'c');
        ImList<Tuple2<Integer, Character>> expected = create(ctup(0, 'a'), ctup(1, 'b'), ctup(2, 'c'));
        assertEquals(expected, intList.zip(charList));

        // Test rhs shorter than lhs
        ImList<Character> smallCharList = create('a', 'b');
        expected = create(ctup(0, 'a'), ctup(1, 'b'));
        assertEquals(expected, intList.zip(smallCharList));

        // Test lhs shorter than rhs
        ImList<Integer> smallIntList = create(0, 1);
        assertEquals(expected, smallIntList.zip(charList));

        // Test empty
        assertEquals(ImCollections.emptyList(), intList.zip(ImCollections.emptyList()));
        assertEquals(ImCollections.emptyList(), ImCollections.emptyList().zip(intList));
    }

    @SuppressWarnings({"AssertEqualsBetweenInconvertibleTypes"})
    public void testZipWithIndex() {
        ImList<Character> charList = create('a', 'b', 'c');
        ImList<Tuple2<Character, Integer>> expected = create(ctup('a', 0), ctup('b', 1), ctup('c', 2));
        assertEquals(expected, charList.zipWithIndex());
        assertEquals(ImCollections.emptyList(), ImCollections.emptyList().zipWithIndex());
    }

    public void testZipWithNext() {
        // 0
        assertEquals(ImCollections.emptyList(), ImCollections.emptyList().zipWithNext());

        // 1
        final ImList<Character> charList1 = create('a');
        final ImList<Tuple2<Character, Option<Character>>> expected1 = create(
            ctup('a', ImOption.empty())
        );
        assertEquals(expected1, charList1.zipWithNext());

        // n
        final ImList<Character> charListN = create('a', 'b', 'c');
        final ImList<Tuple2<Character, Option<Character>>> expectedN = create(
            ctup('a', new Some<>('b')),
            ctup('b', new Some<>('c')),
            ctup('c', ImOption.empty())
        );
        assertEquals(expectedN, charListN.zipWithNext());
    }

    public void testUnzip() {
        ImList<Tuple2<Character, Integer>> zipped = create(ctup('a', 0), ctup('b', 1), ctup('c', 2));
        ImList<Integer> intList = create(0, 1, 2);
        ImList<Character> charList = create('a', 'b', 'c');
        Tuple2<ImList<Character>, ImList<Integer>> expected;
        expected = new Pair<ImList<Character>, ImList<Integer>>(
            charList, intList
        );

        assertEquals(expected, ImCollections.unzip(zipped));
    }

    public void testSort() {
        Comparator<Integer> c = new Comparator<Integer>() {
            @Override public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        };

        ImList<Integer> lst = create(1, 3, 5, 2, 4);
        lst = lst.sort(c);

        assertEquals(create(1,2,3,4,5), lst);

        //noinspection unchecked
        assertSame(empty, empty.sort(c));
        assertSame(emptySingleton, emptySingleton.sort(c));

        lst = create(1);
        assertSame(lst, lst.sort(c));
    }

    public void testTake() {
        assertSame(empty,          empty.take(1));
        assertSame(emptySingleton, emptySingleton.take(1));

        assertEquals(empty,                  emptySingleton.take(1));
        assertEquals(emptySingleton.take(1), emptySingleton);

        final ImList<Integer> lst = create(1, 2, 3);
        assertEquals(empty, lst.take(-1));
        assertEquals(empty, lst.take(0));
        assertEquals(lst,   lst.take(3));

        assertEquals(create(1),    lst.take(1));
        assertEquals(create(1, 2), lst.take(2));
        assertEquals(lst,          lst.take(3));
        assertEquals(lst,          lst.take(4));
        assertEquals(lst,          lst.take(Integer.MAX_VALUE));
    }

    public void testReverse() {
        ImList<Integer> lst = create(1, 2, 3, 4);
        lst = lst.reverse();

        assertEquals(create(4,3,2,1), lst);

        assertSame(empty, empty.reverse());
        assertSame(emptySingleton, emptySingleton.reverse());

        lst = create(1);
        assertSame(lst, lst.reverse());
    }

    public void testFoldLeft() {
        Function2<String, Character, String> op = new Function2<String, Character, String>() {
            @Override public String apply(String cur, Character c) {
                return cur + c;
            }
        };

        assertEquals("abc", create('a', 'b', 'c').foldLeft("", op));
        //noinspection unchecked
        assertEquals("", ImCollections.EMPTY_LIST.foldLeft("", op));
    }

    public void testFoldRight() {

        Function2<Character, String, String> op = new Function2<Character, String, String>() {
            @Override public String apply(Character c, String cur) {
                return cur + c;
            }
        };

        assertEquals("cba", create('a', 'b', 'c').foldRight("", op));
        //noinspection unchecked
        assertEquals("", ImCollections.EMPTY_LIST.foldRight("", op));
    }

    public void testGroupBy() {
        Function1<Character, Integer> f = new Function1<Character, Integer>() {
            @Override public Integer apply(Character c) {
                if (c == 'a') { return 1; }
                else if (c == 'b') { return 2; }
                else if (c == 'c') { return 3; }
                else { return 4; }
            }
        };

        HashMap<Integer, ImList<Character>> em = new HashMap<>();

        em.put(1, create('a'));
        em.put(2, create('b', 'b'));
        em.put(3, create('c', 'c', 'c'));
        em.put(4, create('e', 'd'));

        assertEquals(em, create('c', 'a', 'b', 'e', 'b', 'c', 'c', 'd').groupBy(f));
        assertEquals(new HashMap<>(), ImCollections.EMPTY_LIST.groupBy(f));
    }
}
