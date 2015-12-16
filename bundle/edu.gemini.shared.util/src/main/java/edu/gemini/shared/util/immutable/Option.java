package edu.gemini.shared.util.immutable;

import java.util.NoSuchElementException;
import java.io.Serializable;

/**
 * A class that represents an optional value of a particular type.  This has
 * a couple of advantages over the common idiom of using null to represent the
 * lack of a value.  First, it makes it clear that the value is optional and
 * not just a value that is sometimes <code>null</code> (say because it
 * hasn't been initialized).  Second, it forces the user to check for the
 * lack of a value and avoids errors where the user simply forgot or was too
 * lazy to make this check.
 *
 * <p>Ideally, there should be two implementations of this class
 * {@link Some} and {@link None}.  Unfortunately, this cannot be enforced.
 */
public interface Option<T> extends Iterable<T>, Serializable {

    /**
     * Gets the value assuming there is one.
     *
     * @return value associated with this Option
     *
     * @throws NoSuchElementException if this is a {@link None} instance
     */
    T getValue();

    /**
     * Gets the value assuming there is one, but returns the
     * <code>defaultValue</code> if there is none.
     */
    T getOrElse(T defaultValue);

    /**
     * Returns <code>this</code> value if {@link Some} or else <code>that</code>.
     */
    Option<T> orElse(Option<T> that);

    /**
     * Gets the value assuming there is one, but otherwise returns
     * <code>null</code>.
     */
    T getOrNull();

    /**
     * @return <code>true</code> if this is {@link None}, <code>false</code>
     * otherwise
     */
    boolean isEmpty();

    /**
     * @return <code>true</code> if this is a {@link Some}, <code>false</code>
     * otherwise
     */
    boolean isDefined();

    /**
     * @return an {@link ImList} from the contained value (if there is one),
     * otherwise an empty list
     */
    ImList<T> toImList();

    /**
     * If the option is nonempty and the given predicate op yields
     * <code>false</code> on its value, returns {@link None}.  Otherwise
     * returns itself.
     *
     * @param op predicate to apply on the value
     */
    Option<T> filter(Function1<? super T, Boolean> op);

    /**
     * Apply the given operation on the options value if it is nonempty.
     *
     * @param op operation to apply
     */
    void foreach(ApplyOp<? super T> op);

    /**
     * Returns <code>true</code> if Some and the contained value matches the
     * predicate.
     */
    boolean exists(Function1<? super T, Boolean> op);

    /**
     * Returns <code>true</code> if None or else if the contained value matches
     * the predicate.
     */
    boolean forall(Function1<? super T, Boolean> op);

    /**
     * If the option is nonempty, returns a function applied to its value
     * wrapped in a {@link Some} instance.  Otherwise, returns {@link None}.
     *
     * @param op mapping operation to apply on the option value
     *
     * @param <U> result type of the operation
     */
    <U> Option<U> map(Function1<? super T, U> op);

    /**
     * If the option is nonempty, returns a function applied to its value
     * wrapped in a single {@link Some} instance.  Otherwise, returns
     * {@link None}.
     *
     * @param op mapping operation to apply on the option value
     *
     * @param <U> result type of the operation
     */
    <U> Option<U> flatMap(Function1<? super T, Option<U>> op);
}
