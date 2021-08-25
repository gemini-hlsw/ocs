package edu.gemini.shared.util.immutable;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Right<L,R> implements ImEither<L,R>, Serializable {
    private final R value;

    public Right(final R value) throws NullPointerException {
        this.value = Objects.requireNonNull(value, "Right cannot be initialized with null.");
    }

    @Override
    public ImEither<R,L> swap() {
        return new Left<>(value);
    }

    @Override
    public <T> T foldRight(final T zero,
                           final Function<? super R, ? extends T> rightFunc) {
        return rightFunc.apply(value);
    }

    @Override
    public boolean exists(final Predicate<? super R> rightFunc) {
        return rightFunc.test(value);
    }

    @Override
    public boolean forAll(final Predicate<? super R> rightFunc) {
        return rightFunc.test(value);
    }

    @Override
    public <R2> ImEither<L,R2> map(final Function<? super R, ? extends R2> rightFunc) {
        return new Right<>(rightFunc.apply(value));
    }

    @Override
    public <R2> ImEither<L,R2> flatMap(final Function<? super R, ? extends ImEither<L,R2>> rightFunc) {
        return rightFunc.apply(value);
    }

    @Override
    public boolean isRight() {
        return true;
    }

    @Override
    public Optional<R> toOptional() {
        return Optional.of(value);
    }

    @Override
    public Option<R> toOption() {
        return new Some<>(value);
    }

    @Override
    public <T> T foldLeft(final T zero,
                          final Function<? super L, ? extends T> leftFunc) {
        return zero;
    }

    @Override
    public boolean existsLeft(final Predicate<? super L> leftFunc) {
        return false;
    }

    @Override
    public boolean forAllLeft(final Predicate<? super L> leftFunc) {
        return true;
    }

    @Override
    public void forEachLeft(final Consumer<? super L> leftFunc) {}

    @Override
    public void forEach(final Consumer<? super R> rightFunc) {
        rightFunc.accept(value);
    }

    @Override
    public <L2> ImEither<L2,R> mapLeft(final Function<? super L, ? extends L2> leftFunc) {
        return new Right<>(value);
    }

    @Override
    public <L2> ImEither<L2,R> flatMapLeft(final Function<? super L, ? extends ImEither<L2,R>> leftFunc) {
        return new Right<>(value);
    }

    @Override
    public boolean isLeft() {
        return false;
    }

    @Override
    public Optional<L> toOptionalLeft() {
        return Optional.empty();
    }

    @Override
    public Option<L> toOptionLeft() {
        return ImOption.empty();
    }

    @Override
    public <T> T biFold(Function<? super L, ? extends T> leftFunc,
                        Function<? super R, ? extends T> rightFunc) {
        return rightFunc.apply(value);
    }

    @Override
    public void biForEach(final Consumer<? super L> leftFunc,
                          final Consumer<? super R> rightFunc) {
        rightFunc.accept(value);
    }

    @Override
    public <L2,R2> ImEither<L2,R2> biMap(final Function<? super L, ? extends L2> leftFunc,
                                         final Function<? super R, ? extends R2> rightFunc) {
        return new Right<>(rightFunc.apply(value));
    }
}
