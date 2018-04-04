package edu.gemini.shared.util.immutable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Left<L,R> implements ImEither<L,R> {
    private final L value;

    public Left(final L value) throws NullPointerException {
        this.value = Objects.requireNonNull(value, "Left cannot be initialized with null.");
    }

    @Override
    public ImEither<R,L> swap() {
        return new Right<>(value);
    }

    @Override
    public <T> T foldRight(final T zero,
                           final Function<? super R, ? extends T> rightFunc) {
        return zero;
    }

    @Override
    public boolean exists(final Predicate<? super R> rightFunc) {
        return false;
    }

    @Override
    public boolean forAll(final Predicate<? super R> rightFunc) {
        return true;
    }

    @Override
    public void forEach(final Consumer<? super R> rightFunc) {}

    @Override
    public <R2> ImEither<L,R2> map(final Function<? super R, ? extends R2> rightFunc) {
        return new Left<>(value);
    }

    @Override
    public <R2> ImEither<L,R2> flatMap(final Function<? super R, ? extends ImEither<L,R2>> rightFunc) {
        return new Left<>(value);
    }

    @Override
    public boolean isRight() {
        return false;
    }

    @Override
    public Optional<R> toOptional() {
        return Optional.empty();
    }

    @Override
    public Option<R> toOption() {
        return ImOption.empty();
    }

    @Override
    public <T> T foldLeft(final T zero,
                          final Function<? super L, ? extends T> leftFunc) {
        return leftFunc.apply(value);
    }

    @Override
    public boolean existsLeft(final Predicate<? super L> leftFunc) {
        return leftFunc.test(value);
    }

    @Override
    public boolean forAllLeft(final Predicate<? super L> leftFunc) {
        return leftFunc.test(value);
    }

    @Override
    public void forEachLeft(final Consumer<? super L> leftFunc) {
        leftFunc.accept(value);
    }

    @Override
    public <L2> ImEither<L2,R> mapLeft(final Function<? super L, ? extends L2> leftFunc) {
        return new Left<>(leftFunc.apply(value));
    }

    @Override
    public <L2> ImEither<L2,R> flatMapLeft(final Function<? super L, ? extends ImEither<L2,R>> leftFunc) {
        return leftFunc.apply(value);
    }

    @Override
    public boolean isLeft() {
        return true;
    }

    @Override
    public Optional<L> toOptionalLeft() {
        return Optional.of(value);
    }

    @Override
    public Option<L> toOptionLeft() {
        return new Some<>(value);
    }

    @Override
    public <T> T biFold(final Function<? super L, ? extends T> leftFunc,
                        final Function<? super R, ? extends T> rightFunc) {
        return leftFunc.apply(value);
    }

    @Override
    public void biForEach(final Consumer<? super L> leftFunc,
                          final Consumer<? super R> rightFunc) {
        leftFunc.accept(value);
    }

    @Override
    public <L2,R2> ImEither<L2,R2> biMap(final Function<? super L, ? extends L2> leftFunc,
                                         final Function<? super R, ? extends R2> rightFunc) {
        return new Left<>(leftFunc.apply(value));
    }
}
