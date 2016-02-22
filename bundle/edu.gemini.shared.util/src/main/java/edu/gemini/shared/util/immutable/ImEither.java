package edu.gemini.shared.util.immutable;

import java.io.Serializable;

/**
 * Simulates a Scalaz \/[S,T], and thus is similarly right-biased.
 */
public final class ImEither<L,R> implements Serializable {
    private final Option<L> left;
    private final Option<R> right;

    public static <L,R> ImEither<L,R> left(final L left) {
        return new ImEither<>(new Some<>(left), None.instance());
    }

    public static <L,R> ImEither<L,R> right(final R right) {
        return new ImEither<>(None.instance(), new Some<>(right));
    }

    private ImEither(final Option<L> left, final Option<R> right) {
        this.left  = left;
        this.right = right;
    }

    /**
     * Folds
     */
    public <T> T fold(final Function1<? super L, ? extends T> leftFunc,
                      final Function1<? super R, ? extends T> rightFunc) {

        return left.map(leftFunc).getOrElse(() -> right.map(rightFunc).getValue());
    }

    public <T> T foldRight(final T zero, final Function2<? super R, ? super T, ? extends T> rightFunc) {
        final Option<T> result = right.map(r -> rightFunc.apply(r, zero));
        return result.getOrElse(zero);
    }

    /**
     * Existential qualifiers
     */
    public boolean exists(final PredicateOp<? super R> rightPred) {
        return right.exists(rightPred);
    }

    public boolean forall(final PredicateOp<? super R> rightPred) {
        return right.forall(rightPred);
    }

    /**
     * foreach
     */
    public void foreach(final ApplyOp<? super R> rightConsumer) {
        right.foreach(rightConsumer);
    }

    public void biForeach(final ApplyOp<? super L> leftConsumer,
                          final ApplyOp<? super R> rightConsumer) {
        left.foreach(leftConsumer);
        right.foreach(rightConsumer);
    }

    /**
     * Maps
     */
    public <S,T> ImEither<S,T> biMap(final Function1<? super L, ? extends S> leftMap,
                                     final Function1<? super R, ? extends T> rightMap) {
        return new ImEither<>(left.map(leftMap), right.map(rightMap));
    }

    public <T> ImEither<T,R> leftMap(final Function1<? super L, ? extends T> leftFunc) {
        return new ImEither<>(left.map(leftFunc), right);
    }

    public <T> ImEither<L,T> map(final Function1<? super R, ? extends T> rightFunc) {
        return new ImEither<>(left, right.map(rightFunc));
    }

    /**
     * Accessors and other assorted functions
     */
    public Option<L> getLeft() {
        return left;
    }

    public Option<R> getRight() {
        return right;
    }

    public boolean isLeft() {
        return left.isDefined();
    }

    public boolean isRight() {
        return right.isDefined();
    }

    public ImEither<R,L> swap() {
        return new ImEither<>(right, left);
    }

    public Option<R> toOption() {
        return right;
    }
}
