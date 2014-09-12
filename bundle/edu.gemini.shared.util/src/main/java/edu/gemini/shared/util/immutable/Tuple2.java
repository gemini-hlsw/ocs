//
// $
//

package edu.gemini.shared.util.immutable;

/**
 * An association of two objects of type <code>T</code> and <code>U</code>.
 */
public interface Tuple2<T, U> {
    /**
     * The first (or left) object in the tuple.
     */
    T _1();

    /**
     * The second (or right) object in the tuple.
     */
    U _2();

    /**
     * Returns a new Tuple2 with the elements swapped.
     */
    Tuple2<U, T> swap();

    /**
     * Creates a string representation of this tuple that is formed as in
     * <pre>
     * prefix + _1().toString() + sep + _2().toString() + suffix
     * </pre>
     *
     * @param prefix string to prepend to the result
     * @param sep separator string to place between the two elements
     * @param suffix string to append to the result
     *
     * @return string representation of the tuple
     */
    String mkString(String prefix, String sep, String suffix);
}
