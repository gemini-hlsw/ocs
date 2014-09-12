//
// $
//

package edu.gemini.shared.util.immutable;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The Option implementation that represents the lack of a value.
 */
public final class None<T> implements Option<T>, Serializable {
    public static None INSTANCE = new None();

    public static <T> None<T> instance() {
        //noinspection unchecked
        return (None<T>) INSTANCE;
    }

    // There is only one None instance which is defined above as INSTANCE.
    // Java can infer the type in a statement such as:
    //
    //   Option<Integer> none = None.instance();
    //
    // but not, for example, when provided as a parameter to a method
    //
    //   someMethod(None.instance());
    //
    // for this reason, a few commonly used types are provided below to be
    // directly used without having to ignore type warnings or create a
    // reference to a None on a separate line:
    //
    //   someMethod(None.INTEGER);
    //
    // instead of
    //
    //   Option<Integer> none = None.instance();
    //   someMethod(none);

    /**
     * Equivalent to {@link #INSTANCE} and the result of calling
     * {@link #instance()}, but typed for convenience.
     */
    public static final Option<Integer> INTEGER = instance();

    /**
     * Equivalent to {@link #INSTANCE} and the result of calling
     * {@link #instance()}, but typed for convenience.
     */
    public static final Option<Double> DOUBLE = instance();

    /**
     * Equivalent to {@link #INSTANCE} and the result of calling
     * {@link #instance()}, but typed for convenience.
     */
    public static final Option<String> STRING = instance();

    private None() {
        // blank
    }

    @Override
    public T getValue() {
        throw new NoSuchElementException();
    }

    @Override
    public T getOrElse(T defaultValue) {
        return defaultValue;
    }

    @Override
    public Option<T> orElse(Option<T> that) {
        return that;
    }

    @Override
    public T getOrNull() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ImList<T> toImList() {
        return ImCollections.emptyList();
    }

    @Override
    public Option<T> filter(Function1<? super T, Boolean> p) {
        return instance();
    }

    @Override
    public void foreach(ApplyOp<? super T> tApplyOp) {
        // do nothing
    }

    @Override
    public boolean exists(Function1<? super T, Boolean> op) { return false; }

    @Override
    public boolean forall(Function1<? super T, Boolean> op) { return true; }

    @Override
    public <U> Option<U> map(Function1<? super T, U> tuMapOp) {
        return instance();
    }

    @Override
    public <U> Option<U> flatMap(Function1<? super T, Option<U>> tOptionMapOp) {
        return instance();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            public boolean hasNext() { return false; }
            public T next() { throw new NoSuchElementException(); }
            public void remove() { throw new UnsupportedOperationException(); }
        };
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof None);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    private Object readResolve() {
        return instance();
    }
}
