package edu.gemini.shared.util.immutable;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The Option implementation that represents the lack of a value.
 */
public final class None<T> implements Option<T>, Serializable {
    @SuppressWarnings("rawtypes")
    public static None INSTANCE = new None<>();

    public static <T> Option<T> instance() {
        //noinspection unchecked
        return (Option<T>) INSTANCE;
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
    public T getOrElse(final T defaultValue) {
        return defaultValue;
    }

    @Override
    public T getOrElse(final Supplier<? extends T> supplier) {
        return supplier.get();
    }

    @Override
    public Option<T> orElse(final Option<T> that) {
        return that;
    }

    @Override
    public Option<T> orElse(final Supplier<Option<T>> supplier) {
        return supplier.get();
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
    public boolean isDefined() {
        return false;
    }

    @Override
    public ImList<T> toImList() {
        return ImCollections.emptyList();
    }

    @Override
    public Option<T> filter(final Function1<? super T, Boolean> p) {
        return instance();
    }

    @Override
    public void foreach(final ApplyOp<? super T> tApplyOp) {
        // do nothing
    }

    @Override
    public boolean contains(final T that) { return false; }

    @Override
    public boolean exists(final Function1<? super T, Boolean> op) { return false; }

    @Override
    public boolean forall(final Function1<? super T, Boolean> op) { return true; }

    @Override
    public <U> Option<U> map(final Function1<? super T, ? extends U> tuMapOp) {
        return instance();
    }

    @Override
    public <U> Option<U> flatMap(final Function1<? super T, Option<U>> tOptionMapOp) {
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
    public Stream<T> toStream() {
        return Stream.empty();
    }

    // Note: in this case, since the object is None, we return a Left with the
    // supplier's value.
    @Override
    public <X> ImEither<X, T> toRight(final Supplier<X> sup) {
        return new Left<>(sup.get());
    }

    // Note: in this case, since the object is None, we return a Right with the
    // supplier's value.
    @Override
    public <X> ImEither<T, X> toLeft(final Supplier<X> sup) {
        return new Right<>(sup.get());
    }


    @Override
    public boolean equals(final Object o) {
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
