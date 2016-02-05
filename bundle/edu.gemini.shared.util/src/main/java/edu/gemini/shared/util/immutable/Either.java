package edu.gemini.shared.util.immutable;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Simulates a Scala Either[S,T].
 */
public final class Either<L,R> implements Serializable {
    private final Option<L> left;
    private final Option<R> right;

    public static <L,R> Either<L,R> left(final L left) {
        return new Either<>(new Some<>(left), None.instance());
    }

    public static <L,R> Either<L,R> right(final R right) {
        return new Either<>(None.instance(), new Some<>(right));
    }

    private Either(final Option<L> left, final Option<R> right) {
        this.left  = left;
        this.right = right;
    }

    public Option<L> getLeft() {
        return left;
    }

    public Option<R> getRight() {
        return right;
    }

    public <T> T fold(final Function1<? super L, ? extends T> leftFunc,
                      final Function1<? super R, ? extends T> rightFunc) {

        return left.map(leftFunc).getOrElse(() -> right.map(rightFunc).getValue());
    }

    public <T> Option<T> foldLeft(final Function1<? super L, ? extends T> leftFunc) {
        return left.map(leftFunc);
    }

    public <T> Option<T> foldRight(final Function1<? super R, ? extends T> rightFunc) {
        return right.map(rightFunc);
    }

    public void forEach(final ApplyOp<? super L> leftConsumer,
                        final ApplyOp<? super R> rightConsumer) {
        left.foreach(leftConsumer);
        right.foreach(rightConsumer);
    }

    public void forEachLeft(final ApplyOp<? super L> leftConsumer) {
        left.foreach(leftConsumer);
    }

    public void forEachRight(final ApplyOp<? super R> rightConsumer) {
        right.foreach(rightConsumer);
    }

    public <T> Either<T,R> mapLeft(final Function1<? super L, ? extends T> leftFunc) {
        return new Either<>(left.map(leftFunc), right);
    }

    public <T> Either<L,T> mapRight(final Function1<? super R, ? extends T> rightFunc) {
        return new Either<>(left, right.map(rightFunc));
    }

    public boolean isLeft() {
        return left.isDefined();
    }

    public boolean isRight() {
        return right.isDefined();
    }

    public Either<R,L> swap() {
        return new Either<>(right, left);
    }
}
