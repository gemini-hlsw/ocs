package edu.gemini.shared.util.immutable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

final public class SecondOf3<R,S,T>  extends OneOf3<R,S,T> {
    final private S value;

    public SecondOf3(final S value) {
        this.value = Objects.requireNonNull(value);
    }

    @Override
    <U> U fold(final Function<? super R, ? extends U> r,
               final Function<? super S, ? extends U> s,
               final Function<? super T, ? extends U> t) {
        return s.apply(value);
    }

    @Override
    void forEach(final Consumer<? super R> r,
                 final Consumer<? super S> s,
                 final Consumer<? super T> t) {
        s.accept(value);
    }

    @Override
    boolean forAll(final Predicate<? super R> r,
                   final Predicate<? super S> s,
                   final Predicate<? super T> t) {
        return s.test(value);
    }

    @Override
    <RR, SS, TT> OneOf3<RR, SS, TT> map(final Function<? super R, ? extends RR> r,
                                        final Function<? super S, ? extends SS> s,
                                        final Function<? super T, ? extends TT> t) {
        return new SecondOf3<>(s.apply(value));
    }

    @Override
    <RR, SS, TT> OneOf3<RR, SS, TT> flatMap(final Function<? super R, ? extends OneOf3<RR, SS, TT>> r,
                                            final Function<? super S, ? extends OneOf3<RR, SS, TT>> s,
                                            final Function<? super T, ? extends OneOf3<RR, SS, TT>> t) {
        return s.apply(value);
    }
}
