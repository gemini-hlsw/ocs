package edu.gemini.shared.util.immutable;

/**
 * An association of three objects of type <code>T</code>, <code>U</code> and  <code>V</code>.
 */
public interface Tuple3<T, U, V> {
    /**
     * The first (or left) object in the tuple.
     */
    T _1();

    /**
     * The second (or middle) object in the tuple.
     */
    U _2();

    /**
     * The second (or right) object in the tuple.
     */
    V _3();

    /**
     * Creates a string representation of this tuple that is formed as in
     * <pre>
     * prefix + _1().toString() + sep + _2().toString() + sep + _3().toString() + suffix
     * </pre>
     *
     * @param prefix string to prepend to the result
     * @param sep    separator string to place between two elements
     * @param suffix string to append to the result
     * @return string representation of the tuple
     */
    String mkString(String prefix, String sep, String suffix);
}
