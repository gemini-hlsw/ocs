package edu.gemini.shared.util.immutable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This is essentially an unbiased ImEither[ImEither[R,S],T],
 * which is needed by the target table (TelescopePosTableModel) since rows
 * can comprise three distinct types:
 * 1. SPTargets         (actual targets)
 * 2. Coordinates       (sky targets - no magnitude or other features)
 * 3. IndexedGuideGroup (a row heading and representing a guide group)
 */
public abstract class OneOf3<R,S,T> {
    OneOf3() {
    }

    final boolean isFirst() {
        return fold(r -> true, s -> false, t -> false);
    }

    final boolean isSecond() {
        return fold(r -> false, s -> true, t -> false);
    }

    final boolean isThird() {
        return fold(r -> false, s -> false, t -> true);
    }

    final Option<R> getFirst() {
        return fold(Some::new, s -> None.instance(), t -> None.instance());
    }

    final Option<S> getSecond() {
        return fold(r -> None.instance(), Some::new, t -> None.instance());
    }

    final Option<T> getThird() {
        return fold(r -> None.instance(), s -> None.instance(), Some::new);
    }

    abstract <U> U fold(final Function<? super R, ? extends U> r,
                        final Function<? super S, ? extends U> s,
                        final Function<? super T, ? extends U> t);

    abstract void forEach(final Consumer<? super R> r,
                          final Consumer<? super S> s,
                          final Consumer<? super T> t);

    abstract boolean forAll(final Predicate<? super R> r,
                            final Predicate<? super S> s,
                            final Predicate<? super T> t);

    abstract <RR, SS, TT> OneOf3<RR, SS, TT> map(final Function<? super R, ? extends RR> r,
                                                 final Function<? super S, ? extends SS> s,
                                                 final Function<? super T, ? extends TT> t);

    abstract <RR, SS, TT> OneOf3<RR, SS, TT> flatMap(final Function<? super R, ? extends OneOf3<RR,SS,TT>> r,
                                                     final Function<? super S, ? extends OneOf3<RR,SS,TT>> s,
                                                     final Function<? super T, ? extends OneOf3<RR,SS,TT>> t);

    static <A> Function<A,A> id() {
        return a -> a;
    }

    static <A> Consumer<A> noop() {
        return a -> {};
    }

    static <A> Predicate<A> fail() {
        return a -> false;
    }

    static <A> Predicate<A> pass() {
        return a -> true;
    }

    static <R,SS,TT> Function<? super R, ? extends OneOf3<R,SS,TT>> idR() {
        return r -> new FirstOf3<>(r);
    }

    static <RR,S,TT> Function<? super S, ? extends OneOf3<RR,S,TT>> idS() {
        return s -> new SecondOf3<>(s);
    }

    static <RR,SS,T> Function<? super T, ? extends OneOf3<RR,SS,T>> idT() {
        return t -> new ThirdOf3<>(t);
    }
}
