package edu.gemini.shared.util.immutable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The Option implementation that represents the presence of a value.
 */
public final class Some<T> implements Option<T> {
    private final T val;

    public Some(final T val) {
        this.val = val;
    }

    @Override
    public T getValue() {
        return val;
    }

    @Override
    public T getOrElse(final T defaultValue) {
        return val;
    }

    @Override
    public T getOrElse(final Supplier<? extends T> supplier) {
        return val;
    }

    @Override
    public Option<T> orElse(final Option<T> that) {
        return this;
    }

    @Override
    public Option<T> orElse(final Supplier<Option<T>> supplier) {
        return this;
    }

    @Override
    public <U> U fold(
        Supplier<? extends U>             ifEmpty,
        Function1<? super T, ? extends U> f
    ) {
        return f.apply(val);
    }

    @Override
    public T getOrNull() {
        return val;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isDefined() {
        return true;
    }

    @Override
    public ImList<T> toImList() {
        return ImCollections.singletonList(val);
    }

    @Override
    public Option<T> filter(final Function1<? super T, Boolean> p) {
        if (p.apply(val)) return this;
        return None.instance();
    }

    @Override
    public void foreach(final ApplyOp<? super T> op) {
        op.apply(val);
    }

    @Override
    public boolean contains(final T that) {
        return val.equals(that);
    }

    @Override
    public boolean exists(final Function1<? super T, Boolean> op) {
        return op.apply(val);
    }

    @Override
    public boolean forall(final Function1<? super T, Boolean> op) {
        return op.apply(val);
    }

    @Override
    public <U> Option<U> map(final Function1<? super T, ? extends U> op) {
        final U res = op.apply(val);
        return new Some<>(res);
    }

    @Override
    public <U> Option<U> flatMap(final Function1<? super T, Option<U>> op) {
        return op.apply(val);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            boolean first = true;

            public boolean hasNext() {
                return first;
            }

            public T next() {
                if (first) {
                    first = false;
                    return val;
                } else {
                    throw new NoSuchElementException();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Stream<T> toStream() {
        return Stream.of(val);
    }

    @Override
    public <X> ImEither<X, T> toRight(final Supplier<X> sup) {
        return new Right<>(val);
    }

    @Override
    public <X> ImEither<T, X> toLeft(final Supplier<X> sup) {
        return new Left<>(val);
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Some)) return false;
        return val.equals(((Some) o).val);
    }

    @Override
    public int hashCode() {
        return val.hashCode();
    }
}
