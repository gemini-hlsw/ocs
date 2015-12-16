package edu.gemini.shared.util.immutable;

import java.io.Serializable;

/**
 * Default implementation of the Tuple2 interface.
 */
public final class Trio<T, U, V> implements Tuple3<T, U, V>, Serializable {

    private final T _1;
    private final U _2;
    private final V _3;

    public Trio(final T left, final U middle, final V right) {
        _1 = left;
        _2 = middle;
        _3 = right;
    }

    public T _1() {
        return _1;
    }

    public U _2() {
        return _2;
    }

    public V _3() {
        return _3;
    }


    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Tuple3)) return false;

        final Tuple3 that = (Tuple3) o;
        if (_1 == null) {
            if (that._1() != null) return false;
        } else {
            if (!_1.equals(that._1())) return false;
        }

        if (_2 == null) {
            if (that._2() != null) return false;
        } else {
            if (!_2.equals(that._2())) return false;
        }
        if (_3 == null) {
            if (that._3() != null) return false;
        } else {
            if (!_3.equals(that._3())) return false;
        }
        return true;
    }

    public int hashCode() {
        int res = 1;
        if (_1 != null) res = _1.hashCode();
        if (_2 != null) res = 37 * res + _2.hashCode();
        if (_3 != null) res = 37 * res + _3.hashCode();
        return res;
    }

    public String mkString(final String prefix, final String sep, final String suffix) {
        return prefix + _1 + sep + _2 + sep + _3 + suffix;
    }

    public String toString() {
        return mkString("(", ", ", ")");
    }
}
