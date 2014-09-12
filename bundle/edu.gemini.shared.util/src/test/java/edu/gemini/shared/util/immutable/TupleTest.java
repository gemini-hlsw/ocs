//
// $
//

package edu.gemini.shared.util.immutable;

import junit.framework.TestCase;

/**
 * Test code for Tuple implementations.
 */
public class TupleTest extends TestCase {

    /**
     * Creates the concrete tuple class.
     */
    protected <T, U> Tuple2<T, U> createTuple(T t, U u) {
        return new Pair<T, U>(t, u);
    }

    public void testNormalConstruction() {
        Tuple2<String, Integer> t = createTuple("one", 1);

        assertEquals(t._1(), "one");
        assertEquals(t._2(), new Integer(1));
    }

    public void testNullConstruction() {
        Tuple2<String, Integer> t = createTuple(null, null);
        assertNull(t._1());
        assertNull(t._2());
    }

    public void testSwap() {
        Tuple2<String, Integer> t = createTuple("one", 1);
        Tuple2<Integer, String> s = t.swap();

        assertEquals(new Integer(1), s._1());
        assertEquals("one", s._2());
    }
}
