package edu.gemini.shared.util.immutable;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.Optional;

public interface ImEither<L,R> extends Serializable {
    ImEither<R,L> swap();

    // === Right biased qualifiers / operations ===
    <T> T foldRight(final T zero,
                    final Function<? super R, ? extends T> rightFunc);

    boolean exists(final Predicate<? super R> rightFunc);

    boolean forAll(final Predicate<? super R> rightFunc);

    void forEach(final Consumer<? super R> rightFunc);

    <R2> ImEither<L,R2> map(final Function<? super R, ? extends R2> rightFunc);

    <R2> ImEither<L,R2> flatMap(final Function<? super R, ? extends ImEither<L,R2>> rightFunc);

    boolean isRight();

    Optional<R> toOptional();

    Option<R> toOption();

    // === Left biased qualifiers / operations ===
    <T> T foldLeft(final T zero,
                   final Function<? super L, ? extends T> leftFunc);

    boolean existsLeft(final Predicate<? super L> leftFunc);

    boolean forAllLeft(final Predicate<? super L> leftFunc);

    void forEachLeft(final Consumer<? super L> leftFunc);

    <L2> ImEither<L2,R> mapLeft(final Function<? super L, ? extends L2> leftFunc);

    <L2> ImEither<L2,R> flatMapLeft(final Function<? super L, ? extends ImEither<L2,R>> rightFunc);

    boolean isLeft();

    Optional<L> toOptionalLeft();

    Option<L> toOptionLeft();


    // === Operations on both values ===
    <T> T biFold(final Function<? super L, ? extends T> leftFunc,
                 final Function<? super R, ? extends T> rightFunc);

    void biForEach(final Consumer<? super L> leftFunc,
                   final Consumer<? super R> rightFunc);

    <L2,R2> ImEither<L2,R2> biMap(final Function<? super L, ? extends L2> leftFunc,
                                  final Function<? super R, ? extends R2> rightFunc);

    static <T> T merge(final ImEither<T,T> e) {
        return e.biFold(l -> l, r -> r);
    }
}
